package wtf.demise.gui.click.components.config;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.Demise;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.io.File;
import java.util.Objects;

@Getter
@Setter
public class ConfigComponent implements IComponent {
    private float x, y;
    private boolean isHovered, saveHovered, deleteHovered;
    private Color interpolatedColor = new Color(20, 20, 20, 150);
    private Color interpolatedColor1 = new Color(0, 0, 0, 0);
    public boolean visible;
    private float slideProgress = 0f;
    private String name;

    public ConfigComponent(String name) {
        this.name = name;
    }

    public void initCategory() {
        slideProgress = 0;
    }

    public void render(boolean shader) {
        float width = Math.max(250.0f, PanelGui.width - 20.0f);
        slideProgress = MathUtils.interpolate(slideProgress, visible ? 1.0f : 0.0f, 0.15f);
        float slideOffset = (width / 6.0f) * (1.0f - slideProgress);

        if (!shader) {
            if (isHovered) {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(32, 35, 42, 230), 0.15f);
            } else {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(24, 26, 32, 190), 0.15f);
            }

            boolean isCurrent = Objects.equals(Demise.INSTANCE.getConfigManager().getCurrentConfig(), name);
            if (isCurrent) {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(40, 50, 65, 180), 0.15f);
            } else {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(0, 0, 0, 0), 0.15f);
            }

            RoundedUtils.drawRound(x + slideOffset, y, width, 32.0f, 7.0f, interpolatedColor);
            RoundedUtils.drawRound(x + slideOffset, y, width, 32.0f, 7.0f, interpolatedColor1);

            if (isCurrent) {
                RoundedUtils.drawRound(x + slideOffset + 2.0f, y + 6.0f, 3.0f, 20.0f, 1.5f, new Color(56, 189, 248, 240));
            }

            Fonts.interMedium.get(14).drawString(name, x + 12.0f + slideOffset, y + 10.0f, Color.white.getRGB());

            float saveWidth = Fonts.interRegular.get(12).getStringWidth("Save") + 12.0f;
            float deleteWidth = Fonts.interRegular.get(12).getStringWidth("Delete") + 12.0f;

            float deleteX = x + width - 10.0f - deleteWidth + slideOffset;
            float saveX = deleteX - 6.0f - saveWidth;

            Color saveBg = saveHovered ? new Color(56, 189, 248, 200) : new Color(38, 42, 50, 200);
            Color deleteBg = deleteHovered ? new Color(239, 68, 68, 200) : new Color(38, 42, 50, 200);

            RoundedUtils.drawRound(saveX, y + 7.0f, saveWidth, 18.0f, 4.0f, saveBg);
            Fonts.interRegular.get(12).drawString("Save", saveX + 6.0f, y + 10.0f, Color.white.getRGB());

            RoundedUtils.drawRound(deleteX, y + 7.0f, deleteWidth, 18.0f, 4.0f, deleteBg);
            Fonts.interRegular.get(12).drawString("Delete", deleteX + 6.0f, y + 10.0f, Color.white.getRGB());
        } else {
            RoundedUtils.drawShaderRound(x + slideOffset, y, width, 32.0f, 7.0f, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float width = Math.max(250.0f, PanelGui.width - 20.0f);
        float slideOffset = (width / 6.0f) * (1.0f - slideProgress);
        float saveWidth = Fonts.interRegular.get(12).getStringWidth("Save") + 12.0f;
        float deleteWidth = Fonts.interRegular.get(12).getStringWidth("Delete") + 12.0f;

        float deleteX = x + width - 10.0f - deleteWidth + slideOffset;
        float saveX = deleteX - 6.0f - saveWidth;

        this.isHovered = MouseUtils.isHovered(x + slideOffset, y, saveX - (x + slideOffset), 32.0f, mouseX, mouseY);
        this.saveHovered = MouseUtils.isHovered(saveX, y + 7.0f, saveWidth, 18.0f, mouseX, mouseY);
        this.deleteHovered = MouseUtils.isHovered(deleteX, y + 7.0f, deleteWidth, 18.0f, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (visible && mouseButton == 0) {
            if (isHovered) {
                if (Demise.INSTANCE.getConfigManager().loadConfig(new ModuleConfig(name))) {
                    Demise.INSTANCE.getConfigManager().setCurrentConfig(name);
                    ChatUtils.sendMessageClient("Loaded config " + name);
                } else {
                    ChatUtils.sendMessageClient("Failed to load config " + name + "!");
                }
            } else if (saveHovered) {
                if (Demise.INSTANCE.getConfigManager().saveConfig(new ModuleConfig(name))) {
                    ChatUtils.sendMessageClient("Saved config " + name);
                } else {
                    ChatUtils.sendMessageClient("Failed to save config " + name + "!");
                }
            } else if (deleteHovered) {
                File configFile = new File(Demise.INSTANCE.getMainDir(), name + ".json");
                if (!configFile.exists()) {
                    ChatUtils.sendMessageClient("Config does not exist: " + name);
                    return;
                }

                String message = configFile.delete() ? "Removed config: " + name : "Failed to remove config: " + name;
                ChatUtils.sendMessageClient(message);
            }
        }
    }
}