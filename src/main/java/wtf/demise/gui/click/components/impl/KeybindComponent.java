package wtf.demise.gui.click.components.impl;

import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class KeybindComponent extends Component {
    private final Module module;
    private boolean binding;

    // modern dark glass styling palette matching client clickgui standard
    private static final Color COLOR_TEXT = new Color(245, 248, 255, 255);
    private static final Color COLOR_BADGE_BG = new Color(28, 36, 50, 255);
    private static final Color COLOR_BADGE_BG_ACTIVE = new Color(0, 132, 255, 55);
    private static final Color COLOR_BADGE_BORDER = new Color(45, 58, 80, 200);
    private static final Color COLOR_BADGE_BORDER_ACTIVE = new Color(56, 189, 248, 230);
    private static final Color COLOR_TEXT_MUTED = new Color(140, 155, 175, 255);
    private static final Color COLOR_TEXT_ACTIVE = new Color(56, 189, 248, 255);

    public KeybindComponent(Module module) {
        this.module = module;
        // standard row height providing consistent vertical rhythm across module cards
        setHeight(22.0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        // crisp readable urbanist typography for setting title
        Fonts.urbanist.get(15.0f).drawString("Keybind", getX() + 6.0f, getY() + 5.0f, COLOR_TEXT.getRGB());

        String keyText;
        if (binding) {
            keyText = "...";
        } else if (module.getKeyBind() == 0) {
            keyText = "None";
        } else {
            keyText = Keyboard.getKeyName(module.getKeyBind());
            if (keyText == null || keyText.isEmpty()) {
                keyText = "Key " + module.getKeyBind();
            }
        }

        float textWidth = Fonts.urbanist.get(13.5f).getStringWidth(keyText);
        float badgeW = Math.max(30.0f, textWidth + 12.0f);
        float badgeH = 14.0f;
        float badgeX = getX() + getWidth() - badgeW - 6.0f;
        float badgeY = getY() + (getHeight() - badgeH) / 2.0f;

        Color badgeBg = binding ? COLOR_BADGE_BG_ACTIVE : COLOR_BADGE_BG;
        Color badgeBorder = binding ? COLOR_BADGE_BORDER_ACTIVE : COLOR_BADGE_BORDER;
        Color textCol = binding ? COLOR_TEXT_ACTIVE : (module.getKeyBind() == 0 ? COLOR_TEXT_MUTED : Color.white);

        // draw interactive key badge
        RoundedUtils.drawRoundOutline(badgeX, badgeY, badgeW, badgeH, 4.0f, 0.8f, badgeBg, badgeBorder);
        Fonts.urbanist.get(13.5f).drawCenteredString(keyText, badgeX + badgeW / 2.0f, badgeY + (badgeH - Fonts.urbanist.get(13.5f).getHeight()) / 2.0f + 0.5f, textCol.getRGB());

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY)) {
            if (mouseButton == 0) {
                // toggle listening state for user key press
                binding = !binding;
                SoundUtil.playSound("demise.tick");
            } else if (mouseButton == 1) {
                // right-click instantly clears the assigned keybind
                module.setKeyBind(0);
                binding = false;
                SoundUtil.playSound("demise.tick");
            }
        } else if (binding) {
            // clicking elsewhere on the screen cancels keybind capture mode
            binding = false;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                // unbind key when escape backspace or delete is pressed
                module.setKeyBind(0);
            } else if (keyCode != 0) {
                // assign pressed key code to module
                module.setKeyBind(keyCode);
            }
            binding = false;
            SoundUtil.playSound("demise.tick");
        }
        super.keyTyped(typedChar, keyCode);
    }
}

