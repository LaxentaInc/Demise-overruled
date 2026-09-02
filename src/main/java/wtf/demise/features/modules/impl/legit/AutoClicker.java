package wtf.demise.features.modules.impl.legit;

import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.PlayerTickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.clicking.ClickHandler;

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

    private long nextRightDelay = 50;

    private boolean isRightReady() {
        return rightTimer.hasTimeElapsed(nextRightDelay);
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (left.get()) {
            if (breakBlocks.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
                return;

            if (mc.gameSettings.keyBindAttack.isKeyDown()) {
                // clicking using keybind, don't need raytrace etc. target is mc.thePlayer so you can always attack.
                ClickHandler.initHandler(lminCPS.get(), lmaxCPS.get(), false, false, false, false, false, false, 3, 3, ClickHandler.ClickMode.Legit, mc.thePlayer);
            }
        }
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent e) {
        if (e.state == PlayerTickEvent.State.PRE) {
            if (right.get()) {
                if (mc.gameSettings.keyBindUseItem.isKeyDown() && isRightReady()) {
                    mc.rightClickMouse();
                    rightTimer.reset();
                    
                    long minCps = (long) rminCPS.get();
                    long maxCps = (long) rmaxCPS.get();
                    if (minCps > maxCps) minCps = maxCps;
                    
                    long baseDelay = 1000L / ThreadLocalRandom.current().nextLong(minCps, maxCps + 1);
                    long jitter = ThreadLocalRandom.current().nextLong(-11, 12);
                    nextRightDelay = Math.max(10, baseDelay + jitter);
                }
            }
        }
    }
}
