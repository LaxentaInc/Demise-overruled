package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Freelook", description = "Look around without turning your body", key = Keyboard.KEY_LMENU)
public class Freelook extends Module {
    // optional hold mode: when active, freelook only stays on while key is held down
    private final BoolValue hold = new BoolValue("Hold to look", false, this);

    public float cameraYaw;
    public float cameraPitch;
    private int previousPerspective;
    private boolean pressedKey;

    @Override
    public void onEnable() {
        pressedKey = false;
        if (mc.thePlayer != null) {
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            previousPerspective = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
        }
    }

    @Override
    public void onDisable() {
        pressedKey = false;
        if (mc.thePlayer != null && mc.gameSettings != null) {
            mc.gameSettings.thirdPersonView = previousPerspective;
            if (mc.renderGlobal != null) {
                mc.renderGlobal.setDisplayListEntitiesDirty();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        checkKeyRelease();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        checkKeyRelease();
    }

    private void checkKeyRelease() {
        if (mc.thePlayer == null) return;

        // if hold mode is disabled, keep freelook active until manually toggled off via gui or key
        if (!hold.get()) {
            return;
        }

        // prevent auto-disabling when inside gui menus like clickgui or chat
        if (mc.currentScreen != null) {
            return;
        }

        int bind = getKeyBind();
        if (bind == 0) return;

        // track physical key state to only release when user actively let go of the bound key
        if (Keyboard.isKeyDown(bind)) {
            pressedKey = true;
        } else if (pressedKey) {
            pressedKey = false;
            setEnabled(false);
        }
    }
}


