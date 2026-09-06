package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "TickBase", description = "Abuses packet choking and latency desync to make you laggy to opponents and burst attacks without freezing client side.")
public class TickBase extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "Blatant"}, "Legit", this);
    private final SliderValue maxHoldMs = new SliderValue("Max hold ms", 200, 50, 500, 10, this);
    private final SliderValue pulseDelay = new SliderValue("Pulse delay ms", 60, 0, 300, 10, this);
    private final SliderValue startRange = new SliderValue("Start range", 5.0f, 3.0f, 8.0f, 0.1f, this);
    private final SliderValue burstRange = new SliderValue("Burst range", 3.2f, 2.0f, 5.0f, 0.1f, this);
    private final SliderValue burstHits = new SliderValue("Burst hits", 2, 1, 4, 1, this);
    private final BoolValue hurtBreak = new BoolValue("Hurt break", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final ModeValue renderMode = new ModeValue("Render mode", new String[]{"FakePlayer", "Box"}, "FakePlayer", this, realPos::get);

    private final TimerUtils chokeTimer = new TimerUtils();
    private final TimerUtils delayTimer = new TimerUtils();
    private EntityPlayer target;
    private boolean choking = false;
    private double serverX, serverY, serverZ;

    @Override
    public void onEnable() {
        // clear any existing buffered packets to ensure clean state initialization
        releasePackets();
        delayTimer.reset();
    }

    @Override
    public void onDisable() {
        // flush queued packets immediately upon disabling to prevent desync or rubberbanding
        releasePackets();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        // flush buffered packets on map transitions or respawns to avoid invalid positions
        releasePackets();
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        // in blatant mode, attacking manually triggers immediate packet release to burst hits
        if (choking && mode.is("Blatant")) {
            releaseAndBurst();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        // acquire nearest target within engagement distance
        target = PlayerUtils.getTarget(startRange.get() + 2.0, teamCheck.get());

        // emergency safety flush if player is invalid, dead, taking damage, or target is gone
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead || target == null) {
            if (choking) {
                releasePackets();
            }
            return;
        }

        if (hurtBreak.get() && mc.thePlayer.hurtTime > 0) {
            if (choking) {
                releasePackets();
            }
            return;
        }

        double distance = PlayerUtils.getDistanceToEntityBox(target);

        // enforce strict maximum choke duration so player is never stationary for more than configured threshold
        if (choking && chokeTimer.hasTimeElapsed((long) maxHoldMs.get())) {
            if (mode.is("Blatant") && distance <= burstRange.get()) {
                releaseAndBurst();
            } else {
                releasePackets();
            }
            return;
        }

        if (mode.is("Legit")) {
            // legit mode: micro-choke packets during active combat to desync hitbox without suspicious freezes
            if (distance <= startRange.get() && distance >= 1.5) {
                if (!choking && delayTimer.hasTimeElapsed((long) pulseDelay.get())) {
                    startChoking();
                } else if (choking && chokeTimer.hasTimeElapsed(Math.min((long) maxHoldMs.get(), 120L))) {
                    releasePackets();
                }
            } else if (choking) {
                releasePackets();
            }
        } else if (mode.is("Blatant")) {
            // blatant mode: choke packets while closing the gap, then burst when entering strike range
            if (distance <= startRange.get() && distance > burstRange.get()) {
                if (!choking && delayTimer.hasTimeElapsed((long) pulseDelay.get())) {
                    startChoking();
                }
            } else if (distance <= burstRange.get()) {
                if (choking) {
                    releaseAndBurst();
                }
            } else if (choking) {
                releasePackets();
            }
        }
    }

    private void startChoking() {
        // capture initial stationary coordinates visible to server and opponents
        serverX = mc.thePlayer.posX;
        serverY = mc.thePlayer.posY;
        serverZ = mc.thePlayer.posZ;
        choking = true;
        BlinkComponent.blinking = true;
        chokeTimer.reset();
    }

    private void releasePackets() {
        // dispatch all accumulated outgoing packets to sync real position with server
        if (choking) {
            BlinkComponent.dispatch(true);
            choking = false;
            delayTimer.reset();
        }
    }

    private void releaseAndBurst() {
        // dispatch all choked packets in one single burst transmission
        releasePackets();

        // simulate rapid successive attacks on the synchronized target to deliver burst damage
        if (target != null && PlayerUtils.getDistanceToEntityBox(target) <= burstRange.get() + 0.5) {
            int hits = (int) burstHits.get();
            for (int i = 0; i < hits; i++) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    mc.leftClickCounter = 0;
                    mc.clickMouse();
                    mc.leftClickCounter = 0;
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        // visual representation of the stationary ghost entity representing your position to enemies
        if (realPos.get() && choking && mc.gameSettings.thirdPersonView != 0) {
            double renderX = this.serverX - mc.getRenderManager().viewerPosX;
            double renderY = this.serverY - mc.getRenderManager().viewerPosY;
            double renderZ = this.serverZ - mc.getRenderManager().viewerPosZ;

            switch (renderMode.get()) {
                case "Box":
                    AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
                    AxisAlignedBB axis = new AxisAlignedBB(
                            box.minX - mc.thePlayer.posX + renderX,
                            box.minY - mc.thePlayer.posY + renderY,
                            box.minZ - mc.thePlayer.posZ + renderZ,
                            box.maxX - mc.thePlayer.posX + renderX,
                            box.maxY - mc.thePlayer.posY + renderY,
                            box.maxZ - mc.thePlayer.posZ + renderZ
                    );
                    RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 150), true).getRGB());
                    break;
                case "FakePlayer":
                    GlStateManager.pushMatrix();
                    GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
                    float lightLevel = mc.theWorld.getLight(new BlockPos(mc.thePlayer.getPositionVector()));
                    GlStateManager.color(lightLevel, lightLevel, lightLevel);
                    mc.getRenderManager().doRenderEntity(mc.thePlayer, renderX, renderY, renderZ, mc.thePlayer.rotationYawHead, e.partialTicks(), true, true);
                    GlStateManager.popAttrib();
                    GlStateManager.popMatrix();
                    break;
            }
        }
    }

    // legacy bridge methods required by minecraft.java hooks to permanently prevent game loop stalls
    public boolean skipTick() {
        // always return false to ensure client ticks run smoothly without any skipped movement
        return false;
    }

    public boolean freezeAnim() {
        // always return false to ensure camera updates and 3d world rendering never freeze
        return false;
    }
}