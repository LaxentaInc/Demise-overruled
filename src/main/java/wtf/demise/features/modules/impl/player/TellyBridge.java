package wtf.demise.features.modules.impl.player;

import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.player.rotation.enums.MovementCorrectionMode;
import wtf.demise.utils.player.rotation.enums.SmoothMode;

@ModuleInfo(name = "TellyBridge", description = "Executes strict fixed-cardinal, zero-drift line-anchored Telly Bridging.")
public class TellyBridge extends Module {
    private float initialYaw;
    private float targetYaw;
    private float targetPitch;
    private double anchorX;
    private double anchorZ;
    private double initialY;

    // per-jump cached jitter values to prevent per-tick 20hz camera noise
    private float jumpYawJitter = 0.0f;
    private float jumpPitchJitter = 0.0f;
    private Vec3 jumpHitVecOffset = new Vec3(0, 0, 0);

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        // lock initial cardinal direction (0, 90, 180, 270 / -90, -180, -270) permanently upon enabling module
        initialYaw = Math.round(mc.thePlayer.rotationYaw / 90.0f) * 90.0f;
        anchorX = Math.floor(mc.thePlayer.posX) + 0.5;
        anchorZ = Math.floor(mc.thePlayer.posZ) + 0.5;
        initialY = Math.floor(mc.thePlayer.posY - 1.0);
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // forcibly maintain sprint flag at all times to prevent mid-air momentum loss
        mc.thePlayer.setSprinting(true);

