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
            // crisp dark rectangular card background
            Color cardBg = enabled ?
                    (isHovered ? new Color(28, 36, 52, 230) : new Color(22, 28, 42, 220)) :
                    (isHovered ? new Color(28, 30, 36, 230) : new Color(18, 20, 24, 200));

            Color borderCol = enabled ?
                    new Color(65, 125, 240, 110) :
                    (isHovered ? new Color(255, 255, 255, 25) : new Color(255, 255, 255, 10));

            RoundedUtils.drawRoundOutline(x, y, width, height, 4.0f, 0.5f, cardBg, borderCol);

            // subtle accent indicator bar on left when enabled
            if (enabled) {
                RoundedUtils.drawRound(x + 1.0f, y + 4.0f, 2.5f, height - 8.0f, 1.0f, new Color(65, 125, 240, 255));
            }

            // crisp module name typography
            int nameCol = enabled ? Color.white.getRGB() : (isHovered ? new Color(210, 215, 225).getRGB() : new Color(160, 165, 175).getRGB());
            float textX = x + (enabled ? 9.0f : 8.0f);
            float textY = y + (height - Fonts.interMedium.get(11).getHeight()) / 2.0f;
            Fonts.interMedium.get(11).drawString(module.getName(), textX, textY, nameCol);

            // right side toggle indicator and settings dots
            float rightPadding = 8.0f;
            if (!settings.isEmpty()) {
                // options button
                float dotsX = x + width - 18.0f;
                float dotsY = y + (height - Fonts.interBold.get(11).getHeight()) / 2.0f - 1.0f;
                int dotsCol = optionsHovered ? Color.white.getRGB() : new Color(110, 115, 130).getRGB();
                Fonts.interBold.get(11).drawString("•••", dotsX, dotsY, dotsCol);
                rightPadding = 24.0f;
            }

            // toggle badge / indicator
            float switchW = 20.0f;
            float switchH = 10.0f;
            float switchX = x + width - rightPadding - switchW;
            float switchY = y + (height - switchH) / 2.0f;

            Color switchBg = enabled ? new Color(65, 125, 240, 255) : new Color(38, 41, 50, 220);
            RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 5.0f, switchBg);
            float knobX = enabled ? (switchX + switchW - 8.0f) : (switchX + 2.0f);
            RoundedUtils.drawRound(knobX, switchY + 2.0f, 6.0f, 6.0f, 3.0f, Color.white);
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 4.0f, Color.black);
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