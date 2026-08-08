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
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "AimAssist", description = "Assists in aiming smoothly.")
public class AimAssist extends Module {
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    private EntityLivingBase target;
    private final TimerUtils clickTimer = new TimerUtils();

    @Override
    public void onDisable() {
        target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        // reset click timer whenever attack button is down
        if (Mouse.isButtonDown(0)) {
            clickTimer.reset();
        }

        // if onlyonclick is enabled, maintain active targeting for 300ms after last click release
        // prevents micro-releases during fast cps clicking from resetting tracking
        if (onlyOnClick.get() && clickTimer.hasTimeElapsed(300)) {
            target = null;
            return;
        }

        acquireTarget();

        if (target == null) {
            return;
        }

        // use standard partial-tick frame interpolation for smooth target position
        float partialTicks = mc.timer.renderPartialTicks;
        double targetX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
        double targetZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;

        // aim at chest height (65% of eye height) - single stable aim point, no region switching
        double chestY = targetY + target.getEyeHeight() * 0.65;

        // calculate ideal rotation to target chest
        float[] idealRotation = RotationUtils.getRotations(targetX, chestY, targetZ);
        float idealYaw = idealRotation[0];
        float idealPitch = idealRotation[1];

        // compute angular error from current camera to ideal aim point
        float yawError = RotationUtils.getAngleDifference(idealYaw, mc.thePlayer.rotationYaw);
        float pitchError = idealPitch - mc.thePlayer.rotationPitch;
        float totalError = (float) Math.hypot(yawError, pitchError);

        // disengage if target is wildly off-screen (> 60 degrees total angular distance)
        if (totalError > 60.0f) {
            return;
        }

        // --- adaptive correction strength ---
        // scales from gentle nudge when close to aggressive pull when far off
        // this prevents the "locking" feel at small errors and the "slow" feel at large errors
        //
        // at 0 degrees error: factor ~0 (no correction needed)
        // at 10 degrees error: factor ~0.35 (moderate pull)
        // at 30 degrees error: factor ~0.65 (strong pull)
        // at 60 degrees error: factor ~0.85 (near full tracking)
        float normalizedError = Math.min(totalError / 60.0f, 1.0f);
        float correctionStrength = (float) (1.0 - Math.pow(1.0 - normalizedError, 2.5));

        // apply correction as a fraction of the total error
        // this is additive to mouse input - it doesn't replace or override mouse movement
        // the player's own mouse delta is applied normally by the engine before this runs
        float yawCorrection = yawError * correctionStrength * 0.45f;
        float pitchCorrection = pitchError * correctionStrength * 0.35f;

        // clamp maximum correction per tick to prevent jarring snaps
        // 8 degrees/tick at 20tps = 160 degrees/sec max correction rate (enough for fast strafing)
        yawCorrection = MathHelper.clamp_float(yawCorrection, -8.0f, 8.0f);
        pitchCorrection = MathHelper.clamp_float(pitchCorrection, -4.0f, 4.0f);

        // deadzone: if error is tiny (< 2 degrees), scale correction down to near-zero
        // prevents the aim from "buzzing" or micro-oscillating around the target center
        if (totalError < 2.0f) {
            float deadzoneFactor = totalError / 2.0f;
            yawCorrection *= deadzoneFactor;
            pitchCorrection *= deadzoneFactor;
        }

        // apply correction directly to the player's physical camera
        // this blends seamlessly with mouse input because it's additive, not replacing
        mc.thePlayer.rotationYaw += yawCorrection;
        mc.thePlayer.rotationPitch += pitchCorrection;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0f, 90.0f);
    }

    private void acquireTarget() {
        EntityLivingBase newTarget = PlayerUtils.getTarget(4.5f, teamCheck.get());

        // sticky targeting: don't switch to a new target unless it's significantly closer
        // prevents aim from jumping between two equidistant players
        if (target != null && newTarget != null && newTarget != target && !target.isDead) {
            double currentDist = PlayerUtils.getDistanceToEntityBox(target);
            double newDist = PlayerUtils.getDistanceToEntityBox(newTarget);

            if (currentDist - newDist < 0.6) {
                newTarget = target;
            }
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