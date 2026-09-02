package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "SprintReset", description = "Makes you deal increased knockback to targets.")
public class SprintReset extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"ReSprint", "WTap", "STap", "Sneak", "Block", "Packet", "LessPacket"}, "WTap", this);
    private final SliderValue chance = new SliderValue("Chance", 70, 1, 100, 1, this);
    private final BoolValue fast = new BoolValue("Fast", false, this, () -> mode.is("ReSprint"));
    private final SliderValue minReSprintTime = new SliderValue("Min time", 40, 10, 200, 1, this, () -> mode.is("WTap") || mode.is("STap") || mode.is("Block") || mode.is("Sneak"));
    private final SliderValue maxReSprintTime = new SliderValue("Max time", 80, 10, 200, 1, this, () -> mode.is("WTap") || mode.is("STap") || mode.is("Block") || mode.is("Sneak"));
    private final ModeValue fallbackMode = new ModeValue("Fallback mode", new String[]{"ReSprint", "WTap", "STap", "Packet", "LessPacket"}, "WTap", this, () -> mode.is("Block"));
    private final BoolValue diffCheck = new BoolValue("Angle diff check", false, this);
    private final BoolValue notWhileHurt = new BoolValue("Not while hurt", false, this);

    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils combatTimer = new TimerUtils();
    private boolean isBlocking;
    private EntityLivingBase target;
    private long currentResetDelay;
    private int hitCount;

    @Override
    public void onDisable() {
        hitCount = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        target = PlayerUtils.getTarget(8, false);

        // if out of combat for 5 seconds, reset hit counter for 100% initial burst
        if (combatTimer.hasTimeElapsed(5000)) {
            hitCount = 0;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (e.getTargetEntity() instanceof EntityLivingBase) {
            EntityLivingBase attackedTarget = (EntityLivingBase) e.getTargetEntity();
            
            if (notWhileHurt.get() && mc.thePlayer.hurtTime != 0) {
                return;
            }

            if (diffCheck.get()) {
                float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - attackedTarget.posZ, mc.thePlayer.posX - attackedTarget.posX) * 180.0 / Math.PI - 90.0);
                float diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - attackedTarget.rotationYawHead));
                if (diffX > 120) {
                    return;
                }
            }
            
            // Only sprint reset if this is a valid hit (avoids i-frame spam jumping from autoclickers)
            if (attackedTarget.hurtTime <= 3) {
                combatTimer.reset();
                hitCount++;

                // 100% chance for first 5 hits after 5s out-of-combat, then uses configured chance
                int effectiveChance = (hitCount <= 5) ? 100 : (int) chance.get();

                if (MathUtils.randomizeInt(1, 100) <= effectiveChance) {
                    currentResetDelay = (long) MathUtils.randomizeFloat(minReSprintTime.get(), maxReSprintTime.get());
                    switch (mode.get()) {
                        case "WTap", "STap", "Sneak":
                            timer.reset();
                            break;
                        case "Block":
                            if (PlayerUtils.isHoldingSword()) {
                                timer.reset();
                            } else {
                                reset(true);
                            }
                            break;
                    }

                    if (!mode.is("WTap") && !mode.is("STap") && !mode.is("Block") && !mode.is("Sneak")) {
                        reset(false);
                    }
                }
            }
        }
    }

    private void reset(boolean fallback) {
        switch (fallback ? fallbackMode.get() : mode.get()) {
            case "ReSprint":
                if (!fast.get()) {
                    mc.thePlayer.reSprint = 2;
                } else {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                break;
            case "WTap", "STap", "Sneak":
                timer.reset();
                break;
            case "Packet":
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.serverSprintState = true;
                break;
            case "LessPacket":
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                }
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.serverSprintState = true;
                break;
        }
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (mode.is("Block")) {
            if (target != null) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), !timer.hasTimeElapsed(currentResetDelay));
                isBlocking = !timer.hasTimeElapsed(currentResetDelay);
            } else if (isBlocking) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!timer.hasTimeElapsed(currentResetDelay)) {
            if (mode.is("WTap") || (mode.is("Block") && fallbackMode.is("WTap"))) {
                e.setForward(0);
            }

            if (mode.is("STap") || (mode.is("Block") && fallbackMode.is("STap"))) {
                e.setForward(-1);
            }

            if (mode.is("Sneak") || (mode.is("Block") && fallbackMode.is("Sneak"))) {
                e.setSneaking(true);
            }
        }
    }
}