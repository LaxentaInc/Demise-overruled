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
    private float x, y, width = 140.0f, height = 24.0f;
    private boolean isHovered, optionsHovered;
    public boolean visible;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    // smooth interpolation targets for hover and enable transitions
    private float hoverAlpha = 0.0f;
    private float enableAlpha = 0.0f;

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

        // interpolate hover and enable alpha for buttery smooth transitions
        float targetHover = isHovered ? 1.0f : 0.0f;
        hoverAlpha += (targetHover - hoverAlpha) * 0.35f;
        float targetEnable = enabled ? 1.0f : 0.0f;
        enableAlpha += (targetEnable - enableAlpha) * 0.3f;

        if (!shader) {
            // base row background: near-invisible until hovered, then subtle lift
            int hoverBgAlpha = (int) (hoverAlpha * 35);
            if (hoverBgAlpha > 2) {
                RoundedUtils.drawRound(x, y, width, height, 2.0f, new Color(255, 255, 255, hoverBgAlpha));
            }

            // thin left accent bar animates in when module is enabled
            if (enableAlpha > 0.05f) {
                int barAlpha = (int) (enableAlpha * 255);
                // accent color: soft violet that reads as intentional and distinctive
                RoundedUtils.drawRound(x + 1.0f, y + 4.0f, 2.0f, height - 8.0f, 1.0f, new Color(124, 106, 239, barAlpha));
            }

            // module name: white when active, muted gray when off, brightens on hover
            float textX = x + 9.0f;
            float textY = y + (height - Fonts.interMedium.get(10.5f).getHeight()) / 2.0f;

            int r, g, b;
            if (enabled) {
                // enabled: bright white text
                r = 255; g = 255; b = 255;
            } else {
                // disabled: muted gray, brightens on hover
                int base = 145;
                int boost = (int) (hoverAlpha * 65);
                r = base + boost; g = base + boost; b = base + boost + (int) (hoverAlpha * 10);
            }
            Fonts.interMedium.get(10.5f).drawString(module.getName(), textX, textY, new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255)).getRGB());

            // keybind badge on the right side, only when bound
            // position shifts left if dots button is visible to avoid overlap
            float dotsZoneW = 18.0f;
            if (module.getKeyBind() != 0) {
                String keyName = org.lwjglx.input.Keyboard.getKeyName(module.getKeyBind());
                if (keyName != null && !keyName.isEmpty()) {
                    float badgeW = Fonts.interRegular.get(8.5f).getStringWidth(keyName) + 8.0f;
                    float badgeX = x + width - badgeW - dotsZoneW - 4.0f;
                    float badgeY = y + (height - 12.0f) / 2.0f;
                    // subtle dark pill behind the key name
                    RoundedUtils.drawRound(badgeX, badgeY, badgeW, 12.0f, 2.0f, new Color(255, 255, 255, 12));
                    Fonts.interRegular.get(8.5f).drawString(keyName, badgeX + 4.0f, badgeY + 2.0f, new Color(110, 115, 130).getRGB());
                }
            }

            // three-dot options button on the far right, visible on hover for discoverability
            float dotsAlpha = Math.max(hoverAlpha, optionsHovered ? 1.0f : 0.0f);
            if (dotsAlpha > 0.05f) {
                float dotsX = x + width - 14.0f;
                float dotsY = y + (height - Fonts.interMedium.get(10).getHeight()) / 2.0f;
                int dotsCol = optionsHovered
                        ? new Color(255, 255, 255, (int) (dotsAlpha * 255)).getRGB()
                        : new Color(120, 120, 140, (int) (dotsAlpha * 180)).getRGB();
                Fonts.interMedium.get(10).drawString("•••", dotsX - 4.0f, dotsY, dotsCol);
            }
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 2.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        // dots hit area covers the right 18px strip of the row
        this.optionsHovered = MouseUtils.isHovered(x + width - 18.0f, y, 18.0f, height, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // compute hit test directly with incoming event coordinates to support touchscreen taps
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        if (visible && hovered) {
            // clicking the dots zone or right-clicking anywhere opens settings panel
            boolean optHovered = MouseUtils.isHovered(x + width - 18.0f, y, 18.0f, height, mouseX, mouseY);
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