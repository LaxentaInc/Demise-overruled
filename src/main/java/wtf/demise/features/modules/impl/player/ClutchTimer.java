package wtf.demise.features.modules.impl.player;

import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "ClutchTimer", description = "Slows down game time when falling off bridges or into void to give extra reaction time to clutch.")
public class ClutchTimer extends Module {
    private final SliderValue speed = new SliderValue("Timer speed", 0.5f, 0.1f, 0.9f, 0.05f, this);
    private final SliderValue minDrop = new SliderValue("Min drop distance", 3.0f, 1.0f, 8.0f, 0.5f, this);
    private final SliderValue minFallDistance = new SliderValue("Min fall distance", 0.6f, 0.1f, 3.0f, 0.1f, this);
    private final BoolValue syncPlacement = new BoolValue("Sync placement", true, this);
    private final BoolValue fastPlace = new BoolValue("Fast place", true, this);
    private final BoolValue voidOnly = new BoolValue("Void only", false, this);

    private boolean active = false;
    private long lastPlaceTime = 0L;

    @Override
    public void onDisable() {
        // restore standard client game speed immediately when module is toggled off
        resetTimer();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        // ensure game speed returns to normal on world reloads or respawns
        resetTimer();
        lastPlaceTime = 0L;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        // capture outgoing block placement packets to synchronize tick rate with the server
        if (e.getState() == PacketEvent.State.OUTGOING && e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (syncPlacement.get()) {
                lastPlaceTime = System.currentTimeMillis();
                resetTimer();
            }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            return;
        }

        // guard clause: if on ground, on ladder, or in liquid, cancel slow motion and resume full speed
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.onGround || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) {
            resetTimer();
            return;
        }

        // handle right click interaction state and delay timer bypass
        boolean isRightClicking = mc.gameSettings.keyBindUseItem.isKeyDown() || Mouse.isButtonDown(1);
        if (isRightClicking && fastPlace.get()) {
            // bypass vanilla four tick placement lockout to allow responsive clutching
            mc.rightClickDelayTimer = 0;
        }

        // if the user is actively placing blocks or recently placed a block, restore vanilla tick rate
        boolean isPlacingRecently = (System.currentTimeMillis() - lastPlaceTime) < 300L;
        if (syncPlacement.get() && (isRightClicking || isPlacingRecently)) {
            resetTimer();
            return;
        }

        // only trigger when descending downward and fall distance exceeds threshold to protect harmless jumps
        boolean isDescending = mc.thePlayer.motionY < -0.15 && mc.thePlayer.fallDistance >= minFallDistance.get();
        if (!isDescending) {
            resetTimer();
            return;
        }

        // evaluate whether there is a solid block beneath the player
        boolean isFallingOverVoid = PlayerUtils.overVoid();
        boolean shouldSlow = isFallingOverVoid;

        if (!shouldSlow && !voidOnly.get()) {
            shouldSlow = isDropDeepEnough();
        }

        if (shouldSlow) {
            // apply slow motion to extend physical reaction window for clutching
            mc.timer.timerSpeed = speed.get();
            active = true;
        } else {
            resetTimer();
        }
    }

    private boolean isDropDeepEnough() {
        // inspect vertical column below the player to verify absence of landing blocks within min drop distance
        int startY = (int) Math.floor(mc.thePlayer.posY);
        int targetY = Math.max(0, startY - (int) Math.ceil(minDrop.get()));

        for (int y = startY; y >= targetY; y--) {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
            if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    private void resetTimer() {
        // restores global client timer back to 1.0 to guarantee standard tick rate
        if (active) {
            mc.timer.timerSpeed = 1.0f;
            active = false;
        }
    }
}

