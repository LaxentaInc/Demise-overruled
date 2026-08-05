package wtf.demise.features.modules.impl.legit;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "JumpReset", description = "Automatically jump resets in order to reduce velocity.")
public class JumpReset extends Module {
    private final SliderValue chance = new SliderValue("Chance", 70, 1, 100, 1, this);
    private final SliderValue minDelay = new SliderValue("Min delay", 10, 0, 100, 1, this);
    private final SliderValue maxDelay = new SliderValue("Max delay", 55, 0, 100, 1, this);

    private final TimerUtils delayTimer = new TimerUtils();
    private final TimerUtils combatTimer = new TimerUtils();
    private boolean pendingJump;
    private long targetDelay;
    private int lastHurtTime;
    private int hitCount;

    @Override
    public void onDisable() {
        pendingJump = false;
        hitCount = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(minDelay.get() + "-" + maxDelay.get() + "ms");

        // if player hasn't taken damage for 5 seconds (5000ms), reset initial 5-hit burst counter
        if (combatTimer.hasTimeElapsed(5000)) {
            hitCount = 0;
        }

        // trigger when player takes fresh damage (hurtTime hits max of 10 ticks)
        if (mc.thePlayer.hurtTime == 10 && lastHurtTime < 10) {
            combatTimer.reset();
            hitCount++;

            // 100% chance for first 5 hits after 5s out-of-combat, then uses configured chance (default 70%)
            int effectiveChance = (hitCount <= 5) ? 100 : (int) chance.get();

            if (MathUtils.randomizeInt(1, 100) <= effectiveChance) {
                targetDelay = (long) MathUtils.randomizeFloat(minDelay.get(), maxDelay.get());
                delayTimer.reset();
                pendingJump = true;
            }
        }
        lastHurtTime = mc.thePlayer.hurtTime;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (pendingJump && delayTimer.hasTimeElapsed(targetDelay)) {
            // only jump if player is on ground or about to land
            if (mc.thePlayer.onGround) {
                e.setJumping(true);
            }
            pendingJump = false;
        }
    }
}