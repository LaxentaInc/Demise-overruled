package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Category implements IComponent {
    private ModuleCategory category;
    private float x, y;
    private boolean isHovered, isSelected = true;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;

    public Category(ModuleCategory category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.isSelected = true;
        this.isHovered = false;
        this.interpolatedX = x;

        for (Module module : Demise.INSTANCE.getModuleManager().getModulesByCategory(category)) {
            moduleComponents.add(new ModuleComponent(module, category));
        }
    }

    public void initCategory() {
        moduleComponents.forEach(ModuleComponent::initCategory);
    }

    public void render(boolean shader) {
        drawScreen(0, 0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        handleScroll();

        // strict container dimensions bounded inside the dark window frame
        float sidebarWidth = 118.0f;
        float startX = PanelGui.posX + sidebarWidth + 10.0f;
        float componentStartY = PanelGui.posY + 34.0f;
        float contentWidth = Math.max(120.0f, PanelGui.width - sidebarWidth - 20.0f);
        float viewHeight = Math.max(50.0f, PanelGui.height - 42.0f);

        // responsive masonry: 2 columns on wide windows, 1 column on compact screens
        int cols = contentWidth >= 260.0f ? 2 : 1;
        float colGap = 8.0f;
        float rowGap = 8.0f;
        float cardWidth = cols == 2 ? (contentWidth - colGap) / 2.0f : contentWidth;

        float leftColY = componentStartY;
        float rightColY = componentStartY;

        // calculate total masonry height for strict scroll clamping
        for (ModuleComponent module : moduleComponents) {
            float cardH = module.computeCardHeight();
            if (cols == 1 || leftColY <= rightColY) {
                leftColY += cardH + rowGap;
            } else {
                rightColY += cardH + rowGap;
            }
        }

        float totalContentHeight = Math.max(leftColY, rightColY) - componentStartY;
        maxScroll = Math.max(0.0f, totalContentHeight - viewHeight);
        targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0.0f, maxScroll);
        scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

        // scissor test clamped strictly to the content viewport
        RenderUtils.scissor(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        leftColY = componentStartY;
        rightColY = componentStartY;

        for (ModuleComponent module : moduleComponents) {
            float cardH = module.computeCardHeight();
            float cardX;
            float cardY;

            if (cols == 1 || leftColY <= rightColY) {
                cardX = startX;
                cardY = leftColY - scrollOffset;
                leftColY += cardH + rowGap;
            } else {
                cardX = startX + cardWidth + colGap;
                cardY = rightColY - scrollOffset;
                rightColY += cardH + rowGap;
            }

         
            module.setX(cardX);
            module.setY(cardY);
            module.setWidth(cardWidth);
            module.setHeight(cardH);

            // only draw cards that intersect the visible viewport
            boolean isCardVisible = (cardY + cardH >= componentStartY && cardY <= componentStartY + viewHeight);
            module.setVisible(isCardVisible);

            if (isCardVisible) {
                module.drawCard(mouseX, mouseY, false);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float sidebarWidth = 120.0f;
        float startX = PanelGui.posX + sidebarWidth + 10.0f;
        float componentStartY = PanelGui.posY + 36.0f;
        float contentWidth = Math.max(120.0f, PanelGui.width - sidebarWidth - 20.0f);
        float viewHeight = Math.max(50.0f, PanelGui.height - 44.0f);

        // deliver mouse clicks strictly inside visible container
        if (MouseUtils.isHovered(startX, componentStartY, contentWidth, viewHeight, mouseX, mouseY)) {
            moduleComponents.forEach(moduleComponent -> {
                if (moduleComponent.isVisible()) {
                    moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
                }
            });
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -28.0f : 28.0f;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0.0f, maxScroll);
        }
    }
}
