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

        // auto select block slot in hotbar
        int blockSlot = getBlockSlot();
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot;
        }

        boolean onGround = mc.thePlayer.onGround;

        // hard clamp off-axis velocity to zero out any side drift completely
        int absYaw = Math.abs(Math.round(initialYaw)) % 360;
        if (absYaw == 0 || absYaw == 180) {
            mc.thePlayer.motionX = 0.0;
        } else {
            mc.thePlayer.motionZ = 0.0;
        }

        // check if player is approaching a block edge on ground to initiate jump
        if (onGround && MoveUtil.isMoving()) {
            double nextX = mc.thePlayer.posX + -Math.sin(Math.toRadians(initialYaw)) * 0.6;
            double nextZ = mc.thePlayer.posZ + Math.cos(Math.toRadians(initialYaw)) * 0.6;
            BlockPos edgePos = new BlockPos(nextX, mc.thePlayer.posY - 1, nextZ);
            boolean isAirEdge = mc.theWorld.getBlockState(edgePos).getBlock() instanceof BlockAir;

            if (isAirEdge) {
                if (absYaw == 0 || absYaw == 180) {
                    anchorX = Math.floor(mc.thePlayer.posX) + 0.5;
                } else {
                    anchorZ = Math.floor(mc.thePlayer.posZ) + 0.5;
                }

                mc.thePlayer.jump();
                mc.thePlayer.setSprinting(true);
            }
        }

        int airTicks = mc.thePlayer.offGroundTicks;

        if (!onGround) {
            if (airTicks >= 1 && airTicks <= 2) {
                // leap phase: turn camera backward towards initial yaw - 180
                targetYaw = initialYaw - 180.0f;
                targetPitch = 56.4f;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            } else if (airTicks >= 3 && airTicks <= 8) {
                // placement phase: face backward along cardinal heading
                targetYaw = initialYaw - 180.0f;

                BlockData blockData = findBlockData();
                if (blockData != null) {
                    float[] rots = RotationUtils.getRotations(blockData.pos, blockData.facing);
                    targetPitch = rots[1];
                } else {
                    targetPitch = 68.5f;
                }

                boolean placeWindow = airTicks >= 3 && airTicks <= 8;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), placeWindow);

                if (placeWindow && blockData != null) {
                    placeBlock(blockData);
                }
            } else {
                // return phase: return camera forward to initial yaw
                targetYaw = initialYaw;
                targetPitch = 20.0f;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        } else {
            // ground phase: face forward initial yaw direction cleanly
            targetYaw = initialYaw;
            targetPitch = 20.0f;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()));
        }

        // hard lock visual player camera directly to target yaw and pitch to eliminate mouse-step accumulation
        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;

        // camera rotation and packet synchronization with strict movement correction
        RotationHandler.setRotation(
                new float[]{targetYaw, targetPitch},
                MovementCorrectionMode.Strict,
                new float[]{180.0f, 180.0f},
                false,
                new float[]{0.0f, 0.0f},
                SmoothMode.Linear,
                true,
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
            e.setForward(1.0f);
            e.setStrafe(strafeCorrection);
        } else if (airTicks >= 3 && airTicks <= 8) {
            e.setForward(-1.0f);
            e.setStrafe(strafeCorrection);
        } else {
            e.setForward(1.0f);
            e.setStrafe(strafeCorrection);
        }
    }

    private BlockData findBlockData() {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, initialY, mc.thePlayer.posZ);
        EnumFacing moveFacing = EnumFacing.fromAngle(initialYaw);
        EnumFacing backFacing = moveFacing.getOpposite();

        // search up to 3 blocks back along movement line for nearest solid block face
        for (int i = 0; i <= 3; i++) {
            BlockPos searchPos = playerPos.offset(backFacing, i);
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos neighbor = searchPos.offset(facing);
                if (!(mc.theWorld.getBlockState(neighbor).getBlock() instanceof BlockAir)) {
                    return new BlockData(neighbor, facing.getOpposite());
                }
            }
        }
        return null;
    }

    private void placeBlock(BlockData data) {
        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            return;
        }

        Vec3 hitVec = new Vec3(
                data.pos.getX() + 0.5 + data.facing.getFrontOffsetX() * 0.5,
                data.pos.getY() + 0.5 + data.facing.getFrontOffsetY() * 0.5,
                data.pos.getZ() + 0.5 + data.facing.getFrontOffsetZ() * 0.5
        );

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), data.pos, data.facing, hitVec)) {
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
