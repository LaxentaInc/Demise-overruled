package wtf.demise.features.modules.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TrapBlock", description = "Flicks and places a wall of blocks in front of an approaching enemy to break momentum.")
public class TrapBlock extends Module {
    private final SliderValue minDistance = new SliderValue("Min Distance", 3.5f, 2.0f, 5.0f, 0.1f, this);
    private final SliderValue maxDistance = new SliderValue("Max Distance", 7.0f, 5.0f, 10.0f, 0.1f, this);
    private final SliderValue blocksToPlace = new SliderValue("Blocks to Place", 2f, 1f, 3f, 1f, this);
    private final SliderValue cooldown = new SliderValue("Cooldown (ms)", 1500f, 500f, 3000f, 50f, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    private final TimerUtils cooldownTimer = new TimerUtils();

    // state for flick-and-return tracking
    private boolean isFlicking = false;
    private int flickTicks = 0;
    private float originalYaw;
    private float originalPitch;
    private int originalSlot;

    @Override
    public void onDisable() {
        if (isFlicking) {
            restoreState();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // handle the flick and restore sequence
        if (isFlicking) {
            flickTicks++;
            if (flickTicks > 1) { // 1 tick to place, next tick restore
                restoreState();
            }
            return;
        }

        // only trigger if we are ready and NOT clicking (meaning we are not actively trading hits)
        if (!cooldownTimer.hasTimeElapsed((long) cooldown.get()) || Mouse.isButtonDown(0)) {
            return;
        }

        int blockSlot = getBlockSlot();
        if (blockSlot == -1) return;

        EntityLivingBase target = getValidTarget();
        if (target == null) return;

        // Predict where they are going and get placement targets
        List<BlockPos> trapPositions = calculateTrapPositions(target);
        if (trapPositions.isEmpty()) return;

        executeTrap(trapPositions, blockSlot);
    }

    private EntityLivingBase getValidTarget() {
        EntityLivingBase bestTarget = null;
        double minDistanceFound = Double.MAX_VALUE;

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer || entity.isDead) continue;
            if (teamCheck.get() && PlayerUtils.isInTeam(entity)) continue;
            
            AntiBot antiBot = (AntiBot) Demise.INSTANCE.getModuleManager().getModule(AntiBot.class);
            if (antiBot.isEnabled() && antiBot.bots.contains(entity)) continue;

            double dist = PlayerUtils.getDistanceToEntityBox(entity);
            if (dist >= minDistance.get() && dist <= maxDistance.get()) {
                if (dist < minDistanceFound) {
                    minDistanceFound = dist;
                    bestTarget = entity;
                }
            }
        }
        return bestTarget;
    }

    private List<BlockPos> calculateTrapPositions(EntityLivingBase target) {
        List<BlockPos> positions = new ArrayList<>();
        
        // estimate direction target is moving
        double velX = target.posX - target.lastTickPosX;
        double velZ = target.posZ - target.lastTickPosZ;
        
        // if standing still, use their look direction
        if (Math.hypot(velX, velZ) < 0.05) {
            velX = -MathHelper.sin(target.rotationYaw / 180.0F * (float) Math.PI);
            velZ = MathHelper.cos(target.rotationYaw / 180.0F * (float) Math.PI);
        }

        // normalize
        double length = Math.hypot(velX, velZ);
        if (length > 0) {
            velX /= length;
            velZ /= length;
        }

        // find the block they are roughly going to step on next
        int targetX = MathHelper.floor_double(target.posX + velX * 1.5);
        int targetY = MathHelper.floor_double(target.posY);
        int targetZ = MathHelper.floor_double(target.posZ + velZ * 1.5);

        // construct a line of blocks straight ahead
        for (int i = 0; i < blocksToPlace.get(); i++) {
            BlockPos pos = new BlockPos(targetX + (velX * i), targetY, targetZ + (velZ * i));
            
            // check if we can place here (must be air, and block below must be solid)
            if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir) {
                Block blockBelow = mc.theWorld.getBlockState(pos.down()).getBlock();
                if (!(blockBelow instanceof BlockAir)) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    private void executeTrap(List<BlockPos> positions, int blockSlot) {
        // save state
        originalYaw = mc.thePlayer.rotationYaw;
        originalPitch = mc.thePlayer.rotationPitch;
        originalSlot = mc.thePlayer.inventory.currentItem;

        // switch to block physically to bypass anticheats
        mc.thePlayer.inventory.currentItem = blockSlot;

        // try to place the sequence
        boolean placed = false;
        
        for (BlockPos pos : positions) {
            // aim at the block below the target pos
            BlockPos supportBlock = pos.down();
            float[] rots = RotationUtils.getRotations(supportBlock, EnumFacing.UP);
            
            // snap camera
            mc.thePlayer.rotationYaw = rots[0];
            mc.thePlayer.rotationPitch = rots[1];
            
            // force update raytrace to new camera angle immediately
            mc.entityRenderer.getMouseOver(1.0f);
            
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                if (mc.objectMouseOver.getBlockPos().equals(supportBlock)) {
                    // physical click
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec)) {
                        mc.thePlayer.swingItem();
                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        placed = true;
                    }
                }
            }
        }

        if (placed) {
            isFlicking = true;
            flickTicks = 0;
            cooldownTimer.reset();
        } else {
            // if we failed to place anything, restore immediately so we don't look dumb
            restoreState();
        }
    }

    private void restoreState() {
        mc.thePlayer.rotationYaw = originalYaw;
        mc.thePlayer.rotationPitch = originalPitch;
        mc.thePlayer.inventory.currentItem = originalSlot;
        isFlicking = false;
        flickTicks = 0;
    }

    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 0) {
                return i;
            }
        }
        return -1;
    }
}
