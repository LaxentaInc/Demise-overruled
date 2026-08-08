package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.StrafeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;

@ModuleInfo(name = "LegitStrafe", description = "Perfectly curves your movement around the target with unpredictable juggles.")
public class LegitStrafe extends Module {
    private final BoolValue autoJiggle = new BoolValue("Auto-Jiggle", true, this);
    private final SliderValue minJiggleTicks = new SliderValue("Min Jiggle Ticks", 5, 2, 20, 1, this);
    private final SliderValue maxJiggleTicks = new SliderValue("Max Jiggle Ticks", 15, 5, 40, 1, this);
    private final BoolValue targetBlindSide = new BoolValue("Target Blind Side", true, this);

    private int strafeDirection = 1;
    private int ticksUntilJiggle = 0;
    private boolean wasCollided = false;

    @Override
    public void onEnable() {
        strafeDirection = Math.random() > 0.5 ? 1 : -1;
        ticksUntilJiggle = getRandomJiggleTicks();
        wasCollided = false;
    }

    private int getRandomJiggleTicks() {
        return (int) MathUtils.randomizeFloat(minJiggleTicks.get(), maxJiggleTicks.get());
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null) return;

        // Auto-switch direction if we hit a wall
        if (mc.thePlayer.isCollidedHorizontally && !wasCollided) {
            strafeDirection *= -1;
            ticksUntilJiggle = getRandomJiggleTicks();
        }
        wasCollided = mc.thePlayer.isCollidedHorizontally;

        // Random juggling logic
        if (autoJiggle.get()) {
            ticksUntilJiggle--;
            if (ticksUntilJiggle <= 0) {
                // If targeting blind side, bias the random roll
                if (targetBlindSide.get()) {
                    AimAssist aimAssist = (AimAssist) getModule(AimAssist.class);
                    EntityLivingBase target = aimAssist.getTarget();
                    if (target != null) {
                        // Calculate where we are relative to their look direction
                        float angleToUs = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
                        float diff = MathHelper.wrapAngleTo180_float(angleToUs - target.rotationYawHead);
                        
                        // If diff is positive, we are on their left side (from their perspective).
                        // To get behind them, we need to move towards their back.
                        // We set strafeDirection based on the shortest path to their blind spot.
                        if (diff > 0) {
                            strafeDirection = -1; // Strafe right to go to their back
                        } else {
                            strafeDirection = 1;  // Strafe left to go to their back
                        }

                        // Add some randomness so it's not a perfect robot
                        if (Math.random() < 0.3) {
                            strafeDirection *= -1;
                        }
                    } else {
                        strafeDirection *= -1;
                    }
                } else {
                    strafeDirection *= -1;
                }
                
                ticksUntilJiggle = getRandomJiggleTicks();
            }
        }
    }

    @EventTarget
    @EventPriority(3)
    public void onMoveInput(MoveInputEvent event) {
        if (mc.thePlayer == null) return;

        AimAssist aimAssist = (AimAssist) getModule(AimAssist.class);
        EntityLivingBase target = aimAssist.getTarget();

        // Only activate if we are aiming at someone and trying to engage (holding W)
        if (target != null && event.getForward() > 0) {
            
            // Automated strafing logic
            if (autoJiggle.get()) {
                // If user is manually holding A or D, respect their override
                if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                    event.setStrafe(1.0f);
                } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
                    event.setStrafe(-1.0f);
                } else {
                    // User is not pressing A/D, inject our automated jiggle strafe keypress
                    event.setStrafe(strafeDirection);
                }
            }
        }
    }
}
