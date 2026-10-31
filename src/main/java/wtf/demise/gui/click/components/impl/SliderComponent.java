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

    // dark theme palette matching modern card reference
    private static final Color COLOR_TRACK_BG = new Color(22, 28, 38, 255);
    private static final Color COLOR_TRACK_FILL = new Color(0, 132, 255, 255);
    private static final Color COLOR_THUMB = new Color(74, 172, 255, 255);
    private static final Color COLOR_TEXT_LABEL = new Color(175, 185, 200, 255);
    private static final Color COLOR_TEXT_VALUE = new Color(210, 220, 235, 255);

    public SliderComponent(SliderValue setting) {
        this.setting = setting;
        previousSetting = setting.get();
        // compact height for nested card layout
        setHeight(20.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        // setting name label on left
        Fonts.interRegular.get(9.5f).drawString(setting.getName(), getX() + 6.0f, getY() + 4.5f, COLOR_TEXT_LABEL.getRGB());

        // numeric value readout on right
        String valStr = (setting.getIncrement() % 1 == 0) ? String.format("%.0f", setting.get()) : String.format("%.1f", setting.get());
        float valWidth = Fonts.interRegular.get(9.0f).getStringWidth(valStr);
        Fonts.interRegular.get(9.0f).drawString(valStr, getX() + getWidth() - valWidth - 6.0f, getY() + 4.5f, COLOR_TEXT_VALUE.getRGB());

        // slider track positioned horizontally between label and value
        float trackX = getX() + 85.0f;
        float trackW = getWidth() - 85.0f - valWidth - 14.0f;
        float trackY = getY() + 8.5f;
        float trackH = 2.5f;

        if (trackW < 20.0f) {
            trackW = 20.0f;
        }

        float range = setting.getMax() - setting.getMin();
        float ratio = range > 0 ? (setting.get() - setting.getMin()) / range : 0;
        float filledW = trackW * Math.max(0.0f, Math.min(1.0f, ratio));

        // subtle dark background rail
        RoundedUtils.drawRound(trackX, trackY, trackW, trackH, 1.25f, COLOR_TRACK_BG);

        // active filled rail
        if (filledW > 0.0f) {
            RoundedUtils.drawRound(trackX, trackY, filledW, trackH, 1.25f, COLOR_TRACK_FILL);
        }

        // smooth circular knob at slider thumb position
        float thumbX = trackX + filledW;
        float thumbY = trackY + (trackH / 2.0f);
        RenderUtils.drawCircle(thumbX, thumbY, 0, 360, 3.0f, 0.1f, true, COLOR_THUMB.getRGB());

        // release drag state when left mouse button is unpressed
        if (!Mouse.isButtonDown(0)) {
            dragging = false;
        }

        if (dragging) {
            double clampedRatio = Math.max(0.0, Math.min(1.0, (mouseX - trackX) / (double) trackW));
            double difference = setting.getMax() - setting.getMin();
            double value = setting.getMin() + clampedRatio * difference;

            setting.setValue(BigDecimal.valueOf(incValue(value, setting.getIncrement()))
                    .setScale(getDecimalPoints(String.valueOf(setting.getIncrement())), RoundingMode.HALF_UP)
                    .floatValue());

            if (previousSetting != setting.get()) {
                if (soundTimer.hasTimeElapsed(30)) {
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
        float valWidth = 24.0f;
        float trackX = getX() + 80.0f;
        float trackW = getWidth() - 80.0f - valWidth - 8.0f;

        // click anywhere along the slider track zone to start dragging
        if (mouseButton == 0 && MouseUtils.isHovered(trackX - 6.0f, getY(), trackW + 12.0f, getHeight(), mouseX, mouseY)) {
            dragging = true;
        }
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
