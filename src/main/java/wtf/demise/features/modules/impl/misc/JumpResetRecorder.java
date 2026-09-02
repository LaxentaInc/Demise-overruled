package wtf.demise.features.modules.impl.misc;

import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.JumpReset;
import wtf.demise.utils.misc.ChatUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import wtf.demise.events.impl.player.AttackEvent;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "JumpResetRecorder", description = "Logs per-tick jump reset telemetry for analyzing knockback reduction.")
public class JumpResetRecorder extends Module {
    private final List<String> recordedTicks = new ArrayList<>();
    private int tickCounter = 0;
    private boolean isRecording = false;
    private Entity lastTarget = null;
    private int ticksSinceLastHit = 999;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        recordedTicks.clear();
        tickCounter = 0;
        isRecording = true;
        lastTarget = null;
        ticksSinceLastHit = 999;
        ChatUtils.sendMessageClient("JumpResetRecorder started. Telemetry active!");
    }

    @Override
    public void onDisable() {
        isRecording = false;
        if (recordedTicks.isEmpty()) return;

        File dir = new File(mc.mcDataDir, "demise/telemetry");
        if (!dir.exists()) dir.mkdirs();

        File saveFile = new File(dir, "jumpreset_log_" + System.currentTimeMillis() + ".csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile))) {
            writer.println("Tick | VelRecv | HurtTime | OnGround | MotionXZ | MotionY | IsJumping | ResetCD | MoveFwd | TgtHitCD | TgtOnGnd | TgtMotY");
            writer.println("-----------------------------------------------------------------------------------------------------------------------------");
            for (String line : recordedTicks) {
                writer.println(line);
            }
            ChatUtils.sendMessageClient("Saved " + recordedTicks.size() + " ticks to: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to save jump reset recording: " + e.getMessage());
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (!isRecording) return;
        lastTarget = e.getTargetEntity();
        ticksSinceLastHit = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || !isRecording) return;
        
        tickCounter++;
        if (lastTarget != null) {
            ticksSinceLastHit++;
            if (ticksSinceLastHit > 40) {
                lastTarget = null;
            }
        }
        
        JumpReset jumpReset = (JumpReset) Demise.INSTANCE.getModuleManager().getModule(JumpReset.class);
        
        boolean velRecv = jumpReset != null && jumpReset.lastVelocityReceived;
        int hurtTime = mc.thePlayer.hurtTime;
        boolean onGround = mc.thePlayer.onGround;
        double motionXZ = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
        double motionY = mc.thePlayer.motionY;
        boolean isJumping = jumpReset != null && jumpReset.jumpedThisTick;
        int resetCD = jumpReset != null ? jumpReset.ticksSinceLastReset : 999;
        float moveFwd = mc.thePlayer.moveForward;
        
        boolean tgtOnGround = lastTarget != null && lastTarget.onGround;
        double tgtMotY = lastTarget != null ? lastTarget.motionY : 0.0;
        
        String entry = String.format(
            "%-4d | %-7b | %-8d | %-8b | %-8.3f | %-7.3f | %-9b | %-7d | %-7.1f | %-8d | %-8b | %-7.3f",
            tickCounter, velRecv, hurtTime, onGround, motionXZ, motionY, isJumping, resetCD, moveFwd, ticksSinceLastHit, tgtOnGround, tgtMotY
        );
        
        recordedTicks.add(entry);
    }
}
