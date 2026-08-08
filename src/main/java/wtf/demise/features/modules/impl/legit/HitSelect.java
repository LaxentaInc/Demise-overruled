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

@ModuleInfo(name = "HitSelect", description = "Phase-based hit selection with health awareness and combo pacing.")
public class HitSelect extends Module {

    // --- settings ---
    // max ticks to wait in range before force-initiating (anti-stalemate)
    private final SliderValue maxWaitTicks = new SliderValue("Max wait ticks", 10, 3, 20, this);
    // engage range for hit selection to activate
    private final SliderValue engageRange = new SliderValue("Engage range", 3.5f, 2.0f, 5.0f, 0.1f, this);
    // below this HP, skip opening phase entirely and just swing
    private final SliderValue healthThreshold = new SliderValue("Health threshold", 16, 4, 20, 1, this);
    // whether to pace clicks around target i-frames during combo sustain
    private final BoolValue comboPacing = new BoolValue("Combo pacing", true, this);
    // w-tap on the first retaliation hit for extra knockback
    private final BoolValue wTapOnRetaliate = new BoolValue("W-Tap on retaliate", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    // --- public field consumed by minecraft.clickmouse() to suppress left clicks ---
    public boolean blockClicking;

    // --- combat phases ---
    private enum Phase {
        OPENING,       // waiting for opponent to hit first (or skipping if low hp)
        COMBO_SUSTAIN, // we landed a hit, stay aggressive with pacing
        TRADE          // opponent hit us mid-combo, burst then re-establish
    }

    // --- state tracking ---
    private Phase currentPhase = Phase.OPENING;
    private EntityLivingBase lastTarget;
    private int ticksInRange;
    private int lastPlayerHurtTime;
    private int lastTargetHurtTime;
    private boolean wTapThisTick;

    // combo sustain tracking
    private int comboHitsLanded;          // how many consecutive hits we've landed in this combo
    private int ticksSinceOurLastHit;     // ticks since we last damaged the target

    // trade phase tracking
    private int tradeTicksLeft;           // burst window during trade phase

    // disengage grace: don't reset state if target leaves range briefly (strafing)
    private final TimerUtils disengageTimer = new TimerUtils();
    private boolean wasInRange;

    // idle timeout: reset everything if nobody is interacting
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
        currentPhase = Phase.OPENING;
        lastTarget = null;
        ticksInRange = 0;
        lastPlayerHurtTime = 0;
        lastTargetHurtTime = 0;
        wTapThisTick = false;
        comboHitsLanded = 0;
        ticksSinceOurLastHit = 0;
        tradeTicksLeft = 0;
        wasInRange = false;
        idleTimer.reset();
        disengageTimer.reset();
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        EntityLivingBase target = PlayerUtils.getTarget(8, teamCheck.get());

        if (target == null) {
            resetState();
            return;
        }

        // if target changed, reset state for the new opponent
        if (target != lastTarget) {
            resetState();
            lastTarget = target;
        }

        // check if opponent is facing us (within ~120 degree cone)
        float angleToUs = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float facingDiff = Math.abs(MathHelper.wrapAngleTo180_float(angleToUs - target.rotationYawHead));

        if (facingDiff > 120) {
            // target isn't facing us - no combat, allow clicks freely
            blockClicking = false;
            ticksInRange = 0;
            return;
        }

        double distance = PlayerUtils.getDistanceToEntityBox(target);

        // detect rising edges for hit detection
        boolean playerJustGotHit = mc.thePlayer.hurtTime > lastPlayerHurtTime;
        boolean targetJustGotHit = target.hurtTime > lastTargetHurtTime;

        lastPlayerHurtTime = mc.thePlayer.hurtTime;
        lastTargetHurtTime = target.hurtTime;

        // reset idle timer on any combat interaction
        if (mc.thePlayer.hurtTime > 0 || target.hurtTime > 0) {
            idleTimer.reset();
        }

        // if nobody has interacted for 800ms, go back to opening phase
        if (idleTimer.hasTimeElapsed(800)) {
            currentPhase = Phase.OPENING;
            comboHitsLanded = 0;
            ticksSinceOurLastHit = 0;
            ticksInRange = 0;
        }

        // track our hit timing
        if (targetJustGotHit) {
            ticksSinceOurLastHit = 0;
        } else {
            ticksSinceOurLastHit++;
        }

        // --- range handling with disengage grace ---
        if (distance < engageRange.get()) {
            ticksInRange++;
            wasInRange = true;
            disengageTimer.reset();
        } else {
            // outside range - check grace period (500ms to handle strafing/slight disengage)
            if (wasInRange && !disengageTimer.hasTimeElapsed(500)) {
                // grace period active, keep current state but don't block clicks
                // (can't hit them anyway, they're out of range)
                blockClicking = false;
                return;
            }
            // grace expired or was never in range
            blockClicking = false;
            ticksInRange = 0;
            return;
        }

        // --- health check: skip opening phase if low HP ---
        boolean lowHealth = mc.thePlayer.getHealth() < healthThreshold.get();

        // =====================================================
        //  PHASE STATE MACHINE
        // =====================================================

        switch (currentPhase) {

            case OPENING: {
                // low HP: don't wait, swing immediately
                if (lowHealth) {
                    blockClicking = false;
                    // if we land a hit, transition to combo sustain
                    if (targetJustGotHit) {
                        currentPhase = Phase.COMBO_SUSTAIN;
                        comboHitsLanded = 1;
                    }
                    return;
                }

                // normal HP: wait for opponent to swing first
                if (playerJustGotHit) {
                    // opponent hit us - retaliate immediately
                    blockClicking = false;
                    if (wTapOnRetaliate.get()) {
                        wTapThisTick = true;
                    }
                    // if our retaliation lands this same tick or next few, transition
                    if (targetJustGotHit) {
                        currentPhase = Phase.COMBO_SUSTAIN;
                        comboHitsLanded = 1;
                    }
                    return;
                }

                // anti-stalemate: if we've waited too long, just swing
                if (ticksInRange >= maxWaitTicks.get()) {
                    blockClicking = false;
                    if (targetJustGotHit) {
                        currentPhase = Phase.COMBO_SUSTAIN;
                        comboHitsLanded = 1;
                    }
                    return;
                }

                // default: block clicks, wait for opponent
                blockClicking = true;
                break;
            }

            case COMBO_SUSTAIN: {
                // we're in an active combo. stay aggressive.

                // if WE get hit during our combo, transition to trade phase
                if (playerJustGotHit) {
                    currentPhase = Phase.TRADE;
                    tradeTicksLeft = 5; // 5-tick burst window
                    blockClicking = false;
                    return;
                }

                // if we landed another hit, count it
                if (targetJustGotHit) {
                    comboHitsLanded++;
                    ticksSinceOurLastHit = 0;
                }

                // if we haven't hit them in 15 ticks (750ms), we lost the combo
                if (ticksSinceOurLastHit > 15) {
                    currentPhase = Phase.OPENING;
                    comboHitsLanded = 0;
                    ticksInRange = 0;
                    blockClicking = true;
                    return;
                }

                // combo pacing: suppress clicks while target has i-frames
                if (comboPacing.get() && target.hurtTime > 0) {
                    // target still in i-frames from our last hit.
                    // suppress clicks - hitting them now is wasted input.
                    // allow click through when hurttime is 1 (about to expire)
                    // so the click registers on the exact tick i-frames drop.
                    if (target.hurtTime > 2) {
                        blockClicking = true;
                    } else {
                        // hurttime 1-2: i-frames about to expire, let click through
                        blockClicking = false;
                    }
                } else {
                    // no i-frames active or pacing disabled, allow click
                    blockClicking = false;
                }
                break;
            }

            case TRADE: {
                // opponent hit us mid-combo. burst for a few ticks, then re-establish.
                tradeTicksLeft--;
                blockClicking = false; // full aggression during burst

                if (targetJustGotHit) {
                    // we landed a hit during trade - go back to combo sustain
                    currentPhase = Phase.COMBO_SUSTAIN;
                    comboHitsLanded++;
                    return;
                }

                if (tradeTicksLeft <= 0) {
                    // burst expired without landing a hit
                    if (lowHealth) {
                        // low HP: stay aggressive, go to combo sustain anyway
                        currentPhase = Phase.COMBO_SUSTAIN;
                    } else {
                        // normal HP: go back to opening to re-establish first-hit advantage
                        currentPhase = Phase.OPENING;
                        ticksInRange = 0;
                        comboHitsLanded = 0;
                    }
                }
                break;
            }
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

