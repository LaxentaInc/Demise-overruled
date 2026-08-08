package wtf.demise.features.modules.impl.legit;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "JumpReset", description = "Automatically jump resets in order to reduce velocity.")
public class JumpReset extends Module {
    // chance to actually fire the reset (lower = more human, avoids consistent patterns)
    private final SliderValue chance = new SliderValue("Chance", 80, 1, 100, 1, this);
    // cooldown between resets in ticks. prevents spam-jumping on rapid combo hits.
    // 8-12 ticks is roughly the minecraft i-frame window, so we only reset once per real hit.
    private final SliderValue cooldownTicks = new SliderValue("Cooldown (Ticks)", 10, 5, 20, 1, this);

    // public telemetry fields for the recorder module
    public boolean lastVelocityReceived = false;
    public boolean jumpedThisTick = false;
    public int ticksSinceLastReset = 999;

    // internal state
    private boolean velocityReceivedThisTick = false;
    private boolean wasOnGroundWhenHit = false;
    private boolean pendingJump = false;
    private int hitCount = 0;
    private final TimerUtils combatTimer = new TimerUtils();

    @Override
    public void onDisable() {
        pendingJump = false;
        hitCount = 0;
        velocityReceivedThisTick = false;
        wasOnGroundWhenHit = false;
        ticksSinceLastReset = 999;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.thePlayer == null || e.getState() != PacketEvent.State.INCOMING) return;

        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) e.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                // only care about knockback that has horizontal component (actual combat hits)
                if (Math.abs(packet.getMotionX()) > 0 || Math.abs(packet.getMotionZ()) > 0) {
                    velocityReceivedThisTick = true;
                    // snapshot ground state RIGHT NOW at the moment of impact.
                    // this is the critical check - jump reset only works if we're grounded.
                    wasOnGroundWhenHit = mc.thePlayer.onGround;
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag((int)cooldownTicks.get() + "t cd");
        jumpedThisTick = false;
        lastVelocityReceived = velocityReceivedThisTick;
        ticksSinceLastReset++;

        if (combatTimer.hasTimeElapsed(5000)) {
            hitCount = 0;
        }

        if (velocityReceivedThisTick) {
            combatTimer.reset();
            hitCount++;

            // only queue a jump if:
            // 1. we were on the ground when the velocity packet arrived
            // 2. we haven't jumped too recently (cooldown)
            // 3. chance roll passes
            if (wasOnGroundWhenHit && ticksSinceLastReset >= (int)cooldownTicks.get()) {
                int effectiveChance = (hitCount <= 3) ? 100 : (int) chance.get();
                if (MathUtils.randomizeInt(1, 100) <= effectiveChance) {
                    pendingJump = true;
                }
            }

            velocityReceivedThisTick = false;
            wasOnGroundWhenHit = false;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!pendingJump) return;

        // fire immediately. no delay needed - we already verified we were on ground
        // when the velocity arrived, so this is the same tick or next tick at most.
        // the only hard requirement is that we're still on ground right now.
        if (mc.thePlayer.onGround) {
            e.setJumping(true);
            jumpedThisTick = true;
            ticksSinceLastReset = 0;
        }

        // whether we jumped or not, consume the pending flag.
        // if we're somehow airborne already (server desync), don't keep trying -
        // that's the "baseball" behavior we want to avoid.
        pendingJump = false;
    }
}