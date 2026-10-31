package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.SmoothStepAnimation;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class ModuleComponent implements IComponent {
    private Module module;
    private ModuleCategory category;
    private float x, y, width = 190.0f, height = 30.0f;
    private boolean isHovered;
    public boolean visible = true;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    // smooth animation for the master toggle switch
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(180, 1);

    // modern card color palette matching fdp design specification
    private static final Color COLOR_CARD_BG = new Color(13, 17, 24, 255);
    private static final Color COLOR_CARD_BORDER = new Color(255, 255, 255, 12);
    private static final Color COLOR_CARD_BORDER_HOVER = new Color(255, 255, 255, 22);
    private static final Color COLOR_TRACK_OFF = new Color(24, 30, 42, 255);
    private static final Color COLOR_TRACK_ON = new Color(0, 132, 255, 255);
    private static final Color COLOR_KNOB = new Color(245, 248, 255, 255);
    private static final Color COLOR_TITLE = new Color(245, 248, 255, 255);

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
    }

    public ModuleComponent(Module module) {
        this(module, ModuleCategory.Misc);
    }

    public void initCategory() {
    }

    // calculates full card height including header, child settings, and bottom padding
    public float computeCardHeight() {
        float h = 26.0f; // header strip
        for (Component setting : settings) {
            if (setting.isVisible()) {
                h += setting.getHeight();
            }
        }
        return h + 4.0f;
    }

    public void render(boolean shader) {
        if (!visible) return;

        this.height = computeCardHeight();
        this.toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);

        // draw outer card container with subtle rounded corners and hairline border
        RoundedUtils.drawRoundOutline(x, y, width, height, 5.0f, 0.5f, COLOR_CARD_BG, isHovered ? COLOR_CARD_BORDER_HOVER : COLOR_CARD_BORDER);

        // module title in bold typography
        Fonts.interSemiBold.get(10.5f).drawString(module.getName(), x + 8.0f, y + 8.0f, COLOR_TITLE.getRGB());

        // master toggle switch on top right of the card
        float switchW = 20.0f;
        float switchH = 10.0f;
        float switchX = x + width - switchW - 8.0f;
        float switchY = y + 7.5f;

        float anim = (float) toggleAnimation.getOutput();
        Color trackBg = ColorUtils.interpolateColorC(COLOR_TRACK_OFF, COLOR_TRACK_ON, anim);

        // draw switch track
        RoundedUtils.drawRound(switchX, switchY, switchW, switchH, 5.0f, trackBg);

        // draw switch knob
        float knobDiameter = 7.0f;
        float knobX = switchX + 1.5f + (switchW - knobDiameter - 3.0f) * anim;
        float knobY = switchY + (switchH - knobDiameter) / 2.0f;
        RoundedUtils.drawRound(knobX, knobY, knobDiameter, knobDiameter, knobDiameter / 2.0f, COLOR_KNOB);

        // render nested child settings directly inside the card body
        float settingY = y + 25.0f;
        for (Component setting : settings) {
            if (setting.isVisible()) {
                setting.setX(x + 2.0f);
                setting.setY(settingY);
                setting.setWidth(width - 4.0f);
                setting.drawScreen(0, 0); // coordinates handled by mouse utils
                settingY += setting.getHeight();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);

        // propagate mouse state to all child settings
        float settingY = y + 25.0f;
        for (Component setting : settings) {
            if (setting.isVisible()) {
                setting.setX(x + 2.0f);
                setting.setY(settingY);
                setting.setWidth(width - 4.0f);
                setting.drawScreen(mouseX, mouseY);
                settingY += setting.getHeight();
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible) return;

        // check master switch click zone
        float switchW = 24.0f;
        float switchH = 14.0f;
        float switchX = x + width - switchW - 6.0f;
        float switchY = y + 5.5f;

        boolean switchClicked = MouseUtils.isHovered(switchX, switchY, switchW, switchH, mouseX, mouseY);
        boolean headerClicked = MouseUtils.isHovered(x, y, width, 24.0f, mouseX, mouseY);

        if (mouseButton == 0 && (switchClicked || headerClicked)) {
            module.toggle();
            SoundUtil.playSound("demise.tick");
            return;
        }

        // forward clicks to active child setting components
        for (Component setting : settings) {
            if (setting.isVisible() && MouseUtils.isHovered(setting.getX(), setting.getY(), setting.getWidth(), setting.getHeight(), mouseX, mouseY)) {
                setting.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (Component setting : settings) {
            if (setting.isVisible()) {
                setting.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        for (Component setting : settings) {
            if (setting.isVisible()) {
                setting.keyTyped(typedChar, keyCode);
            }
        }
    }
}
