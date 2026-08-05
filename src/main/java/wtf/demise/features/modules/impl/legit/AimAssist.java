package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;

import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.player.rotation.enums.MovementCorrectionMode;
import wtf.demise.utils.player.rotation.enums.SmoothMode;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "AimAssist", description = "Assists in aiming smoothly.")
public class AimAssist extends Module {
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    private EntityLivingBase target;
    private final TimerUtils clickTimer = new TimerUtils();

    // Hysteresis tracking: 0 = None/Unset, 1 = Head, 2 = Chest, 3 = Legs
    private int currentRegion = 0;

    @Override
    public void onDisable() {
        RotationHandler.enabled = false;
        RotationHandler.targetRotation = null;
        RotationHandler.reset = true;
        target = null;
        currentRegion = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        // Reset click timer whenever attack button is down
        if (Mouse.isButtonDown(0)) {
            clickTimer.reset();
        }

        // If onlyOnClick is enabled, maintain active targeting for 300ms after last click release
        // This prevents micro-releases during fast CPS clicking from constantly setting target = null and resetting tracking
        if (onlyOnClick.get() && clickTimer.hasTimeElapsed(300)) {
            target = null;
            currentRegion = 0;
            return;
        }

        acquireTarget();

        if (target == null) {
            currentRegion = 0;
            return;
        }

        // Use standard partial-tick frame interpolation for target center position
        float partialTicks = mc.timer.renderPartialTicks;
        double targetX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
        double targetZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;

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

        // Hysteresis Region Selection: Lock to region unless candidate region is at least 3 degrees closer.
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

            if (currentDiff - candidateDiff > 3.0f) {
                currentRegion = candidateRegion;
            }
        }

        double chosenTargetY = (currentRegion == 1) ? targetHeadY : (currentRegion == 2) ? targetChestY : targetLegsY;

        // Calculate target rotation to chosen anatomical region (directly to entity center axis)
        float[] rawRotations = RotationUtils.getRotations(targetX, chosenTargetY, targetZ);
        float targetYaw = rawRotations[0];
        float targetPitch = rawRotations[1];

        float yawDiff = Math.abs(RotationUtils.getAngleDifference(targetYaw, mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(targetPitch - currentPitch);
        float totalDist = (float) Math.hypot(yawDiff, pitchDiff);

        // Break away if total 3D angular distance exceeds 60 degrees
        if (totalDist > 60.0f) {
            return;
        }

        // Maintain a healthy minimum speed floor (12.0 deg/sec) so that when you strafe around a player,
        // the crosshair stays locked on their center body instead of slowing down and slipping onto the outer hitbox edge!
        float normalizedDist = Math.min(totalDist / 60.0f, 1.0f);
        float trackingSpeed = 12.0f + (normalizedDist * 28.0f);

        RotationHandler.setRotation(
                new float[]{targetYaw, targetPitch},
                MovementCorrectionMode.Silent,
                new float[]{trackingSpeed, trackingSpeed * 0.75f},
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
            currentRegion = 0;
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