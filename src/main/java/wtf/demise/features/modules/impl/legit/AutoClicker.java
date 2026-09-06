package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "AutoClicker", description = "Automatically clicks.")
public class AutoClicker extends Module {
    private final BoolValue left = new BoolValue("Left click", true, this);
    private final SliderValue lminCPS = new SliderValue("CPS (left min)", 16, 1, 30, this, left::get);
    private final SliderValue lmaxCPS = new SliderValue("CPS (left max)", 20, 1, 30, this, left::get);
    private final BoolValue breakBlocks = new BoolValue("Break blocks (left)", true, this, left::get);

    private final BoolValue right = new BoolValue("Right click", true, this);
    private final SliderValue rminCPS = new SliderValue("CPS (right min)", 16, 1, 30, this, right::get);
    private final SliderValue rmaxCPS = new SliderValue("CPS (right max)", 20, 1, 30, this, right::get);

    private final TimerUtils leftTimer = new TimerUtils();
    private final TimerUtils rightTimer = new TimerUtils();

    private long nextLeftDelay = 50;
    private long nextRightDelay = 50;

    @Override
    public void onEnable() {
        leftTimer.reset();
        rightTimer.reset();
    }

    @Override
    public void onDisable() {
        // ensure attack and use keybindings are released cleanly when module is toggled off
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
    }

    private boolean isLeftReady() {
        return leftTimer.hasTimeElapsed(nextLeftDelay);
    }

    private boolean isRightReady() {
        return rightTimer.hasTimeElapsed(nextRightDelay);
    }

    @EventTarget
    public void onGame(GameEvent e) {
        // guard against click simulation while inside inventory menus, chat, or when window lost focus
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
            return;
        }

        if (left.get()) {
            // poll physical left mouse button directly via lwjgl to mirror external clicker behavior
            boolean isPhysicalLeftDown = Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();

            if (isPhysicalLeftDown) {
                // allow native block mining to proceed uninterrupted if breakblocks option is active
                boolean pointingAtBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

                if (!(breakBlocks.get() && pointingAtBlock)) {
                    if (isLeftReady()) {
                        // zero out counter before and after click to bypass native 10-tick air swing lockout
                        mc.leftClickCounter = 0;

                        // pulse keybind to simulate authentic hardware press
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());

                        mc.clickMouse();
                        mc.leftClickCounter = 0;
                        leftTimer.reset();

                        long minCps = (long) lminCPS.get();
                        long maxCps = (long) lmaxCPS.get();
                        if (minCps > maxCps) minCps = maxCps;

                        // compute delay with randomized jitter distribution
                        long baseDelay = 1000L / ThreadLocalRandom.current().nextLong(minCps, maxCps + 1);
                        long jitter = ThreadLocalRandom.current().nextLong(-8, 9);
                        nextLeftDelay = Math.max(10, baseDelay + jitter);
                    }
                }
            } else if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
                // unpress keybind when physical mouse button is released
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            }
        }

        if (right.get()) {
            // poll physical right mouse button directly via lwjgl
            boolean isPhysicalRightDown = Mouse.isButtonDown(1) || mc.gameSettings.keyBindUseItem.isKeyDown();

            if (isPhysicalRightDown && isRightReady()) {
                // clear native 4-tick right click cooldown timer
                mc.rightClickDelayTimer = 0;

                // pulse keybind for right click simulation
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

                mc.rightClickMouse();
                mc.rightClickDelayTimer = 0;
                rightTimer.reset();

                long minCps = (long) rminCPS.get();
                long maxCps = (long) rmaxCPS.get();
                if (minCps > maxCps) minCps = maxCps;

                // compute right-click interval with random jitter
                long baseDelay = 1000L / ThreadLocalRandom.current().nextLong(minCps, maxCps + 1);
                long jitter = ThreadLocalRandom.current().nextLong(-8, 9);
                nextRightDelay = Math.max(10, baseDelay + jitter);
            } else if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                // release right keybind when button is not held
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        }
    }
}
