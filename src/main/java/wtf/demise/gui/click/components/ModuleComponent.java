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
    private float x, y, width = 140.0f, height = 38.0f;
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
            // sleek dark container with rounded geometry and dynamic glint border
            Color cardBg = enabled ?
                    (isHovered ? new Color(22, 27, 36, 250) : new Color(17, 21, 29, 245)) :
                    (isHovered ? new Color(18, 20, 26, 245) : new Color(13, 15, 19, 240));

            Color borderCol = enabled ?
                    new Color(90, 140, 245, 80) :
                    (isHovered ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 14));

            RoundedUtils.drawRoundOutline(x, y, width, height, 4.0f, enabled ? 0.75f : 0.5f, cardBg, borderCol);

            // subtle illuminated vertical indicator along the left edge when module is active
            if (enabled) {
                RoundedUtils.drawRound(x + 2.0f, y + 8.0f, 2.0f, height - 16.0f, 1.0f, new Color(85, 145, 255, 220));
            }

            // primary module title typography
            int nameCol = enabled ? Color.white.getRGB() : (isHovered ? new Color(220, 225, 235).getRGB() : new Color(160, 165, 175).getRGB());
            float textX = x + 10.0f;
            float textY = y + 7.0f;
            Fonts.interMedium.get(10.5f).drawString(module.getName(), textX, textY, nameCol);

            // inline keybind capsule indicator if a trigger key is configured
            if (module.getKeyBind() != 0) {
                String keyName = org.lwjglx.input.Keyboard.getKeyName(module.getKeyBind());
                if (keyName != null && !keyName.isEmpty()) {
                    float nameW = Fonts.interMedium.get(10.5f).getStringWidth(module.getName());
                    Fonts.outfitThin.get(8.5f).drawString("[" + keyName + "]", textX + nameW + 5.0f, textY + 1.0f, new Color(125, 135, 155, 190).getRGB());
                }
            }

            // secondary module description line styled in thin outfit typography
            String desc = module.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = "No description available.";
            }
            float maxDescW = Math.max(20.0f, width - 56.0f);
            String displayDesc = Fonts.outfitThin.get(9).trimStringToWidth(desc, maxDescW);
            int descCol = enabled ? new Color(145, 165, 195, 220).getRGB() : new Color(105, 110, 125, 200).getRGB();
            Fonts.outfitThin.get(9).drawString(displayDesc, textX, y + 22.0f, descCol);

            // options settings drawer trigger button positioned on the right
            float dotsX = x + width - 15.0f;
            float dotsY = y + (height - Fonts.interBold.get(10).getHeight()) / 2.0f - 1.0f;
            if (optionsHovered) {
                RoundedUtils.drawRound(x + width - 21.0f, y + (height - 18.0f) / 2.0f, 18.0f, 18.0f, 3.0f, new Color(255, 255, 255, 25));
            }
            int dotsCol = optionsHovered ? Color.white.getRGB() : new Color(110, 115, 130).getRGB();
            Fonts.interBold.get(10).drawString("•••", dotsX - 1.0f, dotsY, dotsCol);

            // hardware toggle switch indicator
            float switchW = 18.0f;
            float switchH = 9.5f;
            float switchX = x + width - 42.0f;
            float switchY = y + (height - switchH) / 2.0f;

            Color switchTrack = enabled ? new Color(60, 130, 245, 240) : new Color(28, 31, 38, 240);
            Color switchKnob = enabled ? Color.white : new Color(110, 115, 128, 240);

            RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 2.0f, switchTrack);
            float knobX = enabled ? (switchX + switchW - 7.5f) : (switchX + 1.5f);
            RoundedUtils.drawRound(knobX, switchY + 1.5f, 6.0f, 6.5f, 1.5f, switchKnob);
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 4.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        this.optionsHovered = MouseUtils.isHovered(x + width - 22.0f, y, 22.0f, height, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // compute hit test directly with incoming event coordinates to support touchscreen taps
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        if (visible && hovered) {
            boolean optHovered = MouseUtils.isHovered(x + width - 22.0f, y, 22.0f, height, mouseX, mouseY);
            // clicking the options dots or right clicking opens module settings drawer for any module
            if (optHovered || mouseButton == 1) {
                PanelGui.focusedModule = this;
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