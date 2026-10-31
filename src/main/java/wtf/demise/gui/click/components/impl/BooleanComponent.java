package wtf.demise.gui.click.components.impl;

import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.SmoothStepAnimation;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class BooleanComponent extends Component {
    private final BoolValue setting;
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(180, 1);

    // modern high contrast palette with razor-sharp readability
    private static final Color COLOR_TRACK_OFF = new Color(26, 34, 48, 255);
    private static final Color COLOR_TRACK_ON = new Color(0, 132, 255, 255);
    private static final Color COLOR_KNOB = new Color(255, 255, 255, 255);
    private static final Color COLOR_TEXT = new Color(245, 248, 255, 255);

    public BooleanComponent(BoolValue setting) {
        this.setting = setting;
        this.toggleAnimation.setDirection(setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);
        // generous height providing proper breathing room
        setHeight(22.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.toggleAnimation.setDirection(this.setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);

        // crisp readable urbanist typography matching client watermark
        Fonts.urbanist.get(15.0f).drawString(setting.getName(), getX() + 6.0f, getY() + 5.0f, COLOR_TEXT.getRGB());

        float switchW = 18.0f;
        float switchH = 10.0f;
        float switchX = getX() + getWidth() - switchW - 6.0f;
        float switchY = getY() + (getHeight() - switchH) / 2.0f;

        float anim = (float) toggleAnimation.getOutput();
        Color trackBg = ColorUtils.interpolateColorC(COLOR_TRACK_OFF, COLOR_TRACK_ON, anim);

        // rounded pill track
        RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 5.0f, trackBg);

        // sliding circular knob
        float knobDiameter = 7.0f;
        float knobX = switchX + 1.5f + (switchW - knobDiameter - 3.0f) * anim;
        float knobY = switchY + (switchH - knobDiameter) / 2.0f;
        RoundedUtils.drawRound(knobX, knobY, knobDiameter, knobDiameter, knobDiameter / 2.0f, COLOR_KNOB);

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY)) {
            this.setting.set(!this.setting.get());
            SoundUtil.playSound("demise.tick");
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
