package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
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
    private final SliderValue lminCPS = new SliderValue("CPS (left min)", 9.0f, 1.0f, 20.0f, 0.5f, this, left::get);
    private final SliderValue lmaxCPS = new SliderValue("CPS (left max)", 12.0f, 1.0f, 20.0f, 0.5f, this, left::get);
    private final BoolValue breakBlocks = new BoolValue("Break blocks (left)", true, this, left::get);
    private final SliderValue jitter = new SliderValue("Jitter", 1.8f, 0.0f, 5.0f, 0.1f, this, left::get);
    private final BoolValue abnormalJitter = new BoolValue("Abnormal jitter", true, this, left::get);

    private final BoolValue right = new BoolValue("Right click", true, this);
    private final SliderValue rminCPS = new SliderValue("CPS (right min)", 10.0f, 1.0f, 20.0f, 0.5f, this, right::get);
    private final SliderValue rmaxCPS = new SliderValue("CPS (right max)", 13.0f, 1.0f, 20.0f, 0.5f, this, right::get);

    private final TimerUtils leftTimer = new TimerUtils();
    private final TimerUtils leftHoldTimer = new TimerUtils();
    private final TimerUtils rightTimer = new TimerUtils();
    private final TimerUtils rightHoldTimer = new TimerUtils();

    private long nextLeftDelay = 100;
    private long leftHoldDuration = 45;
    private boolean leftPendingRelease = false;

    private long nextRightDelay = 90;
    private long rightHoldDuration = 45;
    private boolean rightPendingRelease = false;

    private int leftClickCounter = 0;
    private int rightClickCounter = 0;

    @Override
    public void onEnable() {
        leftTimer.reset();
        leftHoldTimer.reset();
        rightTimer.reset();
        rightHoldTimer.reset();
        leftPendingRelease = false;
        rightPendingRelease = false;
        leftClickCounter = 0;
        rightClickCounter = 0;
    }

    @Override
    public void onDisable() {
        // ensure attack and use keybindings are released cleanly when module is toggled off
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
        leftPendingRelease = false;
        rightPendingRelease = false;
    }

    @EventTarget
    public void onGame(GameEvent e) {
        // guard against click simulation while inside inventory menus, chat, or when window lost focus
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
            if (leftPendingRelease && mc.gameSettings != null) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                leftPendingRelease = false;
            }
            if (rightPendingRelease && mc.gameSettings != null) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                rightPendingRelease = false;
            }
            return;
        }

        if (left.get()) {
            // poll physical left mouse button directly via lwjgl to mirror external clicker behavior
            boolean isPhysicalLeftDown = Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();

            if (isPhysicalLeftDown) {
                // allow native block mining to proceed uninterrupted if breakblocks option is active
                boolean pointingAtBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

                if (breakBlocks.get() && pointingAtBlock) {
                    // maintain attack key for normal block destruction without artificial click gaps
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                } else if (!mc.thePlayer.isUsingItem()) {
                    // execute authentic press, hold, and release state lifecycle
                    if (leftPendingRelease) {
                        if (leftHoldTimer.hasTimeElapsed(leftHoldDuration)) {
                            // release button after authentic physical micro-switch hold time
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                            leftPendingRelease = false;
                        }
                    } else if (leftTimer.hasTimeElapsed(nextLeftDelay)) {
                        // zero out counter before and after click to bypass native 10-tick air swing lockout
                        mc.leftClickCounter = 0;

                        // pulse keybind to simulate authentic hardware press
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());

                        mc.clickMouse();
                        mc.leftClickCounter = 0;

                        leftTimer.reset();
                        leftHoldTimer.reset();
                        leftPendingRelease = true;

                        // calculate switch hold duration using gaussian distribution
                        leftHoldDuration = calculateHoldDuration();

                        // calculate next delay with gaussian curve and human fatigue drops
                        nextLeftDelay = calculateNextDelay(lminCPS.get(), lmaxCPS.get(), true);

                        // apply motor coordination cursor jitter during click bursts
                        applyJitter();
                    }
                }
            } else {
                if (leftPendingRelease) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                    leftPendingRelease = false;
                } else if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                }
            }
        }

        if (right.get()) {
            // poll physical right mouse button directly via lwjgl
            boolean isPhysicalRightDown = Mouse.isButtonDown(1) || mc.gameSettings.keyBindUseItem.isKeyDown();

            if (isPhysicalRightDown) {
                if (rightPendingRelease) {
                    if (rightHoldTimer.hasTimeElapsed(rightHoldDuration)) {
                        // release right mouse button after switch hold time
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                        rightPendingRelease = false;
                    }
                } else if (rightTimer.hasTimeElapsed(nextRightDelay)) {
                    // clear native 4-tick right click cooldown timer
                    mc.rightClickDelayTimer = 0;

                    // pulse keybind for right click simulation
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

                    mc.rightClickMouse();
                    mc.rightClickDelayTimer = 0;

                    rightTimer.reset();
                    rightHoldTimer.reset();
                    rightPendingRelease = true;

                    rightHoldDuration = calculateHoldDuration();
                    nextRightDelay = calculateNextDelay(rminCPS.get(), rmaxCPS.get(), false);
                }
            } else {
                if (rightPendingRelease) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    rightPendingRelease = false;
                } else if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                }
            }
        }
    }

    private void applyJitter() {
        float strength = jitter.get();
        if (strength <= 0.0f) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float yawOffset;
        float pitchOffset;

        if (abnormalJitter.get()) {
            // abnormal hand spasm simulation: non-linear asymmetrical muscle twitch
            double angle = random.nextDouble(0, Math.PI * 2.0);
            double magnitude = (random.nextGaussian() * 0.45 + (random.nextBoolean() ? 0.35 : -0.35)) * strength;
            yawOffset = (float) (Math.cos(angle) * magnitude);
            pitchOffset = (float) (Math.sin(angle) * magnitude * 0.65);

            // occasional sudden micro-slip typical in high-tension pvp clicking
            if (random.nextDouble() < 0.15) {
                yawOffset += (random.nextBoolean() ? 0.6f : -0.6f) * strength;
            }
        } else {
            // standard gaussian cursor jitter
            yawOffset = (float) (random.nextGaussian() * 0.35 * strength);
            pitchOffset = (float) (random.nextGaussian() * 0.22 * strength);
        }

        mc.thePlayer.rotationYaw += yawOffset;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchOffset, -90.0f, 90.0f);
    }

    private long calculateHoldDuration() {
        // human physical switch actuation hold duration follows a normal distribution centered at ~45ms
        double gaussian = ThreadLocalRandom.current().nextGaussian();
        long duration = (long) (45.0 + gaussian * 9.0);
        return Math.max(28L, Math.min(65L, duration));
    }

    private long calculateNextDelay(float minCps, float maxCps, boolean isLeft) {
        if (minCps > maxCps) {
            minCps = maxCps;
        }

        double meanCps = (minCps + maxCps) / 2.0;
        double stdDev = Math.max(0.4, (maxCps - minCps) / 3.2);

        // sample target cps from gaussian curve to produce organic human variance with positive skewness
        double targetCps = ThreadLocalRandom.current().nextGaussian() * stdDev + meanCps;
        targetCps = Math.max(1.0, Math.min(25.0, targetCps));

        long baseDelay = (long) (1000.0 / targetCps);

        // simulate authentic finger fatigue micro-pauses and debounce variations
        int count = isLeft ? ++leftClickCounter : ++rightClickCounter;
        if (count % ThreadLocalRandom.current().nextInt(14, 26) == 0) {
            // natural muscle reset pause (drops cps momentarily)
            baseDelay += ThreadLocalRandom.current().nextLong(35, 75);
        } else if (ThreadLocalRandom.current().nextDouble() < 0.10) {
            // slight double-tap acceleration
            baseDelay -= ThreadLocalRandom.current().nextLong(10, 22);
        }

        return Math.max(35L, baseDelay);
    }
}
