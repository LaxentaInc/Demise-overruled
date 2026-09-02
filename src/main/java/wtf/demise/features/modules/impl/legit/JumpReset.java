package wtf.demise.features.modules.impl.legit;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "JumpReset", description = "Automatically jump resets in order to reduce velocity.")
public class JumpReset extends Module {
    // chance to actually fire the reset (lower = more human, avoids consistent patterns)
    private final SliderValue chance = new SliderValue("Chance", 80, 1, 100, 1, this);
    // cooldown between resets in ticks. prevents spam-jumping on rapid combo hits.
    private final SliderValue cooldownTicks = new SliderValue("Cooldown (Ticks)", 10, 5, 20, 1, this);
    // auto jump when hitting a player AFTER a successful jump reset to lock them in an air combo
    private final BoolValue comboJumps = new BoolValue("Combo Jumps", true, this);

    // public telemetry fields for the recorder module
    public boolean lastVelocityReceived = false;
    public boolean jumpedThisTick = false;
    public int ticksSinceLastReset = 999;

    // internal state
    private boolean velocityReceivedThisTick = false;
    private boolean wasOnGroundWhenHit = false;
    private boolean pendingJump = false;
    private boolean pendingComboJump = false;
    private int hitCount = 0;
    private final TimerUtils combatTimer = new TimerUtils();

    // combo state
    private boolean inComboMode = false;
    private int ticksSinceLastAttack = 999;

    @Override
    public void onDisable() {
        pendingJump = false;
        pendingComboJump = false;
        hitCount = 0;
        velocityReceivedThisTick = false;
        wasOnGroundWhenHit = false;
        ticksSinceLastReset = 999;
        inComboMode = false;
        ticksSinceLastAttack = 999;
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
                    wasOnGroundWhenHit = mc.thePlayer.onGround;
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        ticksSinceLastAttack = 0;

        // If we are in combo mode and have Combo Jumps enabled, automatically queue a jump on hit!
        if (comboJumps.get() && inComboMode) {
            // Only jump if it's a REAL hit. AutoClickers send attack packets even during i-frames.
            // If they are in i-frames, the hit doesn't register on the server, so we shouldn't jump.
            if (e.getTargetEntity() instanceof net.minecraft.entity.EntityLivingBase) {
                net.minecraft.entity.EntityLivingBase target = (net.minecraft.entity.EntityLivingBase) e.getTargetEntity();
                if (target.hurtTime <= 3) {
                    pendingComboJump = true;
                }
            } else {
                pendingComboJump = true;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag((int)cooldownTicks.get() + "t cd");
        jumpedThisTick = false;
        lastVelocityReceived = velocityReceivedThisTick;
        ticksSinceLastReset++;
        ticksSinceLastAttack++;

        if (combatTimer.hasTimeElapsed(5000)) {
            hitCount = 0;
        }

        // if we haven't attacked in a while (e.g., 20 ticks / 1 second), combo mode drops
        if (ticksSinceLastAttack > 20) {
            inComboMode = false;
        }

        if (velocityReceivedThisTick) {
            combatTimer.reset();
            hitCount++;

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
        if (pendingJump) {
            if (mc.thePlayer.onGround) {
                e.setJumping(true);
                jumpedThisTick = true;
                ticksSinceLastReset = 0;
                // A successful jump reset STARTS the combo mode!
                inComboMode = true; 
            }
            pendingJump = false;
        } else if (pendingComboJump) {
            if (mc.thePlayer.onGround) {
                e.setJumping(true);
                jumpedThisTick = true;
            }
            pendingComboJump = false;
        }
    }
}