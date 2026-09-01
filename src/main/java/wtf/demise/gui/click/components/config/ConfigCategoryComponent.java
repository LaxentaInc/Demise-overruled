package wtf.demise.gui.click.components.config;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConfigCategoryComponent implements IComponent {
    private float x, y;
    private boolean isHovered, isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String name = "Configs";
    private final List<ConfigComponent> configs = new ArrayList<>();

    public ConfigCategoryComponent(float x, float y) {
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;

        for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
            configs.add(new ConfigComponent(config));
        }
    }

    public void initCategory() {
        configs.forEach(ConfigComponent::initCategory);
    }

    public void initGui() {
        configs.clear();
        for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
            configs.add(new ConfigComponent(config));
        }
    }

    public void render(boolean shader) {
        if (isSelected) {
            handleScroll();

            float startX = PanelGui.posX + 140.0f;
            float componentStartY = PanelGui.posY + 74.0f;
            float contentWidth = Math.max(130.0f, PanelGui.width - 150.0f);
            float viewHeight = Math.max(100.0f, PanelGui.height - 84.0f);

            float totalHeight = 0;
            for (ConfigComponent ignored : configs) {
                totalHeight += 38.0f;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.15f);

            RenderUtils.scissor(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY + 2.0f;
            for (ConfigComponent config : configs) {
                float moduleY = componentOffsetY - scrollOffset;
                config.setX(startX);
                config.setY(moduleY);
                config.render(shader);
                config.setVisible(moduleY + 34.0f >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += 36.0f;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        configs.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        configs.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        configs.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        configs.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}