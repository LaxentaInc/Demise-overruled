package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.components.Category;
import wtf.demise.gui.click.components.ModuleComponent;
import wtf.demise.gui.click.components.SearchCategory;
import wtf.demise.gui.click.components.config.ConfigCategoryComponent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PanelGui extends GuiScreen {
    private final List<Category> categories = new ArrayList<>();
    public static Category selectedCategory;
    public static Category previousCategory;
    public static ConfigCategoryComponent selectedConfigCategory;
    public static SearchCategory selectedSearchCategory;
    public static ModuleComponent focusedModule = null;
    public static boolean bindingListening = false;
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = -1, posY = -1;
    public static float width = 650.0f, height = 420.0f;
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    public static float interpolatedScale;
    private boolean closing;
    private boolean initializedPosition;

    // floating about modal dialog toggle
    public static boolean showAboutModal = false;

    // fdp modern dark palette specification
    private static final Color BG_WINDOW = new Color(11, 14, 20, 255);
    private static final Color BG_SIDEBAR = new Color(9, 12, 17, 255);
    private static final Color BORDER_WINDOW = new Color(255, 255, 255, 12);
    private static final Color ACCENT_BLUE = new Color(0, 132, 255, 255);
    private static final Color ACCENT_CYAN = new Color(33, 150, 243, 255);
    private static final Color PILL_ACTIVE_BG = new Color(16, 42, 77, 240);
    private static final Color PILL_ACTIVE_BORDER = new Color(0, 132, 255, 180);
    private static final Color TEXT_MUTED = new Color(130, 140, 155, 255);
    private static final Color TEXT_SECTION = new Color(85, 95, 110, 255);

    // sidebar navigation item structure linking labels into internal categories
    private static class NavItem {
        final String section;
        final String name;
        final ModuleCategory category;
        final boolean isConfig;

        NavItem(String section, String name, ModuleCategory category, boolean isConfig) {
            this.section = section;
            this.name = name;
            this.category = category;
            this.isConfig = isConfig;
        }
    }

    private final List<NavItem> navItems = new ArrayList<>();

    public PanelGui() {
        for (ModuleCategory category : ModuleCategory.values()) {
            categories.add(new Category(category, 0, 0));
        }

        configCategoryComponent = new ConfigCategoryComponent(0, 0);
        searchCategoryComponent = new SearchCategory();

        // build categorized sidebar navigation hierarchy matching fdp ui
        navItems.add(new NavItem("Combat", "Rage", ModuleCategory.Combat, false));
        navItems.add(new NavItem("Combat", "Legit", ModuleCategory.Legit, false));
        navItems.add(new NavItem("Movement", "Main", ModuleCategory.Movement, false));
        navItems.add(new NavItem("Movement", "Extras", ModuleCategory.Misc, false));
        navItems.add(new NavItem("Player", "Counterattack", ModuleCategory.Player, false));
        navItems.add(new NavItem("Player", "Assist", ModuleCategory.Exploit, false));
        navItems.add(new NavItem("Render", "Self", ModuleCategory.Visual, false));
        navItems.add(new NavItem("Config", "Configs", null, true));

        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }
    }

    private Category getCategoryByModuleCategory(ModuleCategory cat) {
        for (Category c : categories) {
            if (c.getCategory() == cat) {
                return c;
            }
        }
        return categories.get(0);
    }

    @Override
    public void initGui() {
        closing = false;
        interpolatedScale = 1.0f;
        focusedModule = null;
        bindingListening = false;

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        width = Math.max(500.0f, Math.min(760.0f, screenW - 60.0f));
        height = Math.max(340.0f, Math.min(520.0f, screenH - 60.0f));

        if (!initializedPosition || posX < 0 || posY < 0) {
            posX = (screenW - width) / 2.0f;
            posY = (screenH - height) / 2.0f;
            initializedPosition = true;
        } else {
            posX = MathHelper.clamp_float(posX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(posY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        }

        if (selectedConfigCategory != null) {
            selectedConfigCategory.initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (closing) {
            interpolatedScale = MathUtils.interpolate(interpolatedScale, 0.0f, 0.35f);
            if (interpolatedScale < 0.05f) {
                mc.displayGuiScreen(null);
                return;
            }
        } else {
            interpolatedScale = 1.0f;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        if (dragging) {
            posX = MathHelper.clamp_float(mouseX - dragX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(mouseY - dragY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        }

        RenderUtils.scaleStart(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, interpolatedScale);

        // draw background drop shadow for window depth
        RoundedUtils.drawRound(posX - 3.0f, posY - 3.0f, width + 6.0f, height + 6.0f, 10.0f, new Color(0, 0, 0, 90));

        // draw main window body
        RoundedUtils.drawRoundOutline(posX, posY, width, height, 10.0f, 0.5f, BG_WINDOW, BORDER_WINDOW);

        // draw left sidebar background
        float sidebarW = 140.0f;
        RoundedUtils.drawRound(posX, posY, sidebarW, height, 10.0f, BG_SIDEBAR);
        // vertical divider line separating sidebar from main card canvas
        RenderUtils.drawRect(posX + sidebarW, posY, 1.0f, height, new Color(255, 255, 255, 8).getRGB());

        // brand wordmark at top left
        Fonts.interBold.get(15.0f).drawString("DEMISE", posX + 16.0f, posY + 16.0f, Color.white.getRGB());

        // render categorized sidebar items
        float currentItemY = posY + 42.0f;
        String lastSection = "";

        for (NavItem item : navItems) {
            // draw section header when switching categories
            if (!item.section.equals(lastSection)) {
                currentItemY += 5.0f;
                Fonts.interRegular.get(8.5f).drawString(item.section, posX + 16.0f, currentItemY, TEXT_SECTION.getRGB());
                currentItemY += 13.0f;
                lastSection = item.section;
            }

            boolean isSelected;
            if (item.isConfig) {
                isSelected = (selectedConfigCategory != null);
            } else {
                isSelected = (selectedConfigCategory == null && selectedSearchCategory == null && selectedCategory != null && selectedCategory.getCategory() == item.category);
            }

            float itemX = posX + 10.0f;
            float itemW = sidebarW - 20.0f;
            float itemH = 20.0f;
            boolean hovered = MouseUtils.isHovered(itemX, currentItemY, itemW, itemH, mouseX, mouseY);

            if (isSelected) {
                // active navigation pill with rounded border
                RoundedUtils.drawRoundOutline(itemX, currentItemY, itemW, itemH, 4.0f, 0.5f, PILL_ACTIVE_BG, PILL_ACTIVE_BORDER);
                Fonts.interMedium.get(10.0f).drawString(item.name, itemX + 10.0f, currentItemY + 5.0f, Color.white.getRGB());
            } else {
                if (hovered) {
                    RoundedUtils.drawRound(itemX, currentItemY, itemW, itemH, 4.0f, new Color(255, 255, 255, 8));
                }
                int textAlpha = hovered ? 240 : 160;
                Fonts.interRegular.get(10.0f).drawString(item.name, itemX + 10.0f, currentItemY + 5.0f, new Color(210, 220, 235, textAlpha).getRGB());
            }

            currentItemY += 22.0f;
        }

        // bottom profile card at base of sidebar
        float profileY = posY + height - 38.0f;
        float avatarX = posX + 14.0f;
        float avatarY = profileY + 4.0f;

        // circular player avatar
        RenderUtils.drawCircle(avatarX + 10.0f, avatarY + 10.0f, 0, 360, 10.0f, 0.1f, true, new Color(25, 35, 50, 255).getRGB());
        Fonts.interBold.get(9.0f).drawCenteredString("D", avatarX + 10.0f, avatarY + 6.5f, ACCENT_CYAN.getRGB());

        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Hesophere";
        Fonts.interMedium.get(9.5f).drawString(username, avatarX + 26.0f, avatarY + 2.0f, Color.white.getRGB());
        Fonts.interRegular.get(8.0f).drawString("Till: Active", avatarX + 26.0f, avatarY + 12.0f, ACCENT_CYAN.getRGB());

        // ---- top bar of main content area ----
        float topBarY = posY + 12.0f;
        float saveBtnX = posX + sidebarW + 16.0f;
        float saveBtnW = 62.0f;
        float saveBtnH = 20.0f;
        boolean saveHover = MouseUtils.isHovered(saveBtnX, topBarY, saveBtnW, saveBtnH, mouseX, mouseY);

        // rounded save configuration button
        RoundedUtils.drawRoundOutline(saveBtnX, topBarY, saveBtnW, saveBtnH, 4.0f, 0.5f, new Color(15, 20, 28, 240), saveHover ? new Color(0, 132, 255, 140) : new Color(255, 255, 255, 14));
        Fonts.interMedium.get(9.5f).drawString("Save", saveBtnX + 18.0f, topBarY + 5.0f, Color.white.getRGB());

        // right action icons (settings / search)
        float gearX = posX + width - 48.0f;
        float searchIconX = posX + width - 26.0f;
        boolean gearHover = MouseUtils.isHovered(gearX - 4.0f, topBarY - 2.0f, 20.0f, 20.0f, mouseX, mouseY);
        boolean searchHover = MouseUtils.isHovered(searchIconX - 4.0f, topBarY - 2.0f, 20.0f, 20.0f, mouseX, mouseY);

        // gear icon for toggling about dialog
        Fonts.interMedium.get(12.0f).drawString("*", gearX, topBarY + 3.0f, (gearHover || showAboutModal) ? ACCENT_CYAN.getRGB() : TEXT_MUTED.getRGB());

        // search icon
        Fonts.interMedium.get(10.5f).drawString("Q", searchIconX, topBarY + 4.5f, (searchHover || selectedSearchCategory != null) ? ACCENT_CYAN.getRGB() : TEXT_MUTED.getRGB());

        // content viewport rendering
        if (selectedSearchCategory != null) {
            searchCategoryComponent.render(false);
            searchCategoryComponent.drawScreen(mouseX, mouseY);
        } else if (selectedConfigCategory != null) {
            configCategoryComponent.render(false);
            configCategoryComponent.drawScreen(mouseX, mouseY);
        } else if (selectedCategory != null) {
            selectedCategory.render(false);
            selectedCategory.drawScreen(mouseX, mouseY);
        }

        // draw floating about modal dialog if active
        if (showAboutModal) {
            drawAboutModal(mouseX, mouseY);
        }

        RenderUtils.scaleEnd();
    }

    private void drawAboutModal(int mouseX, int mouseY) {
        float modalW = 190.0f;
        float modalH = 190.0f;
        float modalX = posX + width + 10.0f;
        float modalY = posY;

        ScaledResolution sr = new ScaledResolution(mc);
        if (modalX + modalW > sr.getScaledWidth()) {
            modalX = posX + width - modalW - 14.0f;
            modalY = posY + 36.0f;
        }

        // modal background with outline
        RoundedUtils.drawRound(modalX - 2.0f, modalY - 2.0f, modalW + 4.0f, modalH + 4.0f, 8.0f, new Color(0, 0, 0, 100));
        RoundedUtils.drawRoundOutline(modalX, modalY, modalW, modalH, 8.0f, 0.5f, new Color(10, 14, 20, 255), new Color(255, 255, 255, 14));

        // modal header
        Fonts.interRegular.get(9.0f).drawString("* About DEMISE", modalX + 12.0f, modalY + 12.0f, TEXT_MUTED.getRGB());

        // bold title wordmark
        Fonts.interBold.get(16.0f).drawCenteredString("DEMISECLIENT", modalX + (modalW / 2.0f), modalY + 34.0f, Color.white.getRGB());

        // metadata key value list
        float textY = modalY + 68.0f;
        float labelX = modalX + 14.0f;

        Fonts.interRegular.get(9.0f).drawString("Version:", labelX, textY, TEXT_MUTED.getRGB());
        Fonts.interMedium.get(9.0f).drawString("DEV v2.1.0", labelX + 64.0f, textY, ACCENT_CYAN.getRGB());
        textY += 13.0f;

        Fonts.interRegular.get(9.0f).drawString("Build Type:", labelX, textY, TEXT_MUTED.getRGB());
        Fonts.interMedium.get(9.0f).drawString("#0001 - Dev", labelX + 64.0f, textY, ACCENT_CYAN.getRGB());
        textY += 13.0f;

        Fonts.interRegular.get(9.0f).drawString("Build Date:", labelX, textY, TEXT_MUTED.getRGB());
        Fonts.interMedium.get(9.0f).drawString("2026-10-31", labelX + 64.0f, textY, ACCENT_CYAN.getRGB());
        textY += 13.0f;

        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Hesophere";
        Fonts.interRegular.get(9.0f).drawString("Registered to:", labelX, textY, TEXT_MUTED.getRGB());
        Fonts.interMedium.get(9.0f).drawString(username, labelX + 64.0f, textY, ACCENT_CYAN.getRGB());
        textY += 17.0f;

        Fonts.interRegular.get(8.5f).drawCenteredString("demiseclient @ 2026", modalX + (modalW / 2.0f), textY, new Color(90, 100, 115).getRGB());
        textY += 18.0f;

        // style selector with color indicator circles
        Fonts.interRegular.get(9.0f).drawString("Style", labelX, textY + 2.0f, TEXT_MUTED.getRGB());
        RenderUtils.drawCircle(labelX + 44.0f, textY + 6.0f, 0, 360, 4.5f, 0.1f, true, Color.white.getRGB());
        RenderUtils.drawCircle(labelX + 62.0f, textY + 6.0f, 0, 360, 4.5f, 0.1f, false, ACCENT_BLUE.getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float topBarH = 34.0f;

        // check save profile button
        float sidebarW = 140.0f;
        float saveBtnX = posX + sidebarW + 16.0f;
        float saveBtnW = 62.0f;
        float saveBtnH = 20.0f;
        if (mouseButton == 0 && MouseUtils.isHovered(saveBtnX, posY + 12.0f, saveBtnW, saveBtnH, mouseX, mouseY)) {
            Demise.INSTANCE.getConfigManager().saveConfigs();
            return;
        }

        // check gear icon button
        float gearX = posX + width - 48.0f;
        if (mouseButton == 0 && MouseUtils.isHovered(gearX - 4.0f, posY + 10.0f, 20.0f, 20.0f, mouseX, mouseY)) {
            showAboutModal = !showAboutModal;
            return;
        }

        // check search icon button
        float searchIconX = posX + width - 26.0f;
        if (mouseButton == 0 && MouseUtils.isHovered(searchIconX - 4.0f, posY + 10.0f, 20.0f, 20.0f, mouseX, mouseY)) {
            if (selectedSearchCategory == null) {
                previousCategory = selectedCategory;
                searchCategoryComponent.initCategory();
                selectedSearchCategory = searchCategoryComponent;
                selectedCategory = null;
                selectedConfigCategory = null;
                searchCategoryComponent.setInputting(true);
            } else {
                selectedSearchCategory = null;
                searchCategoryComponent.setInputting(false);
                selectedCategory = (previousCategory != null ? previousCategory : categories.get(0));
            }
            return;
        }

        // window dragging from header region
        if (mouseButton == 0 && MouseUtils.isHovered(posX + sidebarW, posY, width - sidebarW, topBarH, mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
            return;
        }

        // sidebar clicks
        if (MouseUtils.isHovered(posX, posY, sidebarW, height, mouseX, mouseY) && mouseButton == 0) {
            float currentItemY = posY + 42.0f;
            String lastSection = "";

            for (NavItem item : navItems) {
                if (!item.section.equals(lastSection)) {
                    currentItemY += 18.0f;
                    lastSection = item.section;
                }

                float itemX = posX + 10.0f;
                float itemW = sidebarW - 20.0f;
                float itemH = 20.0f;

                if (MouseUtils.isHovered(itemX, currentItemY, itemW, itemH, mouseX, mouseY)) {
                    if (item.isConfig) {
                        if (selectedConfigCategory == null) {
                            configCategoryComponent.initCategory();
                        }
                        selectedConfigCategory = configCategoryComponent;
                        selectedCategory = null;
                        selectedSearchCategory = null;
                        return;
                    } else {
                        Category targetCat = getCategoryByModuleCategory(item.category);
                        if (selectedCategory != targetCat) {
                            targetCat.initCategory();
                        }
                        selectedCategory = targetCat;
                        selectedConfigCategory = null;
                        selectedSearchCategory = null;
                        return;
                    }
                }
                currentItemY += 22.0f;
            }
            return;
        }

        // forward to active content category
        if (selectedSearchCategory != null) {
            selectedSearchCategory.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;

        if (selectedSearchCategory != null) {
            selectedSearchCategory.mouseReleased(mouseX, mouseY, state);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.mouseReleased(mouseX, mouseY, state);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RSHIFT || keyCode == Keyboard.KEY_ESCAPE) {
            if (showAboutModal) {
                showAboutModal = false;
                return;
            }
            if (selectedSearchCategory != null) {
                if (!searchCategoryComponent.getFilter().isEmpty()) {
                    searchCategoryComponent.clearFilter();
                    return;
                } else {
                    selectedSearchCategory = null;
                    searchCategoryComponent.setInputting(false);
                    selectedCategory = (previousCategory != null ? previousCategory : categories.get(0));
                    return;
                }
            }
            closing = true;
            return;
        }

        if (closing) return;

        if (selectedSearchCategory != null) {
            selectedSearchCategory.keyTyped(typedChar, keyCode);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.keyTyped(typedChar, keyCode);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
