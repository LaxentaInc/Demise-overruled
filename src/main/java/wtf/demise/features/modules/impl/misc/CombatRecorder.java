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
import wtf.demise.features.modules.impl.legit.HitSelect;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "CombatRecorder", description = "Logs detailed per-tick combat telemetry for analyzing hit selection quality.")
public class CombatRecorder extends Module {
    private final List<String> recordedTicks = new ArrayList<>();
    private int tickCounter = 0;
    private boolean isRecording = false;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        recordedTicks.clear();
        tickCounter = 0;
        isRecording = true;
        ChatUtils.sendMessageClient("CombatRecorder started. Fight telemetry active!");
    }

    @Override
    public void onDisable() {
        isRecording = false;
        if (recordedTicks.isEmpty()) {
            ChatUtils.sendMessageClient("CombatRecorder stopped. No data collected.");
            return;
        }

        File saveFile = new File(Demise.INSTANCE.getMainDir(), "combat_recording.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile, false))) {
            writer.println("=== COMBAT RECORDER TICK-BY-TICK FIGHT TELEMETRY LOG ===");
            writer.println("Total Ticks Recorded: " + recordedTicks.size());
            writer.println("----------------------------------------------------------------------------------------------------------------------");
            writer.println("Tick | PlyHP  | TgtHP  | PlyHT | TgtHT | Dist   | Block | LClick | PlyMXZ  | TgtMXZ  | PlyMotY | TgtMotY | PlyGnd | TgtGnd | PlyYaw  | TgtYaw ");
            writer.println("----------------------------------------------------------------------------------------------------------------------");
            for (String line : recordedTicks) {
                writer.println(line);
            }
            ChatUtils.sendMessageClient("Saved " + recordedTicks.size() + " ticks to: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to save combat recording: " + e.getMessage());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || !isRecording) return;

        // find nearest combat target within 8 blocks
        EntityLivingBase target = PlayerUtils.getTarget(8, false);

        // only record when a target exists to avoid logging idle ticks
        if (target == null) return;

        tickCounter++;

        float playerHealth = mc.thePlayer.getHealth();
        float targetHealth = target.getHealth();
        int playerHurtTime = mc.thePlayer.hurtTime;
        int targetHurtTime = target.hurtTime;
        double distance = PlayerUtils.getDistanceToEntityBox(target);

        // check if hitselect is currently blocking our clicks
        HitSelect hitSelect = (HitSelect) Demise.INSTANCE.getModuleManager().getModule(HitSelect.class);
        boolean blocking = hitSelect.isEnabled() && hitSelect.blockClicking;

        // check if player is left clicking this tick
        boolean leftClick = Mouse.isButtonDown(0);

        // player horizontal speed
        double playerMotionXZ = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
        // target horizontal speed
        double targetMotionXZ = Math.hypot(target.motionX, target.motionZ);

        double playerMotionY = mc.thePlayer.motionY;
        double targetMotionY = target.motionY;

        boolean playerOnGround = mc.thePlayer.onGround;
        boolean targetOnGround = target.onGround;

        float playerYaw = mc.thePlayer.rotationYaw;
        float targetYaw = target.rotationYaw;

        String entry = String.format(
            "%-4d | %-6.1f | %-6.1f | %-5d | %-5d | %-6.2f | %-5b | %-6b | %-7.3f | %-7.3f | %-7.3f | %-7.3f | %-6b | %-6b | %-7.1f | %-7.1f",
            tickCounter, playerHealth, targetHealth,
            playerHurtTime, targetHurtTime, distance,
            blocking, leftClick,
            playerMotionXZ, targetMotionXZ,
            playerMotionY, targetMotionY,
            playerOnGround, targetOnGround,
            playerYaw, targetYaw
        );

        recordedTicks.add(entry);
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!isRecording || mc.thePlayer == null) return;

        EntityLivingBase target = PlayerUtils.getTarget(8, false);
        ScaledResolution sr = new ScaledResolution(mc);

        String targetInfo = target != null
            ? String.format("HP: %.1f | Dist: %.2f | TgtHP: %.1f | TgtHT: %d",
                mc.thePlayer.getHealth(),
                PlayerUtils.getDistanceToEntityBox(target),
                target.getHealth(),
                target.hurtTime)
            : "No target";

        String infoText = String.format("RECORDING COMBAT - Ticks: %d | %s", tickCounter, targetInfo);

        Fonts.interMedium.get(16).drawStringWithShadow(infoText, 10, sr.getScaledHeight() - 40, Color.RED.getRGB());
    }
}
