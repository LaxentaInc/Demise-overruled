package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "Freelook", description = "Look around without turning your body", key = Keyboard.KEY_LMENU)
public class Freelook extends Module {
    public float cameraYaw;
    public float cameraPitch;
    private int previousPerspective = 0;
    private boolean active = false;

    public Freelook() {
    }

    @Override
    public void toggle() {
        // freelook is an always-enabled utility mod that uses hold-to-look; keypresses are handled via physical key polling
    }

    @Override
    public void setEnabled(boolean enabled) {
        // keep module registered and active in the background
        super.setEnabled(true);
    }

    public boolean isActive() {
        updateActiveState();
        return active;
    }

    private void updateActiveState() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
            if (active) {
                stopLooking();
            }
            return;
        }

        // prevent camera decoupling when user is interacting with an open gui screen
        if (mc.currentScreen != null) {
            if (active) {
                stopLooking();
            }
            return;
        }

        int bind = getKeyBind();
        // lunar client freelook only activates when a physical key is assigned
        if (bind == 0) {
            if (active) {
                stopLooking();
            }
            return;
        }

        boolean holding = Keyboard.isKeyDown(bind);
        if (holding && !active) {
            startLooking();
        } else if (!holding && active) {
            stopLooking();
        }
    }

    private void startLooking() {
        active = true;
        if (mc.thePlayer != null) {
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            previousPerspective = mc.gameSettings.thirdPersonView;
            // switch to third-person perspective to view surrounding area
            mc.gameSettings.thirdPersonView = 1;
        }
    }

    private void stopLooking() {
        active = false;
        if (mc.thePlayer != null && mc.gameSettings != null) {
            // snap camera back to the original player perspective
            mc.gameSettings.thirdPersonView = previousPerspective;
            if (mc.renderGlobal != null) {
                mc.renderGlobal.setDisplayListEntitiesDirty();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        updateActiveState();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        updateActiveState();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent event) {
        if (active) {
            stopLooking();
        }
    }

    @Override
    public void onDisable() {
        if (active) {
            stopLooking();
        }
    }
}
