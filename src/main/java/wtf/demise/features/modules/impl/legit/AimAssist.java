package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.player.rotation.enums.MovementCorrectionMode;
import wtf.demise.utils.player.rotation.enums.SmoothMode;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "AimAssist", description = "Assists in aiming.")
public class AimAssist extends Module {
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);
    private final SliderValue speed = new SliderValue("Speed", 25.0f, 5, 100, 1f, this);
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final SliderValue resetTime = new SliderValue("Reset time", 500, 0, 1000, 1, this, onlyOnClick::get);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);
    private final SliderValue breakAwayAngle = new SliderValue("Break away angle", 60, 20, 120, 5, this);
    // controls how much vertical (pitch) correction is applied: 0 = yaw only, 100 = full pitch tracking
    private final SliderValue pitchStrength = new SliderValue("Pitch strength", 10.0f, 0, 100, 1f, this);

    private EntityLivingBase target;
    private final TimerUtils resetTimer = new TimerUtils();

    @Override
    public void onDisable() {
        // hard reset so rotationhandler hands control straight back to raw mouse input
        RotationHandler.enabled = false;
        RotationHandler.targetRotation = null;
        RotationHandler.reset = true;
        target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (onlyOnClick.get() && Mouse.isButtonDown(0)) {
            resetTimer.reset();
        }

        if (!onlyOnClick.get() || !resetTimer.hasTimeElapsed(resetTime.get())) {
            target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());
        } else {
            target = null;
        }

        if (target == null) {
            return;
        }

        // compute the ideal yaw and pitch to the center of the target entity
        float[] fullRotation = RotationUtils.getRotations(
                target.posX,
                target.posY + target.getEyeHeight() * 0.5,
                target.posZ
        );

        // blend between the player's current pitch and the target pitch based on pitch strength
        // at 0% the aim assist is yaw-only (horizontal pull), at 100% it fully tracks pitch too
        // this prevents the jittery body-part snapping while still gently nudging vertical aim
        float pitchFactor = pitchStrength.get() / 100.0f;
        float blendedPitch = mc.thePlayer.rotationPitch + (fullRotation[1] - mc.thePlayer.rotationPitch) * pitchFactor;

        // final target: full yaw correction toward the target, gentle (or zero) pitch nudge
        float[] targetRotation = new float[]{fullRotation[0], blendedPitch};

        // check how far the player is already looking from the target horizontally
        float yawDiff = Math.abs(RotationUtils.getAngleDifference(fullRotation[0], mc.thePlayer.rotationYaw));

        // if the player is looking further away than the break-away angle, let them go
        if (yawDiff > breakAwayAngle.get()) {
            return;
        }

        // subtle speed randomization (+-10%) to avoid perfectly constant rotation velocity
        float baseSpeed = speed.get();
        float randomizedSpeed = MathUtils.randomizeFloat(baseSpeed * 0.9f, baseSpeed * 1.1f);

        // pitch speed is scaled down proportionally to pitch strength so vertical aim doesn't jitter
        float pitchSpeed = randomizedSpeed * pitchFactor;

        RotationHandler.setRotation(
                targetRotation,
                MovementCorrectionMode.Silent,
                new float[]{randomizedSpeed, Math.max(pitchSpeed, 0.01f)},
                // enable acceleration for smooth ramp-up that looks human
                true, new float[]{0.4f, 0.4f},
                SmoothMode.Relative,
                false,
                1f
        );
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }
}