package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.player.rotation.enums.MovementCorrectionMode;
import wtf.demise.utils.player.rotation.enums.SmoothMode;
import wtf.demise.utils.render.RenderUtils;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "AimAssist", description = "Assists in aiming smoothly.")
public class AimAssist extends Module {
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    private EntityLivingBase target;
    // Hysteresis tracking: 0 = None/Unset, 1 = Head, 2 = Chest, 3 = Legs
    private int currentRegion = 0;

    // Smoothed motion vectors (prevents direction flip overshoot & lag spikes)
    private double smoothedMotionX = 0;
    private double smoothedMotionZ = 0;

    // Per-acquisition randomized human variance constants
    private float maxSpeed = 25.0f;
    private float stopThreshold = 0.1f;
    private float hysteresisMargin = 3.0f;

    @Override
    public void onDisable() {
        RotationHandler.enabled = false;
        RotationHandler.targetRotation = null;
        RotationHandler.reset = true;
        target = null;
        currentRegion = 0;
        smoothedMotionX = 0;
        smoothedMotionZ = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (onlyOnClick.get() && !Mouse.isButtonDown(0)) {
            target = null;
            currentRegion = 0;
            return;
        }

        acquireTarget();

        if (target == null) {
            currentRegion = 0;
            smoothedMotionX = 0;
            smoothedMotionZ = 0;
            return;
        }

        double deltaX = target.posX - target.lastTickPosX;
        double deltaY = Math.abs(target.posY - target.lastTickPosY);
        double deltaZ = target.posZ - target.lastTickPosZ;
        double horizontalDistance = Math.hypot(deltaX, deltaZ);

        double targetX;
        double targetY;
        double targetZ;

        // Discontinuity / Rubberband Guard: If position jumps > 1.5 blocks horizontally or > 1.0 block vertically in 1 tick,
        // skip lerping across the jump and snap directly to current position to avoid chasing ghost trajectories.
        if (horizontalDistance > 1.5 || deltaY > 1.0) {
            targetX = target.posX;
            targetY = target.posY;
            targetZ = target.posZ;
            smoothedMotionX = 0;
            smoothedMotionZ = 0;
        } else {
            float partialTicks = mc.timer.renderPartialTicks;
            
            // Exponentially smooth motion lead (60% history, 40% new input) to prevent direction-flip snapping
            smoothedMotionX = (smoothedMotionX * 0.6) + (target.motionX * 0.4);
            smoothedMotionZ = (smoothedMotionZ * 0.6) + (target.motionZ * 0.4);

            // Clamp motion lead magnitude to max 0.25 blocks to prevent packet spikes from causing flicks
            double leadX = MathHelper.clamp_double(smoothedMotionX, -0.25, 0.25);
            double leadZ = MathHelper.clamp_double(smoothedMotionZ, -0.25, 0.25);

            targetX = (target.lastTickPosX + deltaX * partialTicks) + leadX;
            targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
            targetZ = (target.lastTickPosZ + deltaZ * partialTicks) + leadZ;
        }

        double eyeHeight = target.getEyeHeight();
        double targetHeadY = targetY + eyeHeight;
        double targetChestY = targetY + eyeHeight * 0.65;
        double targetLegsY = targetY + eyeHeight * 0.25;

        // Calculate pitch angles to the 3 anatomical reference points
        float pitchToHead = RotationUtils.getRotations(targetX, targetHeadY, targetZ)[1];
        float pitchToChest = RotationUtils.getRotations(targetX, targetChestY, targetZ)[1];
        float pitchToLegs = RotationUtils.getRotations(targetX, targetLegsY, targetZ)[1];

        float currentPitch = mc.thePlayer.rotationPitch;
        float diffHead = Math.abs(currentPitch - pitchToHead);
        float diffChest = Math.abs(currentPitch - pitchToChest);
        float diffLegs = Math.abs(currentPitch - pitchToLegs);

        // Hysteresis Region Selection: Lock to region unless candidate region is at least hysteresisMargin closer.
        int candidateRegion;
        if (diffHead <= diffChest && diffHead <= diffLegs) {
            candidateRegion = 1;
        } else if (diffChest <= diffLegs) {
            candidateRegion = 2;
        } else {
            candidateRegion = 3;
        }

        if (currentRegion == 0) {
            currentRegion = candidateRegion;
        } else if (currentRegion != candidateRegion) {
            float currentDiff = (currentRegion == 1) ? diffHead : (currentRegion == 2) ? diffChest : diffLegs;
            float candidateDiff = (candidateRegion == 1) ? diffHead : (candidateRegion == 2) ? diffChest : diffLegs;

            // Only switch regions if candidate is > hysteresisMargin degrees closer
            if (currentDiff - candidateDiff > hysteresisMargin) {
                currentRegion = candidateRegion;
            }
        }

        double chosenTargetY = (currentRegion == 1) ? targetHeadY : (currentRegion == 2) ? targetChestY : targetLegsY;

        // Calculate target rotation to chosen anatomical region
        float[] rawRotations = RotationUtils.getRotations(targetX, chosenTargetY, targetZ);
        float targetYaw = rawRotations[0];
        float targetPitch = rawRotations[1];

        float yawDiff = Math.abs(RotationUtils.getAngleDifference(targetYaw, mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(targetPitch - currentPitch);
        float totalDist = (float) Math.hypot(yawDiff, pitchDiff);

        // Break away if total 3D angular distance exceeds 60 degrees (prevents vertical snap at steep angles)
        if (totalDist > 60.0f) {
            return;
        }

        // Micro-stutter prevention: if within stopThreshold degrees of target, stop adjusting entirely
        if (totalDist < stopThreshold) {
            RotationHandler.enabled = false;
            return;
        }

        // Normalize distance mapped to the 60.0 degree break-away threshold (prevents speed plateau)
        float normalizedDist = Math.min(totalDist / 60.0f, 1.0f);

        // Genuine Hermite Smoothstep cubic easing: 3x^2 - 2x^3
        float smoothStepFactor = normalizedDist * normalizedDist * (3.0f - 2.0f * normalizedDist);
        float calculatedSpeed = smoothStepFactor * maxSpeed;

        RotationHandler.setRotation(
                new float[]{targetYaw, targetPitch},
                MovementCorrectionMode.Silent,
                new float[]{calculatedSpeed, calculatedSpeed * 0.7f},
                true, new float[]{0.3f, 0.3f},
                SmoothMode.Relative,
                false,
                1f
        );
    }

    private void acquireTarget() {
        EntityLivingBase newTarget = PlayerUtils.getTarget(4.5f, teamCheck.get());

        if (target != null && newTarget != null && newTarget != target && !target.isDead) {
            double currentDist = PlayerUtils.getDistanceToEntityBox(target);
            double newDist = PlayerUtils.getDistanceToEntityBox(newTarget);

            if (currentDist - newDist < 0.6) {
                newTarget = target;
            }
        }

        if (newTarget != target) {
            currentRegion = 0; // reset region commitment when switching targets
            smoothedMotionX = 0;
            smoothedMotionZ = 0;

            // Introduce subtle per-acquisition human variance so tracking speed & thresholds aren't identical every time
            maxSpeed = (float) ThreadLocalRandom.current().nextDouble(22.0, 28.0);
            stopThreshold = (float) ThreadLocalRandom.current().nextDouble(0.08, 0.14);
            hysteresisMargin = (float) ThreadLocalRandom.current().nextDouble(2.5, 3.5);
        }

        target = newTarget;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }
}