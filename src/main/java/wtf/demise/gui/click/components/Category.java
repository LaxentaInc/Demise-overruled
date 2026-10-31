package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
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
    private boolean isHovered, isSelected;
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
        this.isSelected = false;
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
        if (isSelected) {
            handleScroll();

            // content area bounds matching new panel geometry
            float sidebarWidth = 140.0f;
            float startX = PanelGui.posX + sidebarWidth + 14.0f;
            float componentStartY = PanelGui.posY + 44.0f;
            float contentWidth = Math.max(160.0f, PanelGui.width - sidebarWidth - 28.0f);
            float viewHeight = Math.max(80.0f, PanelGui.height - 52.0f);

            // 2-column masonry card grid matching fdp design specification
            float colGap = 10.0f;
            float rowGap = 10.0f;
            float cardWidth = (contentWidth - colGap) / 2.0f;

            float leftColY = componentStartY;
            float rightColY = componentStartY;

            // pre-calculate layout heights and scroll bounds
            for (ModuleComponent module : moduleComponents) {
                float cardH = module.computeCardHeight();
                if (leftColY <= rightColY) {
                    leftColY += cardH + rowGap;
                } else {
                    rightColY += cardH + rowGap;
                }
            }

            float totalContentHeight = Math.max(leftColY, rightColY) - componentStartY;
            maxScroll = Math.max(0.0f, totalContentHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

            // scissor viewport to prevent cards from bleeding outside the main canvas
            RenderUtils.scissor(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            // reset column runners for actual drawing pass
            leftColY = componentStartY;
            rightColY = componentStartY;

            for (ModuleComponent module : moduleComponents) {
                float cardH = module.computeCardHeight();
                float cardX;
                float cardY;

                // balance cards between columns dynamically
                if (leftColY <= rightColY) {
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

                // cull off-screen cards for high fps rendering performance
                boolean isCardVisible = (cardY + cardH >= componentStartY && cardY <= componentStartY + viewHeight);
                module.setVisible(isCardVisible);

                if (isCardVisible) {
                    module.render(shader);
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        if (!isSelected) return;
        moduleComponents.forEach(moduleComponent -> {
            if (moduleComponent.isVisible()) {
                moduleComponent.drawScreen(mouseX, mouseY);
            }
        });
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isSelected) return;
        float sidebarWidth = 140.0f;
        float startX = PanelGui.posX + sidebarWidth + 14.0f;
        float componentStartY = PanelGui.posY + 44.0f;
        float contentWidth = Math.max(160.0f, PanelGui.width - sidebarWidth - 28.0f);
        float viewHeight = Math.max(80.0f, PanelGui.height - 52.0f);

        // deliver clicks only if inside content viewport
        if (MouseUtils.isHovered(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, mouseX, mouseY)) {
            moduleComponents.forEach(moduleComponent -> {
                if (moduleComponent.isVisible()) {
                    moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
                }
            });
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!isSelected) return;
        moduleComponents.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (!isSelected) return;
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
