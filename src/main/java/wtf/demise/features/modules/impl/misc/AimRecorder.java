package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "AimRecorder", description = "Logs detailed per-tick aim assist telemetry for debugging rotation behavior.")
public class AimRecorder extends Module {
    private final List<String> recordedTicks = new ArrayList<>();
    private int tickCounter = 0;
    private boolean isRecording = false;

    // previous tick values for computing deltas
    private float prevPlayerYaw = 0;
    private float prevPlayerPitch = 0;
    private float prevTargetPosX = 0;
    private float prevTargetPosZ = 0;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        recordedTicks.clear();
        tickCounter = 0;
        isRecording = true;
        prevPlayerYaw = mc.thePlayer.rotationYaw;
        prevPlayerPitch = mc.thePlayer.rotationPitch;
        ChatUtils.sendMessageClient("AimRecorder started. Aim telemetry active!");
    }

    @Override
    public void onDisable() {
        isRecording = false;
        if (recordedTicks.isEmpty()) {
            ChatUtils.sendMessageClient("AimRecorder stopped. No data collected.");
            return;
        }

        File saveFile = new File(Demise.INSTANCE.getMainDir(), "aim_recording.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile, false))) {
            writer.println("=== AIM RECORDER TICK-BY-TICK ROTATION TELEMETRY LOG ===");
            writer.println("Total Ticks Recorded: " + recordedTicks.size());
            writer.println("Column Legend:");
            writer.println("  Tick    = game tick counter");
            writer.println("  PlyYaw  = player's actual yaw (client camera)");
            writer.println("  PlyPit  = player's actual pitch (client camera)");
            writer.println("  dYaw    = yaw change this tick (how much camera actually moved)");
            writer.println("  dPitch  = pitch change this tick");
            writer.println("  IdlYaw  = ideal yaw to target body center (what perfect aim would be)");
            writer.println("  IdlPit  = ideal pitch to target chest");
            writer.println("  ErrYaw  = yaw error (ideal - actual, positive = need to rotate right)");
            writer.println("  ErrPit  = pitch error (ideal - actual, positive = need to look down)");
            writer.println("  ErrTot  = total angular error sqrt(errYaw^2 + errPit^2)");
            writer.println("  RHYaw   = rotationhandler's internal current yaw (server/spoofed)");
            writer.println("  RHPit   = rotationhandler's internal current pitch");
            writer.println("  RHAct   = rotationhandler enabled flag (true = aim assist is actively controlling)");
            writer.println("  Dist    = distance to target hitbox");
            writer.println("  TgtVel  = target horizontal velocity (blocks/tick)");
            writer.println("  TgtY    = target posY");
            writer.println("  TgtHT   = target hurtTime");
            writer.println("  PlyHT   = player hurtTime");
            writer.println("  LClick  = left mouse button down");
            writer.println("  PlyGnd  = player on ground");
            writer.println("  TgtGnd  = target on ground");
            writer.println("  PlyMXZ  = player horizontal speed");
            writer.println("  MSens   = mouse sensitivity setting value");
            writer.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            writer.println("Tick | PlyYaw  | PlyPit | dYaw   | dPitch | IdlYaw  | IdlPit | ErrYaw | ErrPit | ErrTot | RHYaw   | RHPit  | RHAct | Dist  | TgtVel | TgtY    | TgtHT | PlyHT | LClick | PlyGnd | TgtGnd | PlyMXZ | MSens");
            writer.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            for (String line : recordedTicks) {
                writer.println(line);
            }
            ChatUtils.sendMessageClient("Saved " + recordedTicks.size() + " ticks to: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to save aim recording: " + e.getMessage());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || !isRecording) return;

        // find nearest combat target within 8 blocks
        EntityLivingBase target = PlayerUtils.getTarget(8, false);

        // only record when a target exists
        if (target == null) return;

        tickCounter++;

        // --- player camera state ---
        float playerYaw = mc.thePlayer.rotationYaw;
        float playerPitch = mc.thePlayer.rotationPitch;

        // how much the camera actually moved this tick (includes both mouse + aim assist contribution)
        float deltaYaw = playerYaw - prevPlayerYaw;
        float deltaPitch = playerPitch - prevPlayerPitch;

        // --- ideal rotation to target body center (chest height = 65% of eye height) ---
        float partialTicks = mc.timer.renderPartialTicks;
        double interpX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double interpY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
        double interpZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;
        double chestY = interpY + target.getEyeHeight() * 0.65;

        float[] idealRots = RotationUtils.getRotations(interpX, chestY, interpZ);
        float idealYaw = idealRots[0];
        float idealPitch = idealRots[1];

        // --- error: how far off the aim is from ideal ---
        float errYaw = RotationUtils.getAngleDifference(idealYaw, playerYaw);
        float errPitch = idealPitch - playerPitch;
        float errTotal = (float) Math.hypot(errYaw, errPitch);

        // --- rotationhandler internal state (what the spoofing system thinks) ---
        float rhYaw = RotationHandler.currentRotation != null && RotationHandler.currentRotation.length >= 2
                ? RotationHandler.currentRotation[0] : 0;
        float rhPitch = RotationHandler.currentRotation != null && RotationHandler.currentRotation.length >= 2
                ? RotationHandler.currentRotation[1] : 0;
        boolean rhActive = RotationHandler.enabled && RotationHandler.targetRotation != null;

        // --- target state ---
        double distance = PlayerUtils.getDistanceToEntityBox(target);
        double targetVelX = target.posX - prevTargetPosX;
        double targetVelZ = target.posZ - prevTargetPosZ;
        double targetVel = Math.hypot(targetVelX, targetVelZ);
        double targetY = target.posY;
        int targetHurtTime = target.hurtTime;
        int playerHurtTime = mc.thePlayer.hurtTime;

        // --- input state ---
        boolean leftClick = Mouse.isButtonDown(0);
        boolean playerOnGround = mc.thePlayer.onGround;
        boolean targetOnGround = target.onGround;
        double playerMotionXZ = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);

        // --- mouse sensitivity (raw setting value 0-1) ---
        float mouseSens = mc.gameSettings.mouseSensitivity;

        String entry = String.format(
            "%-4d | %-7.1f | %-6.1f | %-6.2f | %-6.2f | %-7.1f | %-6.1f | %-6.2f | %-6.2f | %-6.2f | %-7.1f | %-6.1f | %-5b | %-5.2f | %-6.3f | %-7.2f | %-5d | %-5d | %-6b | %-6b | %-6b | %-6.3f | %-5.2f",
            tickCounter,
            playerYaw, playerPitch,
            deltaYaw, deltaPitch,
            idealYaw, idealPitch,
            errYaw, errPitch, errTotal,
            rhYaw, rhPitch, rhActive,
            distance, targetVel, targetY,
            targetHurtTime, playerHurtTime,
            leftClick, playerOnGround, targetOnGround,
            playerMotionXZ, mouseSens
        );

        recordedTicks.add(entry);

        // store for next tick's delta calculations
        prevPlayerYaw = playerYaw;
        prevPlayerPitch = playerPitch;
        prevTargetPosX = (float) target.posX;
        prevTargetPosZ = (float) target.posZ;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!isRecording || mc.thePlayer == null) return;

        EntityLivingBase target = PlayerUtils.getTarget(8, false);
        ScaledResolution sr = new ScaledResolution(mc);

        if (target != null) {
            float[] idealRots = RotationUtils.getRotations(target.posX, target.posY + target.getEyeHeight() * 0.65, target.posZ);
            float errYaw = RotationUtils.getAngleDifference(idealRots[0], mc.thePlayer.rotationYaw);
            float errPitch = idealRots[1] - mc.thePlayer.rotationPitch;
            float errTotal = (float) Math.hypot(errYaw, errPitch);

            String infoText = String.format("AIM REC - Ticks: %d | ErrYaw: %.1f | ErrPit: %.1f | Total: %.1f | Dist: %.2f | RH: %b",
                    tickCounter, errYaw, errPitch, errTotal, PlayerUtils.getDistanceToEntityBox(target), RotationHandler.enabled);

            Fonts.interMedium.get(16).drawStringWithShadow(infoText, 10, sr.getScaledHeight() - 55, Color.CYAN.getRGB());
        } else {
            String infoText = String.format("AIM REC - Ticks: %d | No target", tickCounter);
            Fonts.interMedium.get(16).drawStringWithShadow(infoText, 10, sr.getScaledHeight() - 55, Color.GRAY.getRGB());
        }
    }
}
