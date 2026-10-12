package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Freelook", description = "Hold key to look around without turning your body", key = Keyboard.KEY_LMENU)
public class Freelook extends Module {
    // optional toggle mode, off by default to maintain standard client hold-to-look behavior
    private final BoolValue toggle = new BoolValue("Toggle", false, this);

    public float cameraYaw;
    public float cameraPitch;
    private int previousPerspective;

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            previousPerspective = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
        }
    }

    @Override
    public void onDisable() {
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

        // if user enabled toggle mode, keep active until manually toggled off
        if (toggle.get()) {
            return;
        }

        // prevent auto-disabling when inside gui menus like clickgui or chat
        if (mc.currentScreen != null) {
            return;
        }

        // default hold mode: the moment the bound key is released in-game, disable immediately
        if (getKeyBind() != 0 && !Keyboard.isKeyDown(getKeyBind())) {
            setEnabled(false);
        }
    }
}


