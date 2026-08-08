package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "HitSelect", description = "Reactive hit selection - waits to get hit first, then retaliates with precision.")
public class HitSelect extends Module {
    // max ticks to wait in range before force-allowing a swing (prevents stalemate passivity)
    private final SliderValue maxWaitTicks = new SliderValue("Max wait ticks", 10, 3, 20, this);
    // how many ticks after our retaliation hit to block clicking again (allows combo follow-through)
    private final SliderValue comboWindowTicks = new SliderValue("Combo window", 3, 1, 8, this);
    private final SliderValue engageRange = new SliderValue("Engage range", 3.5f, 2.0f, 5.0f, 0.1f, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue wTapOnRetaliate = new BoolValue("W-Tap on retaliate", true, this);

    // public field consumed by minecraft.clickmouse() to suppress left clicks
    public boolean blockClicking;

    // state machine tracking
    private EntityLivingBase lastTarget;
    private int ticksInRange;           // how many ticks we've been within engage range of a target
    private int lastPlayerHurtTime;     // previous tick's player hurttime to detect rising edge
    private int lastTargetHurtTime;     // previous tick's target hurttime to detect rising edge
    private boolean retaliationAllowed; // true when we detected opponent hit us and we should swing back
    private int retaliationTicksLeft;   // countdown after our hit lands to keep allowing clicks (combo window)
    private boolean wTapThisTick;       // flag for movement input to apply w-tap release

    // idle timeout timer for when nobody is interacting - resets state to avoid stale data
    private final TimerUtils idleTimer = new TimerUtils();

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        blockClicking = false;
        resetState();
    }

    private void resetState() {
        blockClicking = false;
        lastTarget = null;
        ticksInRange = 0;
        lastPlayerHurtTime = 0;
        lastTargetHurtTime = 0;
        retaliationAllowed = false;
        retaliationTicksLeft = 0;
        wTapThisTick = false;
        idleTimer.reset();
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        EntityLivingBase target = PlayerUtils.getTarget(8, teamCheck.get());

        if (target == null) {
            resetState();
            return;
        }

        // if target changed, reset our tracking state for the new opponent
        if (target != lastTarget) {
            resetState();
            lastTarget = target;
        }

        // calculate whether opponent is facing us (within ~120 degree cone)
        // uses the angle between the vector from target->player and target's yaw head direction
        float angleToUs = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float facingDiff = Math.abs(MathHelper.wrapAngleTo180_float(angleToUs - target.rotationYawHead));

        if (facingDiff > 120) {
            // target isn't facing us at all - no combat interaction, don't block clicks
            blockClicking = false;
            ticksInRange = 0;
            return;
        }

        double distance = PlayerUtils.getDistanceToEntityBox(target);
        boolean playerHurt = mc.thePlayer.hurtTime > 0;
        boolean targetHurt = target.hurtTime > 0;

        // detect rising edge: player just got hit this tick (hurttime went from 0 to >0)
        boolean playerJustGotHit = mc.thePlayer.hurtTime > lastPlayerHurtTime;
        // detect rising edge: target just got hit this tick
        boolean targetJustGotHit = target.hurtTime > lastTargetHurtTime;

        // update previous-tick values for next iteration's edge detection
        lastPlayerHurtTime = mc.thePlayer.hurtTime;
        lastTargetHurtTime = target.hurtTime;

        // reset idle timer whenever either player is interacting (hurt or being hurt)
        if (playerHurt || targetHurt) {
            idleTimer.reset();
        }

        // if nobody has been hurt for 500ms+, reset state to prevent stale combo flags
        if (idleTimer.hasTimeElapsed(500)) {
            retaliationAllowed = false;
            retaliationTicksLeft = 0;
            ticksInRange = 0;
        }

        // ---- core hit-select state machine ----

        if (distance < engageRange.get()) {
            ticksInRange++;

            // state 1: we are in a combo follow-through window after landing our retaliation hit
            if (retaliationTicksLeft > 0) {
                retaliationTicksLeft--;
                blockClicking = false;
                return;
            }

            // state 2: we just got hit by the opponent - allow immediate retaliation swing
            if (playerJustGotHit && !targetHurt) {
                retaliationAllowed = true;
                blockClicking = false;
                if (wTapOnRetaliate.get()) {
                    wTapThisTick = true;
                }
                return;
            }

            // state 3: retaliation was allowed and we just landed our hit - start combo window
            if (retaliationAllowed && targetJustGotHit) {
                retaliationAllowed = false;
                retaliationTicksLeft = (int) comboWindowTicks.get();
                blockClicking = false;
                return;
            }

            // state 4: retaliation is active but we haven't connected yet - keep allowing clicks
            if (retaliationAllowed) {
                blockClicking = false;
                return;
            }

            // state 5: max wait exceeded - force allow clicking to prevent standing still getting slimed
            // this is the anti-stalemate: if opponent won't swing, we initiate after maxWaitTicks
            if (ticksInRange >= maxWaitTicks.get()) {
                retaliationAllowed = true;
                blockClicking = false;
                return;
            }

            // state 6: default waiting state - block clicks until opponent swings at us
            blockClicking = true;

        } else {
            // outside engage range - no hit selection needed, allow normal clicking
            blockClicking = false;
            ticksInRange = 0;
            retaliationAllowed = false;
            retaliationTicksLeft = 0;
        }
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent e) {
        // apply single-tick w-release for momentum manipulation on retaliation swing
        if (wTapThisTick) {
            e.setForward(0);
            wTapThisTick = false;
        }
    }
}
