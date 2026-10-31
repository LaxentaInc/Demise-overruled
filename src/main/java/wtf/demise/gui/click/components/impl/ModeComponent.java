package wtf.demise.gui.click.components.impl;

import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class ModeComponent extends Component {
    private final ModeValue setting;

    // modern dark input box palette matching reference gui
    private static final Color COLOR_TEXT_LABEL = new Color(175, 185, 200, 255);
    private static final Color COLOR_TEXT_ACTIVE = new Color(245, 248, 255, 255);
    private static final Color COLOR_BOX_BG = new Color(13, 17, 24, 255);
    private static final Color COLOR_BOX_BORDER = new Color(255, 255, 255, 14);
    private static final Color COLOR_BOX_BORDER_HOVER = new Color(0, 132, 255, 120);

    public ModeComponent(ModeValue setting) {
        this.setting = setting;
        // standard compact row height for card layout
        setHeight(21.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        // setting label on left side
        Fonts.interRegular.get(9.5f).drawString(setting.getName(), getX() + 6.0f, getY() + 5.0f, COLOR_TEXT_LABEL.getRGB());

        // mode combobox pill on right side
        String currentMode = setting.get();
        float textW = Fonts.interMedium.get(9.0f).getStringWidth(currentMode);
        float boxW = Math.max(54.0f, textW + 18.0f);
        float boxH = 15.0f;
        float boxX = getX() + getWidth() - boxW - 6.0f;
        float boxY = getY() + (getHeight() - boxH) / 2.0f;

        boolean hovered = MouseUtils.isHovered(boxX, boxY, boxW, boxH, mouseX, mouseY);

        // rounded container with subtle border
        RoundedUtils.drawRoundOutline(boxX, boxY, boxW, boxH, 3.0f, 0.5f, COLOR_BOX_BG, hovered ? COLOR_BOX_BORDER_HOVER : COLOR_BOX_BORDER);

        // active mode text centered in box
        Fonts.interMedium.get(9.0f).drawString(currentMode, boxX + 6.0f, boxY + 4.0f, COLOR_TEXT_ACTIVE.getRGB());

        // downward chevron glyph on far right of the box
        Fonts.interRegular.get(7.5f).drawString("v", boxX + boxW - 9.0f, boxY + 4.5f, new Color(130, 140, 155, 255).getRGB());

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        String currentMode = setting.get();
        float textW = Fonts.interMedium.get(9.0f).getStringWidth(currentMode);
        float boxW = Math.max(54.0f, textW + 18.0f);
        float boxH = 15.0f;
        float boxX = getX() + getWidth() - boxW - 6.0f;
        float boxY = getY() + (getHeight() - boxH) / 2.0f;

        // click on the combobox cycles to next or previous mode
        if (MouseUtils.isHovered(boxX, boxY, boxW, boxH, mouseX, mouseY)) {
            String[] modes = setting.getModes();
            if (modes != null && modes.length > 0) {
                int currentIndex = setting.getIndex();

                if (mouseButton == 0) {
                    // left click: cycle to next mode in array
                    int nextIndex = (currentIndex + 1) % modes.length;
                    setting.set(nextIndex);
                    SoundUtil.playSound("demise.tick");
                } else if (mouseButton == 1) {
                    // right click: cycle to previous mode in array
                    int prevIndex = (currentIndex - 1 + modes.length) % modes.length;
                    setting.set(prevIndex);
                    SoundUtil.playSound("demise.tick");
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
