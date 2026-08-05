package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.ChatUtils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TellyRecorder", description = "Logs numerical, rotational, and input data during manual telly bridging.")
public class TellyRecorder extends Module {
    private final List<String> recordedTicks = new ArrayList<>();
    private int tickCounter = 0;
    private float initialYaw = 0;
    private boolean isRecording = false;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        recordedTicks.clear();
        tickCounter = 0;
        initialYaw = mc.thePlayer.rotationYaw;
        isRecording = true;
        ChatUtils.sendMessageClient("TellyRecorder started. Do your manual telly bridge now!");
    }

    @Override
    public void onDisable() {
        isRecording = false;
        if (recordedTicks.isEmpty()) {
            ChatUtils.sendMessageClient("TellyRecorder stopped. No data collected.");
            return;
        }

        File saveFile = new File(Demise.INSTANCE.getMainDir(), "telly_recording.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile, false))) {
            writer.println("=== TELLY BRIDGE TICK-BY-TICK MATHEMATICAL LOG ===");
            writer.println("Initial Yaw: " + initialYaw);
            writer.println("Total Ticks Recorded: " + recordedTicks.size());
            writer.println("-----------------------------------------------------------------------------------------------------");
            writer.println("Tick | AirTicks | Ground | PosY    | MotionY | MotionXZ | Pitch  | Yaw    | YawDelta | W | S | RClick");
            writer.println("-----------------------------------------------------------------------------------------------------");
            for (String line : recordedTicks) {
                writer.println(line);
            }
            ChatUtils.sendMessageClient("Saved " + recordedTicks.size() + " ticks to: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to save recording file: " + e.getMessage());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || !isRecording) return;

        tickCounter++;
        int airTicks = mc.thePlayer.offGroundTicks;
        boolean onGround = mc.thePlayer.onGround;
        double posY = mc.thePlayer.posY;
        double motionY = mc.thePlayer.motionY;
        double motionXZ = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
        float pitch = mc.thePlayer.rotationPitch;
        float currentYaw = mc.thePlayer.rotationYaw;
        float yawDelta = currentYaw - initialYaw;

        boolean pressW = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean pressS = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean pressRClick = Mouse.isButtonDown(1);

        String entry = String.format("%-4d | %-8d | %-6b | %-7.3f | %-7.3f | %-8.3f | %-6.1f | %-6.1f | %-8.1f | %-1b | %-1b | %-6b",
                tickCounter, airTicks, onGround, posY, motionY, motionXZ, pitch, currentYaw, yawDelta, pressW, pressS, pressRClick);

        recordedTicks.add(entry);
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!isRecording || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        String infoText = String.format("RECORDING TELLY - Ticks: %d | Air: %d | Pitch: %.1f | DeltaYaw: %.1f",
                tickCounter, mc.thePlayer.offGroundTicks, mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw - initialYaw);

        Fonts.interMedium.get(16).drawStringWithShadow(infoText, 10, sr.getScaledHeight() - 40, Color.GREEN.getRGB());
    }
}
