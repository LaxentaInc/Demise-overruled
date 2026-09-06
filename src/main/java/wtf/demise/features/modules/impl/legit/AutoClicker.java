package wtf.demise.features.modules.impl.legit;

import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render3DEvent;
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

    @Override
    public void onEnable() {
        leftTimer.reset();
        rightTimer.reset();
    }

    private long nextLeftDelay = 50;
    private long nextRightDelay = 50;

    private boolean isLeftReady() {
        return leftTimer.hasTimeElapsed(nextLeftDelay);
    }

    private boolean isRightReady() {
        return rightTimer.hasTimeElapsed(nextRightDelay);
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        // guard clause preventing click simulation when the game is paused, inside gui screens, or ungrabbed
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
            return;
        }

        if (left.get()) {
            if (mc.gameSettings.keyBindAttack.isKeyDown()) {
                // verify whether crosshair is directly aimed at a block to protect ongoing block breaking from click resetting
                boolean pointingAtBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

                if (!(breakBlocks.get() && pointingAtBlock)) {
                    if (isLeftReady()) {
                        // clear vanilla minecraft 10-tick air swing lockout before invoking clickmouse to bypass check at minecraft line 1130
                        mc.leftClickCounter = 0;
                        mc.clickMouse();
                        // zero out counter immediately after invocation to strip the 10-tick penalty assigned on raytrace miss at minecraft line 1156
                        mc.leftClickCounter = 0;
                        leftTimer.reset();

                        long minCps = (long) lminCPS.get();
                        long maxCps = (long) lmaxCPS.get();
                        if (minCps > maxCps) minCps = maxCps;

                        // calculate target delay between simulated packets based on configured range and randomized deviation
                        long baseDelay = 1000L / ThreadLocalRandom.current().nextLong(minCps, maxCps + 1);
                        long jitter = ThreadLocalRandom.current().nextLong(-11, 12);
                        nextLeftDelay = Math.max(10, baseDelay + jitter);
                    }
                }
            }
        }

        if (right.get()) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown() && isRightReady()) {
                // zero out hardcoded 4-tick right click cooldown in minecraft to permit uninterrupted rapid placement
                mc.rightClickDelayTimer = 0;
                mc.rightClickMouse();
                // re-zero timer after rightclickmouse sets rightclickdelaytimer back to 4 at line 1166
                mc.rightClickDelayTimer = 0;
                rightTimer.reset();

                long minCps = (long) rminCPS.get();
                long maxCps = (long) rmaxCPS.get();
                if (minCps > maxCps) minCps = maxCps;

                // compute next right-click timestamp delay with randomized jitter distribution
                long baseDelay = 1000L / ThreadLocalRandom.current().nextLong(minCps, maxCps + 1);
                long jitter = ThreadLocalRandom.current().nextLong(-11, 12);
                nextRightDelay = Math.max(10, baseDelay + jitter);
            }
        }
    }
}
