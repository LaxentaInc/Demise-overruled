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
    private static final Color COLOR_TRACK_BG = new Color(24, 32, 46, 255);
    private static final Color COLOR_TRACK_FILL = new Color(0, 132, 255, 255);
    private static final Color COLOR_THUMB = new Color(74, 172, 255, 255);
    private static final Color COLOR_TEXT_LABEL = new Color(245, 248, 255, 255);
    private static final Color COLOR_TEXT_VALUE = new Color(245, 248, 255, 255);

    public SliderComponent(SliderValue setting) {
        this.setting = setting;
        previousSetting = setting.get();
        // generous height providing proper breathing room
        setHeight(23.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        // setting name label on left in crisp urbanist font
        Fonts.urbanist.get(15.0f).drawString(setting.getName(), getX() + 6.0f, getY() + 5.0f, COLOR_TEXT_LABEL.getRGB());

        // numeric value readout on right in crisp urbanist font
        String valStr = (setting.getIncrement() % 1 == 0) ? String.format("%.0f", setting.get()) : String.format("%.1f", setting.get());
        float valWidth = Fonts.urbanist.get(15.0f).getStringWidth(valStr);
        Fonts.urbanist.get(15.0f).drawString(valStr, getX() + getWidth() - valWidth - 6.0f, getY() + 5.0f, COLOR_TEXT_VALUE.getRGB());

        // slider track positioned horizontally between label and value
        float trackX = getX() + 90.0f;
        float trackW = getWidth() - 90.0f - valWidth - 14.0f;
        float trackY = getY() + 10.0f;
        float trackH = 3.0f;

        if (trackW < 20.0f) {
            trackW = 20.0f;
        }

        float range = setting.getMax() - setting.getMin();
        float ratio = range > 0 ? (setting.get() - setting.getMin()) / range : 0;
        float filledW = trackW * Math.max(0.0f, Math.min(1.0f, ratio));

        // dark background rail
        RoundedUtils.drawRound(trackX, trackY, trackW, trackH, 1.5f, COLOR_TRACK_BG);

        // active filled rail
        if (filledW > 0.0f) {
            RoundedUtils.drawRound(trackX, trackY, filledW, trackH, 1.5f, COLOR_TRACK_FILL);
        }

        // circular thumb knob
        float thumbX = trackX + filledW;
        float thumbY = trackY + (trackH / 2.0f);
        RenderUtils.drawCircle(thumbX, thumbY, 0, 360, 3.5f, 0.1f, true, COLOR_THUMB.getRGB());

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
        float trackX = getX() + 85.0f;
        float trackW = getWidth() - 85.0f - valWidth - 8.0f;

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
