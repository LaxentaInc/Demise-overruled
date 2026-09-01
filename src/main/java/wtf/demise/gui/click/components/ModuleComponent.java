package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.click.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class ModuleComponent implements IComponent {
    private Module module;
    private ModuleCategory category;
    private float x, y, width = 125.0f, height = 115.0f;
    private boolean isHovered, optionsHovered, toggleHovered;
    private Color interpolatedBg = new Color(24, 26, 32, 220);
    private float toggleProgress = 0.0f;
    public boolean visible;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private final EaseInOutQuad settingsAnimation = new EaseInOutQuad(200, 1);

    public ModuleComponent(Module module, ModuleCategory category) {
        this.category = category;
        settingsAnimation.setDirection(Direction.BACKWARDS);
        this.module = module;
        this.toggleProgress = module.isEnabled() ? 1.0f : 0.0f;

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
        toggleProgress = MathUtils.interpolate(toggleProgress, module.isEnabled() ? 1.0f : 0.0f, 0.2f);

        if (!shader) {
            // Glassmorphism background with outline
            if (isHovered) {
                interpolatedBg = ColorUtils.interpolateColorC(interpolatedBg, new Color(30, 30, 30, 180), 0.15f);
            } else {
                interpolatedBg = ColorUtils.interpolateColorC(interpolatedBg, new Color(15, 15, 15, 160), 0.15f);
            }

            RoundedUtils.drawRoundOutline(x, y, width, height, 7.0f, 1.0f, interpolatedBg, new Color(255, 255, 255, 30));

            // Center module name
            String modName = module.getName();
            float nameWidth = Fonts.interRegular.get(15).getStringWidth(modName);
            Fonts.interRegular.get(15).drawString(modName, x + (width - nameWidth) / 2.0f, y + 40.0f, new Color(180, 185, 195, 255).getRGB());

            // row 1: options button
            float optY = y + height - 36.0f;
            float optH = 18.0f;
            Color optBg = optionsHovered ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 20);
            
            RenderUtils.drawRect(x + 1.0f, optY, width - 2.0f, optH, optBg.getRGB());
            RenderUtils.drawRect(x + 1.0f, optY, width - 2.0f, 1.0f, new Color(255, 255, 255, 30).getRGB()); // top separator
            
            String optText = "O P T I O N S";
            float optTextWidth = Fonts.interMedium.get(10).getStringWidth(optText);
            Fonts.interMedium.get(10).drawString(optText, x + (width - optTextWidth) / 2.0f, optY + 5.5f, Color.white.getRGB());

            // row 2: status button
            float statY = y + height - 18.0f;
            float statH = 18.0f;

            Color enabledCol = new Color(50, 185, 100, 255);
            Color disabledCol = new Color(200, 45, 80, 255);
            Color statBg = ColorUtils.interpolateColorC(disabledCol, enabledCol, toggleProgress);

            if (toggleHovered) {
                statBg = module.isEnabled() ? new Color(60, 205, 110, 255) : new Color(220, 55, 90, 255);
            }

            // Draw rounded bottom for the status button, and sharp top using a regular rect overlap
            RoundedUtils.drawRound(x + 1.0f, statY, width - 2.0f, statH - 1.0f, 6.0f, statBg);
            RenderUtils.drawRect(x + 1.0f, statY, width - 2.0f, 6.0f, statBg.getRGB());

            String statusText = module.isEnabled() ? "ENABLED" : "DISABLED";
            float statTextWidth = Fonts.interMedium.get(11).getStringWidth(statusText);
            Fonts.interMedium.get(11).drawString(statusText, x + (width - statTextWidth) / 2.0f, statY + 5.0f, Color.white.getRGB());
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 7.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);

        this.optionsHovered = MouseUtils.isHovered(x + 1.0f, y + height - 36.0f, width - 2.0f, 18.0f, mouseX, mouseY);
        this.toggleHovered = MouseUtils.isHovered(x + 1.0f, y + height - 18.0f, width - 2.0f, 18.0f, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (visible && isHovered) {
            // clicking options button opens settings drawer
            if (MouseUtils.isHovered(x + 1.0f, y + height - 36.0f, width - 2.0f, 18.0f, mouseX, mouseY)) {
                PanelGui.focusedModule = this;
                return;
            }

            // clicking status button or card body toggles module
            if (MouseUtils.isHovered(x + 1.0f, y + height - 18.0f, width - 2.0f, 18.0f, mouseX, mouseY) || mouseButton == 0) {
                module.toggle();
                return;
            }

            // right click opens settings drawer
            if (mouseButton == 1) {
                PanelGui.focusedModule = this;
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