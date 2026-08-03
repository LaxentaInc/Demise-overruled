package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
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
    private final ModeValue aimPoint = new ModeValue("Aim point", new String[]{"Nearest", "Head"}, "Nearest", this);
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final SliderValue resetTime = new SliderValue("Reset time", 500, 0, 1000, 1, this, onlyOnClick::get);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);
    private final SliderValue breakAwayAngle = new SliderValue("Break away angle", 60, 20, 120, 5, this);
    // controls how much vertical (pitch) correction is applied in nearest mode: 0 = yaw only, 100 = full pitch
    private final SliderValue pitchStrength = new SliderValue("Pitch strength", 10.0f, 0, 100, 1f, this, () -> aimPoint.is("Nearest"));

    // keep distance: automatically stops forward movement when within optimal attack range
    private final BoolValue keepDistance = new BoolValue("Keep distance", false, this);
    // the exact distance in blocks to maintain from the target's hitbox edge
    private final SliderValue keepDistanceRange = new SliderValue("Distance", 3.0f, 1.5f, 5, 0.1f, this, keepDistance::get);

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

        float targetYaw;
        float targetPitch;
        float pitchSpeed;
        float baseSpeed = speed.get();

        if (aimPoint.is("Head")) {
            // head mode: aim directly at the target's head with full pitch correction
            float[] headRotation = RotationUtils.getRotations(
                    target.posX,
                    target.posY + target.getEyeHeight(),
                    target.posZ
            );
            targetYaw = headRotation[0];
            targetPitch = headRotation[1];
            // full pitch speed in head mode so it actually locks onto the head
            pitchSpeed = baseSpeed;
        } else {
            // nearest mode: aim horizontally at the target, only nudge pitch when outside the hitbox
            float[] centerRotation = RotationUtils.getRotations(
                    target.posX,
                    target.posY + target.getEyeHeight() * 0.5,
                    target.posZ
            );
            targetYaw = centerRotation[0];

            // calculate the pitch range that covers the target's entire body (top to bottom of hitbox)
            AxisAlignedBB hitbox = target.getEntityBoundingBox();
            float pitchToTop = RotationUtils.getRotations(target.posX, hitbox.maxY, target.posZ)[1];
            float pitchToBottom = RotationUtils.getRotations(target.posX, hitbox.minY, target.posZ)[1];

            // ensure top pitch < bottom pitch (looking up = negative, looking down = positive)
            float minPitch = Math.min(pitchToTop, pitchToBottom);
            float maxPitch = Math.max(pitchToTop, pitchToBottom);

            float currentPitch = mc.thePlayer.rotationPitch;

            if (currentPitch >= minPitch && currentPitch <= maxPitch) {
                // player is already aiming within the target's body - don't touch pitch at all
                targetPitch = currentPitch;
                pitchSpeed = 0;
            } else {
                // player is aiming above or below the target, gently nudge toward the nearest edge
                float nearestEdgePitch = (Math.abs(currentPitch - minPitch) < Math.abs(currentPitch - maxPitch))
                        ? minPitch : maxPitch;

                // blend between current pitch and nearest edge based on pitch strength setting
                float pitchFactor = pitchStrength.get() / 100.0f;
                targetPitch = currentPitch + (nearestEdgePitch - currentPitch) * pitchFactor;
                pitchSpeed = baseSpeed * pitchFactor;
            }
        }

        float[] targetRotation = new float[]{targetYaw, targetPitch};

        // check how far the player is already looking from the target horizontally
        float yawDiff = Math.abs(RotationUtils.getAngleDifference(targetYaw, mc.thePlayer.rotationYaw));

        // if the player is looking further away than the break-away angle, let them go
        if (yawDiff > breakAwayAngle.get()) {
            return;
        }

        // subtle speed randomization (+-10%) to avoid perfectly constant rotation velocity
        float randomizedSpeed = MathUtils.randomizeFloat(baseSpeed * 0.9f, baseSpeed * 1.1f);

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
    public void onMoveInput(MoveInputEvent e) {
        // keep distance: cancel forward movement when within the desired range of the target
        if (!keepDistance.get() || target == null) {
            return;
        }

        // use hitbox-edge distance for accurate range calculation matching mc's actual reach check
        double distanceToTarget = PlayerUtils.getDistanceToEntityBox(target);

        if (distanceToTarget <= keepDistanceRange.get()) {
            // within optimal range - kill forward input so the player stops walking into the target
            // strafe and backward movement are preserved so the player can still dodge and retreat
            if (e.getForward() > 0) {
                e.setForward(0);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }
}