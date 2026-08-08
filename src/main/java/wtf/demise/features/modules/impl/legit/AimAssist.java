package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.combat.AntiBot;
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

    // public telemetry fields for aimrecorder to read each tick
    public float lastYawCorrection = 0;
    public float lastPitchCorrection = 0;
    public float lastCorrectionStrength = 0;
    public boolean isActive = false;

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

        // if onlyonclick is enabled, maintain active targeting for 500ms after last click release
        // 500ms covers even low cps clicking gaps without losing tracking mid-combo
        if (onlyOnClick.get() && clickTimer.hasTimeElapsed(500)) {
            target = null;
            lastYawCorrection = lastPitchCorrection = lastCorrectionStrength = 0;
            isActive = false;
            return;
        }

        acquireTarget();

        if (target == null) {
            lastYawCorrection = lastPitchCorrection = lastCorrectionStrength = 0;
            isActive = false;
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

        // no hard cutoff - aim assist works across entire field of view
        // only skip if target is literally behind the player (> 160 degrees off)
        // this covers quake pro fov (110) plus generous peripheral margin
        if (totalError > 160.0f) {
            lastYawCorrection = lastPitchCorrection = lastCorrectionStrength = 0;
            isActive = false;
            return;
        }

        // --- adaptive correction strength ---
        // uses a smooth curve that ramps up quickly for large errors
        // and eases off gently for small errors to avoid jitter
        //
        // at 0 degrees error: factor ~0 (no correction needed)
        // at 5 degrees: ~0.08 (gentle nudge within body hitbox area)
        // at 15 degrees: ~0.22 (moderate tracking)
        // at 40 degrees: ~0.50 (strong pull for off-screen targets)
        // at 80+ degrees: ~0.75+ (aggressive acquisition)
        float normalizedError = Math.min(totalError / 90.0f, 1.0f);
        float correctionStrength = (float) (1.0 - Math.pow(1.0 - normalizedError, 2.0));
        lastCorrectionStrength = correctionStrength;

        // scale correction by error to produce the actual yaw/pitch delta
        // 0.4 yaw and 0.3 pitch give smooth tracking without overshooting
        float yawCorrection = yawError * correctionStrength * 0.4f;
        float pitchCorrection = pitchError * correctionStrength * 0.3f;

        // max correction rate: 12 deg/tick = 240 deg/sec yaw, 6 deg/tick = 120 deg/sec pitch
        // fast enough to track strafing at close range while preventing snap-like movement
        yawCorrection = MathHelper.clamp_float(yawCorrection, -12.0f, 12.0f);
        pitchCorrection = MathHelper.clamp_float(pitchCorrection, -6.0f, 6.0f);

        // soft deadzone: errors below 1.5 degrees get proportionally reduced corrections
        // prevents micro-oscillation around target center without fully freezing the aim
        if (totalError < 1.5f) {
            float deadzoneFactor = totalError / 1.5f;
            yawCorrection *= deadzoneFactor;
            pitchCorrection *= deadzoneFactor;
        }

        // apply correction directly to the player's physical camera
        // additive blending: mouse input from the engine is already applied before this hook,
        // so this adds on top of the player's own aiming without replacing or blocking it
        // store final corrections for telemetry before applying
        lastYawCorrection = yawCorrection;
        lastPitchCorrection = pitchCorrection;
        isActive = true;

        mc.thePlayer.rotationYaw += yawCorrection;
        mc.thePlayer.rotationPitch += pitchCorrection;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0f, 90.0f);
    }

    private void acquireTarget() {
        // search within 6 blocks - covers all realistic pvp engagement distances
        // the recorder showed meaningful data at 5-8 blocks, 4.5 was too restrictive
        EntityLivingBase bestTarget = null;
        float bestAngle = Float.MAX_VALUE;
        double bestDist = Double.MAX_VALUE;

        if (mc.theWorld == null) {
            target = null;
            return;
        }

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer) continue;
            if (entity.isDead) continue;
            if (teamCheck.get() && PlayerUtils.isInTeam(entity)) continue;

            // check if antibot considers this entity a bot
            AntiBot antiBot = (AntiBot) Demise.INSTANCE.getModuleManager().getModule(AntiBot.class);
            if (antiBot.isEnabled() && antiBot.bots.contains(entity)) continue;

            double dist = PlayerUtils.getDistanceToEntityBox(entity);
            if (dist > 6.0) continue;

            // compute angular distance from crosshair to this entity's chest
            float[] rots = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight() * 0.65, entity.posZ);
            float yawDiff = Math.abs(RotationUtils.getAngleDifference(rots[0], mc.thePlayer.rotationYaw));
            float pitchDiff = Math.abs(rots[1] - mc.thePlayer.rotationPitch);
            float angleDist = (float) Math.hypot(yawDiff, pitchDiff);

            // pick the target closest to crosshair center
            // if angles are very similar (within 5 degrees), prefer the physically closer one
            if (bestTarget == null || angleDist < bestAngle - 5.0f || (angleDist < bestAngle + 5.0f && dist < bestDist)) {
                bestTarget = entity;
                bestAngle = angleDist;
                bestDist = dist;
            }
        }

        // sticky targeting: don't switch away from current target unless the new one is
        // significantly better (> 15 degrees closer to crosshair or current target died/despawned)
        if (target != null && !target.isDead && bestTarget != null && bestTarget != target) {
            float[] currentRots = RotationUtils.getRotations(target.posX, target.posY + target.getEyeHeight() * 0.65, target.posZ);
            float currentAngle = (float) Math.hypot(
                Math.abs(RotationUtils.getAngleDifference(currentRots[0], mc.thePlayer.rotationYaw)),
                Math.abs(currentRots[1] - mc.thePlayer.rotationPitch)
            );

            double currentDist = PlayerUtils.getDistanceToEntityBox(target);
            if (currentDist <= 6.0 && bestAngle > currentAngle - 15.0f) {
                // current target is still viable and new target isn't dramatically better
                bestTarget = target;
            }
        }

        target = bestTarget;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }
}