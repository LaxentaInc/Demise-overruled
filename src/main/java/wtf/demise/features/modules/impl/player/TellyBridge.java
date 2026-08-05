package wtf.demise.features.modules.impl.player;

import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
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
import wtf.demise.utils.player.rotation.enums.MovementCorrectionMode;
import wtf.demise.utils.player.rotation.enums.SmoothMode;

@ModuleInfo(name = "TellyBridge", description = "Executes strict fixed-cardinal, line-anchored legit Telly Bridging.")
public class TellyBridge extends Module {
    private float initialYaw;
    private float targetYaw;
    private float targetPitch;
    private boolean isLocked;
    private double anchorX;
    private double anchorZ;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        initialYaw = Math.round(mc.thePlayer.rotationYaw / 90.0f) * 90.0f;
        isLocked = true;
        anchorX = Math.floor(mc.thePlayer.posX) + 0.5;
        anchorZ = Math.floor(mc.thePlayer.posZ) + 0.5;
    }

    @Override
    public void onDisable() {
        isLocked = false;
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Auto-select block slot in hotbar
        int blockSlot = getBlockSlot();
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot;
        }

        // Lock anchor line once on ground if not set
        if (!isLocked && mc.thePlayer.onGround) {
            initialYaw = Math.round(mc.thePlayer.rotationYaw / 90.0f) * 90.0f;
            isLocked = true;
        }

        boolean onGround = mc.thePlayer.onGround;

        // Check if player is approaching a block edge on ground to initiate jump
        if (onGround && MoveUtil.isMoving()) {
            double nextX = mc.thePlayer.posX + -Math.sin(Math.toRadians(initialYaw)) * 0.6;
            double nextZ = mc.thePlayer.posZ + Math.cos(Math.toRadians(initialYaw)) * 0.6;
            BlockPos edgePos = new BlockPos(nextX, mc.thePlayer.posY - 1, nextZ);
            boolean isAirEdge = mc.theWorld.getBlockState(edgePos).getBlock() instanceof BlockAir;

            if (isAirEdge) {
                int absYaw = Math.abs(Math.round(initialYaw)) % 360;
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
                // leap phase: turn camera backward
                targetYaw = initialYaw - 180.0f; // instantly target full backward to prevent mid-turn spiraling
                targetPitch = 45.0f;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            } else if (airTicks >= 3 && airTicks <= 7) {
                // placement phase: face 180 degrees backward along fixed initial cardinal axis and pitch to 73.5 deg
                targetYaw = initialYaw - 180.0f;
                targetPitch = 73.5f;

                // hold physical right-click and place block
                boolean placeWindow = airTicks >= 4;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), placeWindow);

                if (placeWindow) {
                    placeBlockUnderFeet();
                }
            } else {
                // return phase: face forward fixed cardinal direction
                targetYaw = initialYaw;
                targetPitch = 15.0f;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }

            // smooth physical camera movement (silent = false)
            RotationHandler.setRotation(
                    new float[]{targetYaw, targetPitch},
                    MovementCorrectionMode.Strict, // strict decoupling of movement from visual camera
                    new float[]{80.0f, 80.0f}, // slightly faster turn speed
                    true,
                    new float[]{0.1f, 0.1f},
                    SmoothMode.Relative,
                    false,
                    1.0f
            );
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode()));
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mc.thePlayer == null) return;

        int airTicks = mc.thePlayer.offGroundTicks;

        // Calculate reference line micro-correction strafe to keep player 100% dead-center on the block line
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
            e.setForward(1.0f); // preserve forward momentum during initial leap
            e.setStrafe(strafeCorrection);
        } else if (airTicks >= 3 && airTicks <= 7) {
            // Because we use MovementCorrectionMode.Strict, we can simply pass forward input as 1.0f relative to initialYaw!
            // The strict correction will mathematically convert e.setForward(1.0f) into perfect backward world movement 
            // when targetYaw is initialYaw - 180, regardless of visual mid-spin camera angles!
            e.setForward(1.0f);
            e.setStrafe(strafeCorrection);
        } else {
            e.setForward(0.0f);
            e.setStrafe(strafeCorrection);
        }
    }

    private void placeBlockUnderFeet() {
        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            return;
        }

        BlockPos targetPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = targetPos.offset(facing);
            if (!(mc.theWorld.getBlockState(neighbor).getBlock() instanceof BlockAir)) {
                Vec3 hitVec = new Vec3(neighbor.getX() + 0.5 + facing.getFrontOffsetX() * 0.5,
                        neighbor.getY() + 0.5 + facing.getFrontOffsetY() * 0.5,
                        neighbor.getZ() + 0.5 + facing.getFrontOffsetZ() * 0.5);

                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), neighbor, facing.getOpposite(), hitVec)) {
                    mc.thePlayer.swingItem();
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                }
                break;
            }
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
}
