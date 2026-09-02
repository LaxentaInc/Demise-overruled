package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "LegitStrafe", description = "Intelligently strafes to maintain an angle at the edge of the target's FOV.")
public class LegitStrafe extends Module {

    // The angle (in degrees) to maintain from the target's crosshair.
    // 45 degrees is generally a good "edge of screen" angle.
    private final SliderValue targetAngle = new SliderValue("Target Angle", 45, 20, 90, 1, this);

    // Buffer to prevent micro-stuttering when riding the exact angle.
    private final float angleBuffer = 3.0f;

    @Override
    public void onEnable() {
        // Nothing needed on enable
    }

    @EventTarget
    @EventPriority(3)
    public void onMoveInput(MoveInputEvent event) {
        if (mc.thePlayer == null) return;

        AimAssist aimAssist = (AimAssist) getModule(AimAssist.class);
        EntityLivingBase target = aimAssist.getTarget();

        // Only activate if we are aiming at someone and trying to engage (holding W)
        if (target != null && event.getForward() > 0) {

            // If user is manually holding A or D, respect their override
            if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                event.setStrafe(1.0f);
                return;
            } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
                event.setStrafe(-1.0f);
                return;
            }

            // Calculate where we are relative to their look direction
            float angleToUs = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
            
            // diff is how many degrees off from their center crosshair we are.
            // Positive diff means we are to their left. Negative means we are to their right.
            float diff = MathHelper.wrapAngleTo180_float(angleToUs - target.rotationYawHead);
            
            float absDiff = Math.abs(diff);
            float currentTargetAngle = targetAngle.get();

            // If we are too close to their center crosshair, strafe outward to reach the target angle.
            if (absDiff < currentTargetAngle - angleBuffer) {
                // If diff is positive (we are to their left), we want to strafe right to go further left (relative to them).
                // Our right strafe (-1) moves us left from their perspective.
                // Our left strafe (1) moves us right from their perspective.
                // If diff > 0 (we are left of their crosshair), move further left -> setStrafe(1.0f)
                // If diff < 0 (we are right of their crosshair), move further right -> setStrafe(-1.0f)
                
                if (diff > 0) {
                    event.setStrafe(1.0f); // Strafe left (moves us to their left)
                } else {
                    event.setStrafe(-1.0f); // Strafe right (moves us to their right)
                }
            } 
            // If we are beyond the target angle, we could potentially strafe inward to maintain the exact angle,
            // but usually it's better to just stop strafing and let forward momentum carry us,
            // or just let the player naturally drift. 
            // For edge-riding, we just stop forcing strafe if we've reached the safe zone.
            else {
                event.setStrafe(0.0f);
            }
        }
    }
}
