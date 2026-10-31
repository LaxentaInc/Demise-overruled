package wtf.demise.gui.click.components.impl;

import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MultiBooleanComponent extends Component {
    private final MultiBoolValue setting;
    private final Map<BoolValue, EaseOutSine> select = new HashMap<>();

    public MultiBooleanComponent(MultiBoolValue setting) {
        this.setting = setting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float heightoff = 4.0f;
        float lineHeight = Fonts.urbanist.get(14.0f).getHeight() + 3.0f;

        // crisp urbanist font matching client watermark
        Fonts.urbanist.get(15.0f).drawString(setting.getName(), getX() + 6.0f, getY() + 3.0f, new Color(245, 248, 255, 255).getRGB());

        for (BoolValue boolValue : setting.getValues()) {
            select.putIfAbsent(boolValue, new EaseOutSine(100, 1));
            select.get(boolValue).setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            int itemColor = ColorUtils.interpolateColor2(new Color(148, 163, 184, 255), Color.white, (float) select.get(boolValue).getOutput());
            Fonts.urbanist.get(14.0f).drawString(boolValue.getName(), getX() + 10.0f, getY() + Fonts.urbanist.get(15.0f).getHeight() + 2.0f + heightoff, itemColor);

            heightoff += lineHeight;
        }

        setHeight(Fonts.urbanist.get(15.0f).getHeight() + 6.0f + heightoff);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float heightoff = 4.0f;
        float lineHeight = Fonts.urbanist.get(14.0f).getHeight() + 3.0f;

        for (BoolValue boolValue : setting.getValues()) {
            if (MouseUtils.isHovered(getX() + 10.0f, getY() + Fonts.urbanist.get(15.0f).getHeight() + 2.0f + heightoff, Fonts.urbanist.get(14.0f).getStringWidth(boolValue.getName()), Fonts.urbanist.get(14.0f).getHeight(), mouseX, mouseY) && mouse == 0) {
                boolValue.set(!boolValue.get());
                SoundUtil.playSound("demise.tick");
            }
            heightoff += lineHeight;
        }
        super.mouseClicked(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.canDisplay();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
