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
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(175, 1);

    public BooleanComponent(BoolValue setting) {
        this.setting = setting;
        this.toggleAnimation.setDirection(Direction.BACKWARDS);
        setHeight(Fonts.interRegular.get(15).getHeight() + 5);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.toggleAnimation.setDirection(this.setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);

        // setting label with clean typography
        Fonts.interMedium.get(11).drawString(setting.getName(), getX() + 4.0f, getY() + 3.0f, Color.white.getRGB());

        float switchW = 18.0f;
        float switchH = 9.0f;
        float switchX = getX() + getWidth() - switchW - 4.0f;
        float switchY = getY() + (getHeight() - switchH) / 2.0f;

        float anim = (float) toggleAnimation.getOutput();
        Color trackBg = ColorUtils.interpolateColorC(new Color(30, 32, 38, 255), new Color(220, 225, 235, 255), anim);
        Color knobColor = ColorUtils.interpolateColorC(new Color(130, 135, 145, 255), new Color(16, 18, 22, 255), anim);

        RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 2.0f, trackBg);
        float knobX = switchX + 1.5f + 9.5f * anim;
        RoundedUtils.drawRound(knobX, switchY + 1.5f, 5.5f, 6.0f, 1.5f, knobColor);

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // responsive click on the entire row rather than tiny switch
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