        // auto select block slot in hotbar
        int blockSlot = getBlockSlot();
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot;
        }

        boolean onGround = mc.thePlayer.onGround;

        // update initialY when on ground to allow starting a new bridge plane if the player dropped down
        if (onGround) {
            initialY = Math.floor(mc.thePlayer.posY - 1.0);
        }

        // check if player is approaching a block edge on ground to initiate jump
        int absYaw = Math.abs(Math.round(initialYaw)) % 360;
        if (onGround && MoveUtil.isMoving()) {
            double threshold = 0.15 + (Math.random() * 0.1); // 0.15 to 0.25 randomized jump edge
            double nextX = mc.thePlayer.posX + -Math.sin(Math.toRadians(initialYaw)) * threshold;
            double nextZ = mc.thePlayer.posZ + Math.cos(Math.toRadians(initialYaw)) * threshold;
            BlockPos edgePos = new BlockPos(nextX, mc.thePlayer.posY - 1, nextZ);
            boolean isAirEdge = mc.theWorld.getBlockState(edgePos).getBlock() instanceof BlockAir;

            if (isAirEdge) {
                if (absYaw == 0 || absYaw == 180) {
                    anchorX = Math.floor(mc.thePlayer.posX) + 0.5;
                } else {
                    anchorZ = Math.floor(mc.thePlayer.posZ) + 0.5;
                }

                // generate per-jump jitter once at jump initiation
                jumpYawJitter = (float) ((Math.random() - 0.5) * 2.0);
                jumpPitchJitter = (float) ((Math.random() - 0.5) * 1.5);
                jumpHitVecOffset = new Vec3((Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08);

                mc.thePlayer.jump();
                mc.thePlayer.setSprinting(true);
            }
        }

        int airTicks = mc.thePlayer.offGroundTicks;

        // cache block data once per tick to guarantee consistency
        BlockData cachedBlockData = (!onGround && airTicks >= 3) ? findBlockData() : null;

        if (!onGround) {
            if (airTicks >= 1 && airTicks <= 2) {
                // human telly phase 1: leap forward facing movement heading cleanly
                targetYaw = initialYaw;
                targetPitch = 20.0f;
            } else if (airTicks >= 3 && airTicks <= 5) {
                // human telly phase 2: flick camera backward towards target block face
                targetYaw = initialYaw - 180.0f;
                if (cachedBlockData != null) {
                    Vec3 hitVec = RotationUtils.getVec3(cachedBlockData.pos, cachedBlockData.facing);
                    float[] rots = RotationUtils.getRotations(hitVec);
                    targetPitch = rots[1];
                } else {
                    targetPitch = 72.2f;
                }
            } else {
                // human telly phase 3: descent placement window (airticks 6-13) derived from human telemetry
                targetYaw = initialYaw - 180.0f + jumpYawJitter;

                if (cachedBlockData != null) {
                    Vec3 hitVec = RotationUtils.getVec3(cachedBlockData.pos, cachedBlockData.facing).add(jumpHitVecOffset);
                    float[] rots = RotationUtils.getRotations(hitVec);
                    targetPitch = rots[1] + jumpPitchJitter;
                } else {
                    targetPitch = 72.2f + jumpPitchJitter;
                }

                boolean placeWindow = airTicks >= 6 && airTicks <= 13;

                if (placeWindow && cachedBlockData != null) {
                    placeBlock(cachedBlockData);
                }
            }
        } else {
            // ground phase: face forward initial yaw direction cleanly
            targetYaw = initialYaw;
            targetPitch = 20.0f;
            mc.thePlayer.setSprinting(true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()));
        }

        // dynamic human camera rotation speeds based on 10k tick telemetry
        float yawSpeed = (airTicks <= 7) ? 180.0f + (float)(Math.random() * 25.0) : 30.0f + (float)(Math.random() * 10.0);
        float pitchSpeed = (airTicks <= 7) ? 180.0f + (float)(Math.random() * 25.0) : 20.0f + (float)(Math.random() * 5.0);

        // 100% physical: apply rotations directly to the player camera
        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;

        // single authoritative rotation pipeline
        RotationHandler.setRotation(
                new float[]{targetYaw, targetPitch},
                MovementCorrectionMode.Strict,
                new float[]{yawSpeed, pitchSpeed},
                false,
                new float[]{0.0f, 0.0f},
                SmoothMode.Linear,
                false,
                1.0f
        );
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mc.thePlayer == null) return;

        int airTicks = mc.thePlayer.offGroundTicks;

        float strafeCorrection = 0.0f;
        int absYaw = Math.abs(Math.round(initialYaw)) % 360;
        if (absYaw == 0 || absYaw == 180) {
            double offset = mc.thePlayer.posX - anchorX;
            strafeCorrection = (float) -MathHelper.clamp_double(offset * 2.5, -0.4, 0.4);
        } else {
            double offset = mc.thePlayer.posZ - anchorZ;
            strafeCorrection = (float) MathHelper.clamp_double(offset * 2.5, -0.4, 0.4);
        }

        if (mc.thePlayer.onGround) {
            e.setForward(1.0f);
            e.setStrafe(strafeCorrection);
        } else if (airTicks >= 1 && airTicks <= 2) {
            // forward leap momentum
            e.setForward(1.0f);
            e.setStrafe(strafeCorrection);
        } else {
            // coasting backward naturally without artificial mid-air boosting
            e.setForward(0.0f);
            e.setStrafe(strafeCorrection);
        }
    }

    private BlockData findBlockData() {
        int feetY = (int) Math.floor(mc.thePlayer.posY - 0.5);
        EnumFacing moveFacing = EnumFacing.fromAngle(initialYaw);
        EnumFacing backFacing = moveFacing.getOpposite();

        // search Y levels near player feet (current level and level below)
        for (int yOffset = 0; yOffset >= -1; yOffset--) {
            BlockPos basePos = new BlockPos(mc.thePlayer.posX, feetY + yOffset, mc.thePlayer.posZ);

            // search up to 4 blocks back along movement line for nearest solid block
            for (int i = 0; i <= 4; i++) {
                BlockPos searchPos = basePos.offset(backFacing, i);
                for (EnumFacing facing : EnumFacing.values()) {
                    if (facing == EnumFacing.UP) {
                        continue; // skip ceiling placements (clicking the bottom face of a block above us)
                    }
                    
                    BlockPos neighbor = searchPos.offset(facing);

                    // if clicking the top face of a block, ensure it's below our feet for strict line-of-sight
                    if (facing == EnumFacing.DOWN && neighbor.getY() >= mc.thePlayer.posY) {
                        continue;
                    }

                    if (!(mc.theWorld.getBlockState(neighbor).getBlock() instanceof BlockAir)) {
                        return new BlockData(neighbor, facing.getOpposite());
                    }
                }
            }
        }
        return null;
    }

    private void placeBlock(BlockData data) {
        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            return;
        }

        // update client raytrace mouseover to get exact crosshair hit target
        mc.entityRenderer.getMouseOver(1.0f);

        // strictly verify that the crosshair is physically aiming at the target block face
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        if (!mc.objectMouseOver.getBlockPos().equals(data.pos)) {
            return;
        }

        Vec3 hitVec = mc.objectMouseOver.hitVec;

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, hitVec)) {
            mc.thePlayer.swingItem();
            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }
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

    private static class BlockData {
        public final BlockPos pos;
        public final EnumFacing facing;

        public BlockData(BlockPos pos, EnumFacing facing) {
            this.pos = pos;
            this.facing = facing;
        }
    }
}




