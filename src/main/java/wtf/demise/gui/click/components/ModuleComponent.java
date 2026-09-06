package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.click.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class ModuleComponent implements IComponent {
    private Module module;
    private ModuleCategory category;
    private float x, y, width = 140.0f, height = 26.0f;
    private boolean isHovered, optionsHovered;
    public boolean visible;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    public ModuleComponent(Module module, ModuleCategory category) {
        this.category = category;
        this.module = module;

        for (Value value : module.getValues()) {
            if (value instanceof BoolValue boolValue) {
                settings.add(new BooleanComponent(boolValue));
            }
            if (value instanceof ColorValue colorValue) {
                settings.add(new ColorPickerComponent(colorValue));
            }
            if (value instanceof SliderValue sliderValue) {
                settings.add(new SliderComponent(sliderValue));
            }
            if (value instanceof ModeValue modeValue) {
                settings.add(new ModeComponent(modeValue));
            }
            if (value instanceof MultiBoolValue multiBoolValue) {
                settings.add(new MultiBooleanComponent(multiBoolValue));
            }
            if (value instanceof TextValue textValue) {
                settings.add(new StringComponent(textValue));
            }
        }
    }

    public ModuleComponent(Module module) {
        this(module, ModuleCategory.Misc);
    }

    public void initCategory() {
    }

    public void render(boolean shader) {
        boolean enabled = module.isEnabled();
        if (!shader) {
            // sharp minimalist rectangular card background with subtle border
            Color cardBg = enabled ?
                    (isHovered ? new Color(24, 26, 32, 255) : new Color(18, 20, 25, 255)) :
                    (isHovered ? new Color(20, 21, 26, 255) : new Color(14, 15, 18, 255));

            Color borderCol = enabled ?
                    new Color(255, 255, 255, 28) :
                    (isHovered ? new Color(255, 255, 255, 18) : new Color(255, 255, 255, 8));

            RoundedUtils.drawRoundOutline(x, y, width, height, 2.0f, 0.5f, cardBg, borderCol);

            // clean module name typography with uniform margin
            int nameCol = enabled ? Color.white.getRGB() : (isHovered ? new Color(215, 220, 230).getRGB() : new Color(150, 155, 165).getRGB());
            float textX = x + 10.0f;
            float textY = y + (height - Fonts.interMedium.get(11).getHeight()) / 2.0f;
            Fonts.interMedium.get(11).drawString(module.getName(), textX, textY, nameCol);

            // right side toggle indicator and settings dots
            float rightPadding = 8.0f;
            if (!settings.isEmpty()) {
                // options button
                float dotsX = x + width - 18.0f;
                float dotsY = y + (height - Fonts.interBold.get(11).getHeight()) / 2.0f - 1.0f;
                int dotsCol = optionsHovered ? Color.white.getRGB() : new Color(100, 105, 118).getRGB();
                Fonts.interBold.get(11).drawString("•••", dotsX, dotsY, dotsCol);
                rightPadding = 24.0f;
            }

            // minimalist monochrome hardware switch indicator
            float switchW = 18.0f;
            float switchH = 9.0f;
            float switchX = x + width - rightPadding - switchW;
            float switchY = y + (height - switchH) / 2.0f;

            Color switchTrack = enabled ? new Color(220, 225, 235, 255) : new Color(30, 32, 38, 255);
            Color switchKnob = enabled ? new Color(16, 18, 22, 255) : new Color(130, 135, 145, 255);

            RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 2.0f, switchTrack);
            float knobX = enabled ? (switchX + switchW - 7.0f) : (switchX + 1.5f);
            RoundedUtils.drawRound(knobX, switchY + 1.5f, 5.5f, 6.0f, 1.5f, switchKnob);
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 2.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        this.optionsHovered = !settings.isEmpty() && MouseUtils.isHovered(x + width - 24.0f, y, 24.0f, height, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (visible && isHovered) {
            // clicking the options dots or right clicking opens module settings drawer
            if (optionsHovered || mouseButton == 1) {
                if (!settings.isEmpty()) {
                    PanelGui.focusedModule = this;
                }
                return;
            }

            // left click toggles module state
            if (mouseButton == 0) {
                module.toggle();
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}