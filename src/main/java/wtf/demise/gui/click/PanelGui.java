package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.Component;
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
    // tracks whether the user clicked the bind button and the gui is awaiting key input
    public static boolean bindingListening = false;
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = -1, posY = -1;
    public static float width = 580, height = 370;
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    public static float interpolatedScale;
    private boolean closing;
    private boolean initializedPosition;
    private float settingsScroll = 0.0f;
    private float targetSettingsScroll = 0.0f;
    private float maxSettingsScroll = 0.0f;

    // ---- color palette: centralized so changes propagate everywhere ----
    // base surface colors for the main window chrome
    private static final Color BG_MAIN = new Color(14, 14, 18, 255);
    private static final Color BG_SIDEBAR = new Color(14, 14, 18, 255);
    // separator lines drawn at very low alpha to avoid visual noise
    private static final Color SEPARATOR = new Color(255, 255, 255, 8);
    // accent: muted violet -- stands out without screaming
    private static final Color ACCENT = new Color(124, 106, 239, 255);
    private static final Color ACCENT_DIM = new Color(124, 106, 239, 120);

    public PanelGui() {
        for (ModuleCategory category : ModuleCategory.values()) {
            categories.add(new Category(category, 0, 0));
        }

        configCategoryComponent = new ConfigCategoryComponent(0, 0);
        searchCategoryComponent = new SearchCategory();

        if (selectedCategory == null) {
            selectedCategory = categories.get(0);
        }
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

        width = Math.max(300.0f, Math.min(800.0f, screenW - 60.0f));
        height = Math.max(250.0f, Math.min(600.0f, screenH - 60.0f));

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
            interpolatedScale = MathUtils.interpolate(interpolatedScale, 0.0f, 0.4f);
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

        width = Math.max(300.0f, Math.min(800.0f, screenW - 60.0f));
        height = Math.max(250.0f, Math.min(600.0f, screenH - 60.0f));

        if (dragging) {
            posX = MathHelper.clamp_float(mouseX - dragX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(mouseY - dragY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        } else {
            posX = MathHelper.clamp_float(posX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(posY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        }

        RenderUtils.scaleStart(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, interpolatedScale);

        // outer shadow layer for depth separation against game world
        RoundedUtils.drawRound(posX - 2.0f, posY - 2.0f, width + 4.0f, height + 4.0f, 5.0f, new Color(0, 0, 0, 70));
        // main window body -- sharp radius, solid bg, hairline border
        RoundedUtils.drawRoundOutline(posX, posY, width, height, 4.0f, 0.5f, BG_MAIN, new Color(255, 255, 255, 14));

        // ---- header strip (32px tall): brand + tabs + close ----
        float headerH = 32.0f;

        // brand wordmark: lowercase "demise" in medium weight for understated confidence
        Fonts.interMedium.get(13).drawString("demise", posX + 14.0f, posY + 10.0f, new Color(255, 255, 255, 230).getRGB());

        // tab pills centered in the header -- concise labels, no letter-spacing gimmick
        float navCenterX = posX + (width / 2.0f) - 60.0f;
        float navY = posY + 7.0f;

        boolean modsSelected = (selectedConfigCategory == null);
        boolean configsSelected = (selectedConfigCategory != null);

        String modsText = "Mods";
        float modsWidth = Fonts.interMedium.get(10).getStringWidth(modsText) + 16.0f;
        // active tab: accent underline + white text; inactive: ghost text
        Fonts.interMedium.get(10).drawString(modsText, navCenterX + 8.0f, navY + 5.5f, modsSelected ? Color.white.getRGB() : new Color(90, 90, 100).getRGB());
        if (modsSelected) {
            RoundedUtils.drawRound(navCenterX + 8.0f, navY + 17.0f, Fonts.interMedium.get(10).getStringWidth(modsText), 1.5f, 0.5f, ACCENT);
        }

        String cfgText = "Configs";
        float cfgStartX = navCenterX + modsWidth + 6.0f;
        float cfgWidth = Fonts.interMedium.get(10).getStringWidth(cfgText) + 16.0f;
        Fonts.interMedium.get(10).drawString(cfgText, cfgStartX + 8.0f, navY + 5.5f, configsSelected ? Color.white.getRGB() : new Color(90, 90, 100).getRGB());
        if (configsSelected) {
            RoundedUtils.drawRound(cfgStartX + 8.0f, navY + 17.0f, Fonts.interMedium.get(10).getStringWidth(cfgText), 1.5f, 0.5f, ACCENT);
        }

        // close button: minimal x glyph with subtle hover feedback
        float closeBtnX = posX + width - 26.0f;
        float closeBtnY = posY + 8.0f;
        boolean closeHovered = MouseUtils.isHovered(closeBtnX, closeBtnY, 18.0f, 18.0f, mouseX, mouseY);
        Fonts.interMedium.get(11).drawString("✕", closeBtnX + 4.0f, closeBtnY + 3.0f, closeHovered ? new Color(240, 80, 80).getRGB() : new Color(80, 80, 90).getRGB());

        // header bottom separator
        RenderUtils.drawRect(posX + 1.0f, posY + headerH, width - 2.0f, 1.0f, SEPARATOR.getRGB());

        // ---- sidebar (110px wide): category list + bottom actions ----
        float sidebarW = 110.0f;
        float sidebarY = posY + headerH + 1.0f;
        float sidebarH = height - headerH - 1.0f;

        // vertical separator between sidebar and content
        RenderUtils.drawRect(posX + sidebarW, sidebarY, 1.0f, sidebarH, SEPARATOR.getRGB());

        float itemY = sidebarY + 8.0f;
        for (Category cat : categories) {
            boolean isActive = (selectedCategory == cat && selectedConfigCategory == null && selectedSearchCategory == null);
            boolean isHover = MouseUtils.isHovered(posX + 4.0f, itemY, sidebarW - 8.0f, 18.0f, mouseX, mouseY);

            cat.setX(posX + 4.0f);
            cat.setY(itemY);
            cat.setHovered(isHover);
            cat.setSelected(isActive);

            String catName = cat.getCategory().getName();

            if (isActive) {
                // active: accent text + thin left accent pip
                RoundedUtils.drawRound(posX + 5.0f, itemY + 3.0f, 2.0f, 12.0f, 1.0f, ACCENT);
                Fonts.interMedium.get(10.5f).drawString(catName, posX + 14.0f, itemY + 3.0f, Color.white.getRGB());
            } else {
                // inactive: muted text that brightens on hover
                int textAlpha = isHover ? 200 : 120;
                Fonts.interRegular.get(10.5f).drawString(catName, posX + 14.0f, itemY + 3.0f, new Color(255, 255, 255, textAlpha).getRGB());
            }

            itemY += 20.0f;
        }

        // bottom sidebar action buttons
        float btnH = 16.0f;
        float cfgBtnY = posY + height - 22.0f;
        float saveBtnY = cfgBtnY - 20.0f;
        float saveBtnW = sidebarW - 10.0f;

        boolean saveHover = MouseUtils.isHovered(posX + 5.0f, saveBtnY, saveBtnW, btnH, mouseX, mouseY);
        Fonts.interRegular.get(9).drawString("Save Profile", posX + 10.0f, saveBtnY + 3.5f, saveHover ? new Color(255, 255, 255, 200).getRGB() : new Color(255, 255, 255, 80).getRGB());

        boolean cfgHover = MouseUtils.isHovered(posX + 5.0f, cfgBtnY, saveBtnW, btnH, mouseX, mouseY);
        Fonts.interMedium.get(9).drawString("Config Manager", posX + 10.0f, cfgBtnY + 3.5f, cfgHover ? Color.white.getRGB() : new Color(255, 255, 255, 100).getRGB());

        // ---- content area subheader: section label + search field ----
        String activeSection = selectedSearchCategory != null ? "Search" : (selectedConfigCategory != null ? "Configs" : (selectedCategory != null ? selectedCategory.getCategory().getName() : "Modules"));
        Fonts.interMedium.get(10).drawString(activeSection, posX + sidebarW + 12.0f, posY + headerH + 10.0f, new Color(255, 255, 255, 180).getRGB());

        // inline search field on the right of the subheader
        float searchW = 120.0f;
        float searchH = 18.0f;
        float searchX = posX + width - searchW - 10.0f;
        float searchY = posY + headerH + 6.0f;
        boolean searchHovered = MouseUtils.isHovered(searchX, searchY, searchW, searchH, mouseX, mouseY);

        // thin dark input well with subtle border
        RoundedUtils.drawRoundOutline(searchX, searchY, searchW, searchH, 2.0f, 0.5f, new Color(20, 20, 26, 240), searchHovered ? new Color(255, 255, 255, 24) : new Color(255, 255, 255, 10));

        boolean isSearchActive = (selectedSearchCategory != null);
        String filter = searchCategoryComponent.getFilter();
        boolean hasFilter = !filter.isEmpty();

        if (isSearchActive && searchCategoryComponent.isInputting()) {
            String cursor = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
            String display = hasFilter ? filter + cursor : cursor;
            if (!hasFilter && cursor.isEmpty()) {
                Fonts.interRegular.get(9).drawString("Search...", searchX + 7.0f, searchY + 4.5f, new Color(80, 80, 95).getRGB());
            } else {
                Fonts.interRegular.get(9).drawString(display, searchX + 7.0f, searchY + 4.5f, new Color(220, 220, 230).getRGB());
            }
        } else if (hasFilter) {
            Fonts.interRegular.get(9).drawString(filter, searchX + 7.0f, searchY + 4.5f, new Color(220, 220, 230).getRGB());
        } else {
            Fonts.interRegular.get(9).drawString("Search...", searchX + 7.0f, searchY + 4.5f, new Color(80, 80, 95).getRGB());
        }

        // clear button when search has text
        if (hasFilter) {
            float clearBtnX = searchX + searchW - 12.0f;
            float clearBtnY2 = searchY + 4.0f;
            boolean clearHovered = MouseUtils.isHovered(clearBtnX - 3.0f, searchY, 15.0f, searchH, mouseX, mouseY);
            Fonts.interMedium.get(9).drawString("✕", clearBtnX, clearBtnY2, clearHovered ? Color.white.getRGB() : new Color(100, 100, 110).getRGB());
        }

        // subheader bottom separator
        RenderUtils.drawRect(posX + sidebarW + 1.0f, posY + headerH + 28.0f, width - sidebarW - 1.0f, 1.0f, SEPARATOR.getRGB());

        searchCategoryComponent.setSelected(selectedSearchCategory != null);
        configCategoryComponent.setSelected(selectedConfigCategory != null);

        // render either settings modal exclusively or content list to prevent z-fighting
        if (focusedModule != null) {
            drawFocusedSettingsModal(mouseX, mouseY);
        } else {
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

            // hover tooltip with module info
            drawModuleHoverTooltip(mouseX, mouseY);
        }

        RenderUtils.scaleEnd();
    }

    private void drawModuleHoverTooltip(int mouseX, int mouseY) {
        ModuleComponent hoveredModule = null;
        if (selectedCategory != null) {
            for (ModuleComponent comp : selectedCategory.getModuleComponents()) {
                if (comp.isVisible() && comp.isHovered()) {
                    hoveredModule = comp;
                    break;
                }
            }
        } else if (selectedSearchCategory != null) {
            for (ModuleComponent comp : searchCategoryComponent.getModuleComponents()) {
                if (comp.isVisible() && comp.isHovered()) {
                    hoveredModule = comp;
                    break;
                }
            }
        }

        if (hoveredModule == null) {
            return;
        }

        String desc = hoveredModule.getModule().getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = "No description.";
        }

        String name = hoveredModule.getModule().getName();
        int key = hoveredModule.getModule().getKeyBind();
        String keyStr = key == 0 ? "NONE" : Keyboard.getKeyName(key);
        String bindTag = "[" + (keyStr != null ? keyStr : "NONE") + "]";

        float nameW = Fonts.interMedium.get(9.5f).getStringWidth(name);
        float bindW = Fonts.interRegular.get(8.5f).getStringWidth(bindTag);
        float descW = Fonts.interRegular.get(8.5f).getStringWidth(desc);

        float tipW = Math.max(nameW + bindW + 20.0f, descW + 14.0f);
        tipW = Math.min(tipW, 220.0f);
        float tipH = 26.0f;

        float tipX = mouseX + 8.0f;
        float tipY = mouseY + 8.0f;

        ScaledResolution sr = new ScaledResolution(mc);
        if (tipX + tipW > sr.getScaledWidth() - 6.0f) {
            tipX = mouseX - tipW - 6.0f;
        }
        if (tipY + tipH > sr.getScaledHeight() - 6.0f) {
            tipY = mouseY - tipH - 6.0f;
        }

        // opaque tooltip with sharp corners and thin border
        RoundedUtils.drawRoundOutline(tipX, tipY, tipW, tipH, 2.0f, 0.5f, new Color(18, 18, 24, 250), new Color(255, 255, 255, 16));
        Fonts.interMedium.get(9.5f).drawString(name, tipX + 6.0f, tipY + 4.0f, Color.white.getRGB());
        Fonts.interRegular.get(8.5f).drawString(bindTag, tipX + tipW - bindW - 6.0f, tipY + 4.5f, new Color(90, 90, 105).getRGB());
        Fonts.interRegular.get(8.5f).drawString(desc, tipX + 6.0f, tipY + 15.0f, new Color(140, 140, 155).getRGB());
    }

    private void drawFocusedSettingsModal(int mouseX, int mouseY) {
        float modalX = posX + 118.0f;
        float modalY = posY + 36.0f;
        float modalW = width - 126.0f;
        float modalH = height - 44.0f;

        // solid opaque panel so underlying module rows never bleed through
        RoundedUtils.drawRoundOutline(modalX, modalY, modalW, modalH, 3.0f, 0.5f, new Color(16, 16, 22, 255), new Color(255, 255, 255, 10));

        // back navigation link
        boolean backHover = MouseUtils.isHovered(modalX + 8.0f, modalY + 6.0f, 40.0f, 16.0f, mouseX, mouseY);
        Fonts.interMedium.get(9.5f).drawString("← Back", modalX + 10.0f, modalY + 9.0f, backHover ? Color.white.getRGB() : new Color(120, 120, 135).getRGB());

        // module name as modal title
        Fonts.interMedium.get(11).drawString(focusedModule.getModule().getName(), modalX + 56.0f, modalY + 8.5f, Color.white.getRGB());

        // keybind button capsule in top-right
        int bindKey = focusedModule.getModule().getKeyBind();
        String keyName = bindKey == 0 ? "NONE" : Keyboard.getKeyName(bindKey);
        if (keyName == null) keyName = "NONE";
        String bindText = bindingListening ? "..." : keyName;
        float bindBtnW = Math.max(40.0f, Fonts.interRegular.get(9).getStringWidth(bindText) + 14.0f);
        float bindBtnX = modalX + modalW - bindBtnW - 10.0f;
        float bindBtnY = modalY + 6.0f;
        float bindBtnH = 16.0f;

        boolean bindHover = MouseUtils.isHovered(bindBtnX, bindBtnY, bindBtnW, bindBtnH, mouseX, mouseY);
        Color bindBorder = bindingListening ? ACCENT_DIM : new Color(255, 255, 255, bindHover ? 20 : 10);
        RoundedUtils.drawRoundOutline(bindBtnX, bindBtnY, bindBtnW, bindBtnH, 2.0f, 0.5f, new Color(22, 22, 30, 240), bindBorder);
        Fonts.interRegular.get(9).drawCenteredString(bindText, bindBtnX + bindBtnW / 2.0f, bindBtnY + 3.5f, bindingListening ? ACCENT.getRGB() : new Color(160, 160, 175).getRGB());

        // header divider
        RenderUtils.drawRect(modalX, modalY + 26.0f, modalW, 1.0f, SEPARATOR.getRGB());

        // optional description line below header
        String desc = focusedModule.getModule().getDescription();
        boolean hasDesc = desc != null && !desc.isEmpty();
        if (hasDesc) {
            Fonts.interRegular.get(9).drawString(desc, modalX + 10.0f, modalY + 32.0f, new Color(100, 100, 115).getRGB());
        }

        float settingsStartY = modalY + (hasDesc ? 44.0f : 32.0f);
        float settingsViewH = modalH - (hasDesc ? 50.0f : 38.0f);

        // empty state for modules with no configurable settings
        if (focusedModule.getSettings().isEmpty()) {
            Fonts.interRegular.get(9.5f).drawString("No settings available.", modalX + 10.0f, settingsStartY + 8.0f, new Color(80, 80, 95).getRGB());
            Fonts.interRegular.get(9).drawString("Right-click keybind above to rebind.", modalX + 10.0f, settingsStartY + 22.0f, new Color(60, 60, 72).getRGB());
        }

        float totalH = 0.0f;
        for (Component comp : focusedModule.getSettings()) {
            if (comp.isVisible()) {
                totalH += comp.getHeight() + 4.0f;
            }
        }

        maxSettingsScroll = Math.max(0.0f, totalH - settingsViewH);
        settingsScroll = MathUtils.interpolate(settingsScroll, targetSettingsScroll, 0.25f);

        RenderUtils.scissor(modalX, settingsStartY, modalW, settingsViewH, interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        float currY = settingsStartY - settingsScroll;
        for (Component comp : focusedModule.getSettings()) {
            if (!comp.isVisible()) continue;

            comp.setX(modalX + 8.0f);
            comp.setY(currY);
            comp.setWidth(modalW - 16.0f);

            comp.drawScreen(mouseX, mouseY);
            currY += comp.getHeight() + 4.0f;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float closeBtnX = posX + width - 26.0f;
        float closeBtnY = posY + 8.0f;
        if (MouseUtils.isHovered(closeBtnX, closeBtnY, 18.0f, 18.0f, mouseX, mouseY) && mouseButton == 0) {
            closing = true;
            return;
        }

        // if the module settings modal is open, it has absolute priority over covered components
        if (focusedModule != null) {
            float modalX = posX + 118.0f;
            float modalY = posY + 36.0f;
            float modalW = width - 126.0f;
            float modalH = height - 44.0f;

            if (MouseUtils.isHovered(modalX, modalY, modalW, modalH, mouseX, mouseY)) {
                // back button to return to module grid
                if (MouseUtils.isHovered(modalX + 8.0f, modalY + 6.0f, 40.0f, 16.0f, mouseX, mouseY) && mouseButton == 0) {
                    focusedModule = null;
                    bindingListening = false;
                    return;
                }

                // bind button in the top-right of the modal header
                int bindKey = focusedModule.getModule().getKeyBind();
                String keyName = bindKey == 0 ? "NONE" : Keyboard.getKeyName(bindKey);
                if (keyName == null) keyName = "NONE";
                String bindText = bindingListening ? "..." : keyName;
                float bindBtnW = Math.max(40.0f, Fonts.interRegular.get(9).getStringWidth(bindText) + 14.0f);
                float bindBtnX = modalX + modalW - bindBtnW - 10.0f;
                float bindBtnY = modalY + 6.0f;
                float bindBtnH = 16.0f;

                if (MouseUtils.isHovered(bindBtnX, bindBtnY, bindBtnW, bindBtnH, mouseX, mouseY) && mouseButton == 0) {
                    bindingListening = !bindingListening;
                    return;
                }

                // clicking anywhere else inside modal cancels key listening mode
                if (bindingListening) {
                    bindingListening = false;
                }

                for (Component comp : focusedModule.getSettings()) {
                    if (comp.isVisible()) {
                        comp.mouseClicked(mouseX, mouseY, mouseButton);
                    }
                }
                return;
            } else {
                // clicked outside modal boundary, dismiss modal
                focusedModule = null;
                bindingListening = false;
            }
        }

        float headerH = 32.0f;
        if (mouseButton == 0 && MouseUtils.isHovered(posX, posY, width, headerH, mouseX, mouseY)) {
            float navCenterX = posX + (width / 2.0f) - 60.0f;
            float navY = posY + 7.0f;
            
            String modsText = "Mods";
            float modsWidth = Fonts.interMedium.get(10).getStringWidth(modsText) + 16.0f;

            if (MouseUtils.isHovered(navCenterX, navY, modsWidth, 20.0f, mouseX, mouseY)) {
                selectedConfigCategory = null;
                selectedSearchCategory = null;
                focusedModule = null;
                return;
            }
            
            float cfgStartX = navCenterX + modsWidth + 6.0f;
            String cfgText = "Configs";
            float cfgWidth = Fonts.interMedium.get(10).getStringWidth(cfgText) + 16.0f;

            if (MouseUtils.isHovered(cfgStartX, navY, cfgWidth, 20.0f, mouseX, mouseY)) {
                if (selectedConfigCategory == null) {
                    configCategoryComponent.initCategory();
                }
                selectedConfigCategory = configCategoryComponent;
                selectedCategory = null;
                selectedSearchCategory = null;
                focusedModule = null;
                return;
            }

            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
            return;
        }

        float sidebarW = 110.0f;
        float sidebarY = posY + headerH + 1.0f;
        if (MouseUtils.isHovered(posX, sidebarY, sidebarW, height - headerH - 1.0f, mouseX, mouseY) && mouseButton == 0) {
            float itemY = sidebarY + 8.0f;
            for (Category cat : categories) {
                if (MouseUtils.isHovered(posX + 4.0f, itemY, sidebarW - 8.0f, 18.0f, mouseX, mouseY)) {
                    if (selectedCategory != cat) {
                        cat.initCategory();
                    }
                    selectedCategory = cat;
                    selectedConfigCategory = null;
                    selectedSearchCategory = null;
                    focusedModule = null;
                    return;
                }
                itemY += 20.0f;
            }

            float btnH = 16.0f;
            float cfgBtnY = posY + height - 22.0f;
            float saveBtnY = cfgBtnY - 20.0f;

            if (MouseUtils.isHovered(posX + 5.0f, saveBtnY, sidebarW - 10.0f, btnH, mouseX, mouseY)) {
                Demise.INSTANCE.getConfigManager().saveConfigs();
                return;
            }

            if (MouseUtils.isHovered(posX + 5.0f, cfgBtnY, sidebarW - 10.0f, btnH, mouseX, mouseY)) {
                if (selectedConfigCategory == null) {
                    configCategoryComponent.initCategory();
                }
                selectedConfigCategory = configCategoryComponent;
                selectedCategory = null;
                selectedSearchCategory = null;
                focusedModule = null;
                return;
            }
            return;
        }

        // search capsule click hitbox
        float searchW = 120.0f;
        float searchH = 18.0f;
        float searchX = posX + width - searchW - 10.0f;
        float searchY2 = posY + headerH + 6.0f;
        if (MouseUtils.isHovered(searchX, searchY2, searchW, searchH, mouseX, mouseY) && mouseButton == 0) {
            float clearBtnX = searchX + searchW - 12.0f;
            if (!searchCategoryComponent.getFilter().isEmpty() && MouseUtils.isHovered(clearBtnX - 4.0f, searchY2, 16.0f, searchH, mouseX, mouseY)) {
                searchCategoryComponent.clearFilter();
                return;
            }

            if (selectedSearchCategory == null) {
                previousCategory = selectedCategory;
                searchCategoryComponent.initCategory();
            }
            selectedSearchCategory = searchCategoryComponent;
            selectedCategory = null;
            selectedConfigCategory = null;
            focusedModule = null;
            searchCategoryComponent.setInputting(true);
            return;
        }

        if (selectedSearchCategory != null) {
            // keep inputting active in search view so clicking to toggle a module does not disable typing
            searchCategoryComponent.setInputting(true);
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

        if (focusedModule != null) {
            for (Component comp : focusedModule.getSettings()) {
                if (comp.isVisible()) {
                    comp.mouseReleased(mouseX, mouseY, state);
                }
            }
            return;
        }

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
        // intercept key press if actively listening for a module bind
        if (focusedModule != null && bindingListening) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                bindingListening = false;
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                // unbind module if backspace or delete is pressed
                focusedModule.getModule().setKeyBind(0);
                bindingListening = false;
                return;
            }
            // assign pressed keyboard keycode to module
            focusedModule.getModule().setKeyBind(keyCode);
            bindingListening = false;
            return;
        }

        if (keyCode == Keyboard.KEY_RSHIFT) {
            closing = !closing;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (focusedModule != null) {
                focusedModule = null;
                bindingListening = false;
                return;
            }
            if (selectedSearchCategory != null) {
                if (!searchCategoryComponent.getFilter().isEmpty()) {
                    searchCategoryComponent.clearFilter();
                    return;
                } else {
                    // return to previous category instead of abruptly closing gui
                    selectedSearchCategory = null;
                    searchCategoryComponent.setInputting(false);
                    selectedCategory = (previousCategory != null ? previousCategory : categories.get(0));
                    return;
                }
            }
            closing = true;
            return;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            selectedCategory = categories.get((categories.indexOf(selectedCategory) + 1) % categories.size());
            selectedConfigCategory = null;
            selectedSearchCategory = null;
            searchCategoryComponent.setInputting(false);
            focusedModule = null;
            return;
        }

        if (closing) {
            return;
        }

        if (focusedModule != null) {
            for (Component comp : focusedModule.getSettings()) {
                if (comp.isVisible()) {
                    comp.keyTyped(typedChar, keyCode);
                }
            }
            return;
        }

        if (selectedSearchCategory != null) {
            selectedSearchCategory.keyTyped(typedChar, keyCode);
            return;
        }

        // if typing in category view, jump directly into search for instant access
        if (selectedCategory != null && (Character.isLetterOrDigit(typedChar) || typedChar == ' ' || typedChar == '-' || typedChar == '_')) {
            previousCategory = selectedCategory;
            selectedSearchCategory = searchCategoryComponent;
            selectedCategory = null;
            searchCategoryComponent.setInputting(true);
            searchCategoryComponent.keyTyped(typedChar, keyCode);
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
        if (focusedModule != null) {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0) {
                float scrollAmount = wheel > 0 ? -25.0f : 25.0f;
                targetSettingsScroll = MathHelper.clamp_float(targetSettingsScroll + scrollAmount, 0.0f, maxSettingsScroll);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}