package wtf.demise.gui.click.components.impl;

import org.lwjglx.input.Mouse;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private final SliderValue setting;
    private boolean dragging;
    private float previousSetting;
    private final TimerUtils soundTimer = new TimerUtils();

    public SliderComponent(SliderValue setting) {
        this.setting = setting;
        previousSetting = setting.get();
        setHeight(22.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        // label and formatted value readout
        String valStr = (setting.getIncrement() % 1 == 0) ? String.format("%.0f", setting.get()) : String.valueOf(setting.get());
        Fonts.interMedium.get(11).drawString(setting.getName(), getX() + 4, getY() + 2, new Color(200, 205, 215).getRGB());
        Fonts.interRegular.get(11).drawString(valStr, getX() + getWidth() - 4 - Fonts.interRegular.get(11).getStringWidth(valStr), getY() + 2, new Color(175, 180, 192).getRGB());

        float trackX = getX() + 4;
        float trackY = getY() + 13.0f;
        float trackW = getWidth() - 8;
        float trackH = 3.0f;

        float range = setting.getMax() - setting.getMin();
        float ratio = range > 0 ? (setting.get() - setting.getMin()) / range : 0;
        float filledW = trackW * Math.max(0, Math.min(1, ratio));

        // sharp dark background track
        RoundedUtils.drawRound(trackX, trackY, trackW, trackH, 1.0f, new Color(26, 28, 34, 255));
        // minimalist silver progress bar and knob
        if (filledW > 0) {
            RoundedUtils.drawRound(trackX, trackY, filledW, trackH, 1.0f, new Color(215, 220, 230, 255));
            RenderUtils.drawCircle(trackX + filledW, trackY + 1.5f, 0, 360, 2.5f, 0.1f, true, Color.white.getRGB());
        }

        // safety release if mouse button was released outside the window
        if (!Mouse.isButtonDown(0)) {
            dragging = false;
        }

        if (dragging) {
            double clampedRatio = Math.max(0, Math.min(1, (mouseX - trackX) / (double) trackW));
            double difference = setting.getMax() - setting.getMin();
            double value = setting.getMin() + clampedRatio * difference;

            setting.setValue(BigDecimal.valueOf(incValue(value, setting.getIncrement())).setScale(getDecimalPoints(String.valueOf(setting.getIncrement())), RoundingMode.HALF_UP).floatValue());

            if (previousSetting != setting.get()) {
                if (soundTimer.hasTimeElapsed(25)) {
                    SoundUtil.playSound("demise.tick");
                    soundTimer.reset();
                }
                previousSetting = setting.get();
            }
        }
    }

    public static double incValue(double value, double increment) {
        return Math.round(value / increment) * increment;
    }

    public static Integer getDecimalPoints(String n) {
        if (n.contains(".")) {
            return n.replaceAll(".*\\.(?=\\d?)", "").length();
        }
        return 0;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // full component height hitbox for responsive clicking and dragging
        if (mouseButton == 0 && MouseUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY)) {
            dragging = true;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return this.setting.canDisplay();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
