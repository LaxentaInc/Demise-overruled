package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
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
import wtf.demise.utils.misc.StringUtils;
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
    private float x, y;
    private boolean isHovered, isExpanded, gearHovered;
    private float height;
    private Color interpolatedColor = new Color(24, 26, 30, 200);
    private float toggleProgress = 0.0f;
    public boolean visible;
    private boolean visibleSetting;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(220, 1);
    private float slideProgress = 0.0f;

    public ModuleComponent(Module module) {
        openAnimation.setDirection(Direction.BACKWARDS);
        this.module = module;
        this.height = 38.0f;
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

    public void initCategory() {
        slideProgress = 0.0f;
    }

    public void render(boolean shader) {
        float width = Math.max(250.0f, PanelGui.width - 20.0f);
        slideProgress = MathUtils.interpolate(slideProgress, visibleSetting ? 1.0f : 0.0f, 0.15f);
        float slideOffset = (width / 6.0f) * (1.0f - slideProgress);

        toggleProgress = MathUtils.interpolate(toggleProgress, module.isEnabled() ? 1.0f : 0.0f, 0.2f);

        if (!shader) {
            if (isHovered) {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(32, 35, 42, 230), 0.15f);
            } else {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(24, 26, 32, 190), 0.15f);
            }

            // draw module card body
            RoundedUtils.drawRound(x + slideOffset, y, width, height, 7.0f, interpolatedColor);

            // draw subtle left accent bar when enabled
            if (toggleProgress > 0.05f) {
                int accentRgb = Demise.INSTANCE.getModuleManager().getModule(Interface.class).color();
                Color accentCol = new Color(accentRgb);
                Color barCol = new Color(accentCol.getRed(), accentCol.getGreen(), accentCol.getBlue(), (int) (240 * toggleProgress));
                RoundedUtils.drawRound(x + slideOffset + 2.0f, y + 6.0f, 3.0f, 26.0f, 1.5f, barCol);
            }

            // module name and description
            Fonts.interMedium.get(14).drawString(module.getName(), x + 12.0f + slideOffset, y + 8.0f, Color.white.getRGB());
            String desc = module.getDescription();
            if (desc != null && !desc.isEmpty()) {
                Fonts.interRegular.get(11).drawString(desc, x + 12.0f + slideOffset, y + 22.0f, new Color(150, 155, 165, 200).getRGB());
            }

            // keybind badge
            float rightOffset = 12.0f;
            if (module.getKeyBind() != 0) {
                String keyName = StringUtils.upperSnakeCaseToPascal(Keyboard.getKeyName(module.getKeyBind()));
                float keyW = Fonts.interRegular.get(10).getStringWidth(keyName) + 8.0f;
                float keyX = x + width - rightOffset - keyW + slideOffset;
                RoundedUtils.drawRound(keyX, y + 11.0f, keyW, 15.0f, 4.0f, new Color(38, 42, 50, 200));
                Fonts.interRegular.get(10).drawString(keyName, keyX + 4.0f, y + 14.0f, new Color(180, 185, 195, 220).getRGB());
                rightOffset += keyW + 6.0f;
            }

            // lunar style animated toggle switch
            float switchW = 26.0f;
            float switchH = 14.0f;
            float switchX = x + width - 36.0f + slideOffset;
            float switchY = y + 12.0f;

            Color offTrack = new Color(48, 52, 60, 220);
            int accentRgb = Demise.INSTANCE.getModuleManager().getModule(Interface.class).color();
            Color onTrack = new Color(accentRgb);
            Color trackCol = ColorUtils.interpolateColorC(offTrack, onTrack, toggleProgress);

            RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 7.0f, trackCol);

            // sliding white knob
            float knobX = switchX + 1.5f + (12.0f * toggleProgress);
            float knobY = switchY + 1.5f;
            RoundedUtils.drawRound(knobX, knobY, 11.0f, 11.0f, 5.5f, Color.white);

            // settings gear button if module contains configurable values
            if (!settings.isEmpty()) {
                float gearX = switchX - 20.0f;
                float gearY = y + 11.0f;

                if (isExpanded) {
                    RoundedUtils.drawRound(gearX, gearY, 16.0f, 16.0f, 4.0f, new Color(255, 255, 255, 30));
                    Fonts.interRegular.get(12).drawString("⚙", gearX + 3.0f, gearY + 4.0f, onTrack.getRGB());
                } else if (gearHovered) {
                    RoundedUtils.drawRound(gearX, gearY, 16.0f, 16.0f, 4.0f, new Color(255, 255, 255, 15));
                    Fonts.interRegular.get(12).drawString("⚙", gearX + 3.0f, gearY + 4.0f, Color.white.getRGB());
                } else {
                    Fonts.interRegular.get(12).drawString("⚙", gearX + 3.0f, gearY + 4.0f, new Color(140, 145, 155, 180).getRGB());
                }
            }
        } else {
            RoundedUtils.drawShaderRound(x + slideOffset, y, width, height, 7.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float width = Math.max(250.0f, PanelGui.width - 20.0f);
        this.isHovered = MouseUtils.isHovered(x, y, width, 38.0f, mouseX, mouseY);

        float switchX = x + width - 36.0f;
        float gearX = switchX - 20.0f;
        this.gearHovered = !settings.isEmpty() && MouseUtils.isHovered(gearX, y + 10.0f, 18.0f, 18.0f, mouseX, mouseY);

        float yOffset = 38.0f;
        openAnimation.setDirection(isExpanded ? Direction.FORWARDS : Direction.BACKWARDS);

        float viewStartY = PanelGui.posY + 44.0f;
        float viewHeight = Math.max(100.0f, PanelGui.height - 70.0f);

        RenderUtils.scissor(x, viewStartY, width, viewHeight, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float slideOffset = (width / 6.0f) * (1.0f - slideProgress);

        for (Component component : settings) {
            if (!component.isVisible()) continue;

            component.setX((component.isChild() ? x + 8.0f : x + 4.0f) + slideOffset);
            component.setY((float) (y + yOffset * openAnimation.getOutput()) + 1.0f);
            component.setWidth(component.isChild() ? width - 16.0f : width - 8.0f);

            if (openAnimation.getOutput() > 0.7f) {
                component.drawScreen(mouseX, mouseY);

                if (component.isChild()) {
                    RenderUtils.drawRect(x + 5.0f + slideOffset, component.getY() - 2.0f, 1.0f, component.getHeight(), new Color(70, 75, 85, 180).getRGB());
                }
            }

            yOffset += (float) (component.getHeight() * openAnimation.getOutput());
            this.height = yOffset;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float width = Math.max(250.0f, PanelGui.width - 20.0f);
        float viewStartY = PanelGui.posY + 44.0f;

        if (isHovered && visible && mouseY >= viewStartY) {
            float switchX = x + width - 36.0f;
            float gearX = switchX - 20.0f;

            // check if clicking settings gear icon
            if (!settings.isEmpty() && MouseUtils.isHovered(gearX, y + 10.0f, 18.0f, 18.0f, mouseX, mouseY)) {
                isExpanded = !isExpanded;
                return;
            }

            // toggle on left click or expand on right click
            switch (mouseButton) {
                case 0 -> module.toggle();
                case 1 -> {
                    if (!settings.isEmpty()) {
                        isExpanded = !isExpanded;
                    }
                }
            }
        } else if (isExpanded) {
            settings.forEach(setting -> setting.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (isExpanded && !isHovered) {
            settings.forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (isExpanded && !isHovered) {
            settings.forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
    }
}