package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
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
public class SearchCategory implements IComponent {
    private ModuleCategory category;
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> allModules = new ArrayList<>();
    private final List<ModuleComponent> filteredModules = new ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String filter = "";
    private boolean inputting;

    public SearchCategory() {
        this.isSelected = false;
    }

    public List<ModuleComponent> getModuleComponents() {
        return filteredModules;
    }

    public void initCategory() {
        // initialize cached module component list once to avoid allocating new components on every keystroke
        if (allModules.isEmpty()) {
            for (Module module : Demise.INSTANCE.getModuleManager().getModules()) {
                allModules.add(new ModuleComponent(module));
            }
        }
        allModules.forEach(ModuleComponent::initCategory);
        filterModules();
    }

    public void clearFilter() {
        this.filter = "";
        filterModules();
    }

    public void filterModules() {
        filteredModules.clear();
        String query = filter.trim().toLowerCase();

        for (ModuleComponent comp : allModules) {
            if (query.isEmpty()) {
                filteredModules.add(comp);
            } else {
                String name = comp.getModule().getName().toLowerCase();
                String desc = comp.getModule().getDescription() != null ? comp.getModule().getDescription().toLowerCase() : "";
                if (name.contains(query) || desc.contains(query)) {
                    filteredModules.add(comp);
                }
            }
        }

        // reset scroll offset so matching modules are immediately visible and not scrolled out of bounds
        targetScrollOffset = 0.0f;
        scrollOffset = 0.0f;
    }

    public void render(boolean shader) {
        if (isSelected) {
            handleScroll();

            float startX = PanelGui.posX + 136.0f;
            float componentStartY = PanelGui.posY + 74.0f;
            float contentWidth = Math.max(130.0f, PanelGui.width - 144.0f);
            float viewHeight = Math.max(80.0f, PanelGui.height - 82.0f);

            int cols = contentWidth > 320.0f ? 2 : 1;
            float colGap = 6.0f;
            float rowGap = 5.0f;
            float cardWidth = (contentWidth - (cols - 1) * colGap) / cols;
            // card height expanded to comfortably display both module title and description
            float cardHeight = 38.0f;

            int totalRows = (filteredModules.size() + cols - 1) / cols;
            float totalHeight = totalRows * cardHeight + Math.max(0, totalRows - 1) * rowGap;

            maxScroll = Math.max(0, totalHeight - viewHeight);
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0.0f, maxScroll);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

            if (filteredModules.isEmpty()) {
                String emptyMsg = filter.isEmpty() ? "No modules available." : "No modules found matching \"" + filter + "\"";
                Fonts.interRegular.get(10).drawString(emptyMsg, startX + 6.0f, componentStartY + 14.0f, new Color(130, 135, 145, 200).getRGB());
                return;
            }

            RenderUtils.scissor(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY + 2.0f;
            for (int i = 0; i < filteredModules.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                float cardX = startX + col * (cardWidth + colGap);
                float cardY = componentOffsetY + row * (cardHeight + rowGap) - scrollOffset;

                ModuleComponent module = filteredModules.get(i);
                module.setX(cardX);
                module.setY(cardY);
                module.setWidth(cardWidth);
                module.setHeight(cardHeight);
                module.render(shader);
                module.setVisible(cardY + cardHeight >= componentStartY && cardY <= componentStartY + viewHeight);
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        filteredModules.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float startX = PanelGui.posX + 136.0f;
        float componentStartY = PanelGui.posY + 74.0f;
        float contentWidth = Math.max(130.0f, PanelGui.width - 144.0f);
        float viewHeight = Math.max(80.0f, PanelGui.height - 82.0f);
        // ensure clicks only register if within the module viewport area
        if (wtf.demise.utils.render.MouseUtils.isHovered(startX - 2.0f, componentStartY, contentWidth + 4.0f, viewHeight, mouseX, mouseY)) {
            filteredModules.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (inputting) {
            String lastFilter = filter;

            if (keyCode == Keyboard.KEY_BACK) {
                deleteLastCharacter();
            } else if (keyCode == Keyboard.KEY_DELETE) {
                clearFilter();
                return;
            } else if (GuiScreen.isCtrlKeyDown()) {
                if (keyCode == Keyboard.KEY_V) {
                    String clipboard = GuiScreen.getClipboardString();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        for (char c : clipboard.toCharArray()) {
                            if (c >= 32 && c != 127) {
                                filter += c;
                            }
                        }
                    }
                } else if (keyCode == Keyboard.KEY_A) {
                    clearFilter();
                    return;
                }
            } else if (typedChar >= 32 && typedChar != 127) {
                filter += typedChar;
            }

            if (!lastFilter.equals(filter)) {
                filterModules();
            }
        }

        filteredModules.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    private void deleteLastCharacter() {
        if (!filter.isEmpty()) {
            filter = filter.substring(0, filter.length() - 1);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        filteredModules.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25.0f : 25.0f;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0.0f, maxScroll);
        }
    }
}