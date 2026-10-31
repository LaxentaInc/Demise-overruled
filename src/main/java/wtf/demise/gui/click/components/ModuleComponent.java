package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Freelook;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.click.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.SmoothStepAnimation;
import wtf.demise.utils.misc.SoundUtil;
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
    private float x, y, width = 180.0f, height = 26.0f;
    private boolean isHovered;
    public boolean visible = true;
    private boolean expanded = false;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    // smooth animation for the master toggle switch
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(180, 1);

    // visible card color palette with distinct surface and border contrast
    private static final Color COLOR_CARD_BG = new Color(19, 25, 36, 255);
    private static final Color COLOR_CARD_HEADER = new Color(24, 32, 46, 255);
    private static final Color COLOR_CARD_BORDER = new Color(42, 54, 76, 255);
    private static final Color COLOR_CARD_BORDER_HOVER = new Color(60, 78, 110, 255);
    private static final Color COLOR_TRACK_OFF = new Color(30, 38, 52, 255);
    private static final Color COLOR_TRACK_ON = new Color(0, 132, 255, 255);
    private static final Color COLOR_KNOB = new Color(250, 252, 255, 255);
    private static final Color COLOR_TITLE = new Color(255, 255, 255, 255);

    public ModuleComponent(Module module, ModuleCategory category) {
        this.category = category;
        this.module = module;
        this.toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);

        for (Value value : module.getValues()) {
            if (value instanceof BoolValue boolValue) {
                settings.add(new BooleanComponent(boolValue));
            } else if (value instanceof SliderValue sliderValue) {
                settings.add(new SliderComponent(sliderValue));
            } else if (value instanceof ModeValue modeValue) {
                settings.add(new ModeComponent(modeValue));
            } else if (value instanceof ColorValue colorValue) {
                settings.add(new ColorPickerComponent(colorValue));
            } else if (value instanceof MultiBoolValue multiBoolValue) {
                settings.add(new MultiBooleanComponent(multiBoolValue));
            } else if (value instanceof TextValue textValue) {
                settings.add(new StringComponent(textValue));
            }
        }

        // append interactive keybind component to module card settings
        settings.add(new KeybindComponent(module));
    }

    public ModuleComponent(Module module) {
        this(module, ModuleCategory.Misc);
    }

    public void initCategory() {
    }

    // calculates total card height from header strip plus settings if expanded
    public float computeCardHeight() {
        if (!expanded || settings.isEmpty()) {
            return 26.0f;
        }

        float h = 26.0f;
        for (Component setting : settings) {
            if (setting.isVisible()) {
                h += setting.getHeight();
            }
        }
        return h + 4.0f;
    }

    public void render(boolean shader) {
        drawCard(0, 0, shader);
    }

    public void drawCard(int mouseX, int mouseY, boolean shader) {
        if (!visible) return;

        this.height = computeCardHeight();
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        this.toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);

        // draw card body container with clear background and crisp visible border
        RoundedUtils.drawRoundOutline(x, y, width, height, 5.0f, 0.8f, COLOR_CARD_BG, isHovered ? COLOR_CARD_BORDER_HOVER : COLOR_CARD_BORDER);

        // draw header bar
        RoundedUtils.drawRound(x + 1.0f, y + 1.0f, width - 2.0f, 24.0f, 4.0f, COLOR_CARD_HEADER);
        if (expanded && !settings.isEmpty()) {
            RenderUtils.drawRect(x + 1.0f, y + 25.0f, width - 2.0f, 1.0f, COLOR_CARD_BORDER.getRGB());
        }

        // module title in crisp urbanist typography matching client watermark
        Fonts.urbanist.get(17.0f).drawString(module.getName(), x + 8.0f, y + 6.5f, COLOR_TITLE.getRGB());

        // expand collapse indicator if module has configurable settings
        if (!settings.isEmpty()) {
            float chevronX = x + Fonts.urbanist.get(17.0f).getStringWidth(module.getName()) + 10.0f;
            Fonts.urbanist.get(14.0f).drawString(expanded ? "v" : ">", chevronX, y + 7.0f, new Color(140, 155, 175, 255).getRGB());
        }

        if (module instanceof Freelook) {
            // render a locked hold badge instead of a toggle switch for lunar-style hold utility
            float badgeW = 34.0f;
            float badgeH = 12.0f;
            float badgeX = x + width - badgeW - 8.0f;
            float badgeY = y + 7.0f;
            RoundedUtils.drawRound(badgeX, badgeY, badgeW, badgeH, 3.0f, new Color(25, 45, 65, 180));
            Fonts.urbanist.get(11.0f).drawString("HOLD", badgeX + 5.0f, badgeY + 2.5f, new Color(120, 200, 255, 230).getRGB());
        } else {
            // master toggle switch on top right of the card header
            float switchW = 20.0f;
            float switchH = 11.0f;
            float switchX = x + width - switchW - 8.0f;
            float switchY = y + 7.5f;

            float anim = (float) toggleAnimation.getOutput();
            Color trackBg = ColorUtils.interpolateColorC(COLOR_TRACK_OFF, COLOR_TRACK_ON, anim);

            // draw switch track
            RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 5.5f, trackBg);

            // draw switch knob
            float knobDiameter = 8.0f;
            float knobX = switchX + 1.5f + (switchW - knobDiameter - 3.0f) * anim;
            float knobY = switchY + (switchH - knobDiameter) / 2.0f;
            RoundedUtils.drawRound(knobX, knobY, knobDiameter, knobDiameter, knobDiameter / 2.0f, COLOR_KNOB);
        }

        // only draw settings if card is expanded
        if (expanded && !settings.isEmpty()) {
            float settingY = y + 28.0f;
            for (Component setting : settings) {
                if (setting.isVisible()) {
                    setting.setX(x + 4.0f);
                    setting.setY(settingY);
                    setting.setWidth(width - 8.0f);
                    setting.drawScreen(mouseX, mouseY);
                    settingY += setting.getHeight();
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible) return;

        // master switch zone
        float switchW = 22.0f;
        float switchH = 14.0f;
        float switchX = x + width - switchW - 5.0f;
        float switchY = y + 5.0f;
        boolean switchClicked = MouseUtils.isHovered(switchX, switchY, switchW, switchH, mouseX, mouseY);

        // header click zone
        boolean headerClicked = MouseUtils.isHovered(x, y, width, 23.0f, mouseX, mouseY);

        if (module instanceof Freelook) {
            // freelook is always active; clicking header folds or unfolds settings
            if (headerClicked) {
                expanded = !expanded;
                SoundUtil.playSound("demise.tick");
                return;
            }
        } else {
            if (switchClicked && mouseButton == 0) {
                // click on switch toggles module state
                module.toggle();
                SoundUtil.playSound("demise.tick");
                return;
            }

            if (headerClicked) {
                if (mouseButton == 1 || (mouseButton == 0 && !switchClicked && !settings.isEmpty())) {
                    // right-click anywhere on header or click on title folds/unfolds settings
                    expanded = !expanded;
                    SoundUtil.playSound("demise.tick");
                    return;
                } else if (mouseButton == 0 && settings.isEmpty()) {
                    module.toggle();
                    SoundUtil.playSound("demise.tick");
                    return;
                }
            }
        }

        // forward clicks to child settings only when expanded
        if (expanded) {
            for (Component setting : settings) {
                if (setting.isVisible() && MouseUtils.isHovered(setting.getX(), setting.getY(), setting.getWidth(), setting.getHeight(), mouseX, mouseY)) {
                    setting.mouseClicked(mouseX, mouseY, mouseButton);
                    return;
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (expanded) {
            for (Component setting : settings) {
                if (setting.isVisible()) {
                    setting.mouseReleased(mouseX, mouseY, state);
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Component setting : settings) {
                if (setting.isVisible()) {
                    setting.keyTyped(typedChar, keyCode);
                }
            }
        }
    }
}
