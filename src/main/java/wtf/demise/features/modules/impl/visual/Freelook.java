package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "Freelook", description = "Look around without turning your body", key = Keyboard.KEY_LMENU)
public class Freelook extends Module {
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
        mc.gameSettings.thirdPersonView = previousPerspective;
        mc.renderGlobal.setDisplayListEntitiesDirty();
    }

    @wtf.demise.events.annotations.EventTarget
    public void onTick(wtf.demise.events.impl.misc.TickEvent event) {
        if (getKeyBind() != 0 && !Keyboard.isKeyDown(getKeyBind())) {
            setEnabled(false);
        }
    }
}
