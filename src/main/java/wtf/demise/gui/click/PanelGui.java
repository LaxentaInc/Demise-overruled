package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.ShaderEvent;
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

    public PanelGui() {
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);

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

    private String getCategoryIcon(ModuleCategory cat) {
        if (cat == null) return "★";
        switch (cat) {
            case Combat: return "⚔";
            case Legit: return "🎯";
            case Movement: return "🏃";
            case Player: return "🛡";
            case Visual: return "👁";
            case Exploit: return "⚡";
            case Misc: return "📦";
            default: return "★";
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

        Color mainBg = new Color(14, 16, 20, 245);
        RoundedUtils.drawRoundOutline(posX, posY, width, height, 6.0f, 0.5f, mainBg, new Color(255, 255, 255, 15));

        // horizontal line separator for header
        RenderUtils.drawRect(posX + 1.0f, posY + 40.0f, width - 2.0f, 1.0f, new Color(255, 255, 255, 12).getRGB());

        // header title typography
        Fonts.interBold.get(16).drawString("DEMISE", posX + 16.0f, posY + 16.0f, Color.white.getRGB());
        Fonts.interRegular.get(16).drawString("CLIENT", posX + 18.0f + Fonts.interBold.get(16).getStringWidth("DEMISE"), posY + 16.0f, new Color(160, 165, 180, 200).getRGB());

        float navCenterX = posX + (width / 2.0f) - 95.0f;
        float navY = posY + 9.0f;

        boolean modsSelected = (selectedConfigCategory == null);
        boolean configsSelected = (selectedConfigCategory != null);

        // tabs: mods and configs
        String modsText = "M O D S";
        float modsWidth = Fonts.interMedium.get(10).getStringWidth(modsText) + 20.0f;
        if (modsSelected) {
            RoundedUtils.drawRound(navCenterX, navY, modsWidth, 22.0f, 3.0f, new Color(255, 255, 255, 30));
        } else {
            RoundedUtils.drawRoundOutline(navCenterX, navY, modsWidth, 22.0f, 3.0f, 0.5f, new Color(0, 0, 0, 0), new Color(255, 255, 255, 20));
        }
        Fonts.interMedium.get(10).drawString(modsText, navCenterX + 10.0f, navY + 7.0f, Color.white.getRGB());

        String cfgText = "C O N F I G S";
        float cfgWidth = Fonts.interMedium.get(10).getStringWidth(cfgText) + 20.0f;
        if (configsSelected) {
            RoundedUtils.drawRound(navCenterX + modsWidth + 8.0f, navY, cfgWidth, 22.0f, 3.0f, new Color(255, 255, 255, 30));
        } else {
            RoundedUtils.drawRoundOutline(navCenterX + modsWidth + 8.0f, navY, cfgWidth, 22.0f, 3.0f, 0.5f, new Color(0, 0, 0, 0), new Color(255, 255, 255, 20));
        }
        Fonts.interMedium.get(10).drawString(cfgText, navCenterX + modsWidth + 18.0f, navY + 7.0f, Color.white.getRGB());

        // close button
        float closeBtnX = posX + width - 30.0f;
        float closeBtnY = posY + 9.0f;
        boolean closeHovered = MouseUtils.isHovered(closeBtnX, closeBtnY, 20.0f, 20.0f, mouseX, mouseY);
        RoundedUtils.drawRound(closeBtnX, closeBtnY, 20.0f, 20.0f, 3.0f, closeHovered ? new Color(255, 255, 255, 30) : new Color(25, 27, 34, 200));
        Fonts.interBold.get(11).drawString("X", closeBtnX + 6.5f, closeBtnY + 5.0f, Color.white.getRGB());

        float sidebarW = 125.0f;
        float sidebarY = posY + 41.0f;
        float sidebarH = height - 42.0f;
        
        // vertical line separator for sidebar
        RenderUtils.drawRect(posX + sidebarW, sidebarY, 1.0f, sidebarH, new Color(255, 255, 255, 12).getRGB());

        float itemY = sidebarY + 6.0f;
        for (Category cat : categories) {
            boolean isSelected = (selectedCategory == cat && selectedConfigCategory == null && selectedSearchCategory == null);
            boolean isHovered = MouseUtils.isHovered(posX + 6.0f, itemY, sidebarW - 12.0f, 19.0f, mouseX, mouseY);

            cat.setX(posX + 6.0f);
            cat.setY(itemY);
            cat.setHovered(isHovered);
            cat.setSelected(isSelected);

            String catName = cat.getCategory().getName().substring(0, 1).toUpperCase() + cat.getCategory().getName().substring(1).toLowerCase();

            if (isSelected) {
                // sharp minimalist active category container with subtle border
                RoundedUtils.drawRoundOutline(posX + 6.0f, itemY, sidebarW - 12.0f, 19.0f, 2.0f, 0.5f, new Color(28, 31, 38, 255), new Color(255, 255, 255, 25));
                Fonts.interMedium.get(11).drawString(catName, posX + 16.0f, itemY + 4.5f, Color.white.getRGB());
            } else {
                if (isHovered) {
                    RoundedUtils.drawRound(posX + 6.0f, itemY, sidebarW - 12.0f, 19.0f, 2.0f, new Color(22, 24, 30, 200));
                }
                Fonts.interMedium.get(11).drawString(catName, posX + 16.0f, itemY + 4.5f, new Color(140, 145, 155, 220).getRGB());
            }

            itemY += 22.0f;
        }

        // compact bottom buttons positioned to prevent collision
        float btnH = 17.0f;
        float cfgBtnY = posY + height - 24.0f;
        float saveBtnY = cfgBtnY - 21.0f;
        float saveBtnW = sidebarW - 12.0f;

        boolean saveHover = MouseUtils.isHovered(posX + 6.0f, saveBtnY, saveBtnW, btnH, mouseX, mouseY);
        RoundedUtils.drawRoundOutline(posX + 6.0f, saveBtnY, saveBtnW, btnH, 2.0f, 0.5f, saveHover ? new Color(28, 31, 38, 240) : new Color(18, 20, 25, 200), new Color(255, 255, 255, 22));
        Fonts.interMedium.get(9).drawString("SAVE AS PROFILE", posX + 12.0f, saveBtnY + 4.5f, new Color(175, 180, 190, 220).getRGB());

        boolean cfgHover = MouseUtils.isHovered(posX + 6.0f, cfgBtnY, saveBtnW, btnH, mouseX, mouseY);
        RoundedUtils.drawRoundOutline(posX + 6.0f, cfgBtnY, saveBtnW, btnH, 2.0f, 0.5f, cfgHover ? new Color(34, 38, 48, 255) : new Color(24, 27, 34, 230), new Color(255, 255, 255, 30));
        Fonts.interMedium.get(9).drawString("CONFIG MANAGER", posX + 12.0f, cfgBtnY + 4.5f, Color.white.getRGB());

        // subheader row: minimalist section breadcrumb and dedicated search bar
        String activeSection = selectedSearchCategory != null ? "SEARCH" : (selectedConfigCategory != null ? "CONFIGS" : (selectedCategory != null ? selectedCategory.getCategory().getName().toUpperCase() : "MODULES"));
        Fonts.interBold.get(10).drawString(">", posX + 138.0f, posY + 50.0f, new Color(200, 205, 215, 200).getRGB());
        Fonts.interMedium.get(10).drawString(activeSection + " // DIRECTORY", posX + 148.0f, posY + 50.0f, new Color(145, 150, 160, 200).getRGB());

        float searchW = 120.0f;
        float searchH = 18.0f;
        float searchX = posX + width - searchW - 12.0f;
        float searchY = posY + 46.0f;
        RoundedUtils.drawRoundOutline(searchX, searchY, searchW, searchH, 2.0f, 0.5f, new Color(16, 18, 22, 230), new Color(255, 255, 255, 25));

        if (selectedSearchCategory != null && searchCategoryComponent.isInputting()) {
            String cursor = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
            String query = searchCategoryComponent.getFilter();
            String display = query.isEmpty() ? "Search..." + cursor : query + cursor;
            Fonts.interRegular.get(10).drawString(display, searchX + 6.0f, searchY + 4.5f, Color.white.getRGB());
        } else if (selectedSearchCategory != null && !searchCategoryComponent.getFilter().isEmpty()) {
            Fonts.interRegular.get(10).drawString(searchCategoryComponent.getFilter(), searchX + 6.0f, searchY + 4.5f, Color.white.getRGB());
        } else {
            Fonts.interRegular.get(10).drawString("Search...", searchX + 6.0f, searchY + 4.5f, new Color(120, 125, 135, 180).getRGB());
        }

        // subheader bottom separator line
        RenderUtils.drawRect(posX + 126.0f, posY + 69.0f, width - 126.0f, 1.0f, new Color(255, 255, 255, 10).getRGB());

        searchCategoryComponent.setSelected(selectedSearchCategory != null);
        configCategoryComponent.setSelected(selectedConfigCategory != null);

        // render either settings modal exclusively or content list to prevent bleed
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

            // draw rich floating tooltip describing the currently hovered module
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
            desc = "No description available.";
        }

        String name = hoveredModule.getModule().getName();
        int key = hoveredModule.getModule().getKeyBind();
        String keyStr = key == 0 ? "NONE" : Keyboard.getKeyName(key);
        String bindTag = "[ " + (keyStr != null ? keyStr : "NONE") + " ]";

        float nameW = Fonts.interBold.get(10).getStringWidth(name);
        float bindW = Fonts.interMedium.get(9).getStringWidth(bindTag);
        float descW = Fonts.interRegular.get(9).getStringWidth(desc);

        float tipW = Math.max(nameW + bindW + 24.0f, descW + 18.0f);
        float tipH = 28.0f;

        float tipX = mouseX + 10.0f;
        float tipY = mouseY + 10.0f;

        ScaledResolution sr = new ScaledResolution(mc);
        if (tipX + tipW > sr.getScaledWidth() - 8.0f) {
            tipX = mouseX - tipW - 8.0f;
        }
        if (tipY + tipH > sr.getScaledHeight() - 8.0f) {
            tipY = mouseY - tipH - 8.0f;
        }

        // render dark elevated tooltip box with crisp border
        RoundedUtils.drawRoundOutline(tipX, tipY, tipW, tipH, 3.0f, 0.5f, new Color(12, 13, 17, 245), new Color(255, 255, 255, 30));
        Fonts.interBold.get(10).drawString(name, tipX + 8.0f, tipY + 5.0f, Color.white.getRGB());
        Fonts.interMedium.get(9).drawString(bindTag, tipX + tipW - bindW - 8.0f, tipY + 5.5f, new Color(150, 155, 170).getRGB());
        Fonts.interRegular.get(9).drawString(desc, tipX + 8.0f, tipY + 16.0f, new Color(180, 185, 195).getRGB());
    }

    private void drawFocusedSettingsModal(int mouseX, int mouseY) {
        float modalX = posX + 134.0f;
        float modalY = posY + 44.0f;
        float modalW = width - 142.0f;
        float modalH = height - 52.0f;

        // solid opaque background so underlying elements never bleed through
        RoundedUtils.drawRoundOutline(modalX, modalY, modalW, modalH, 2.0f, 0.5f, new Color(12, 13, 17, 255), new Color(255, 255, 255, 20));

        boolean backHover = MouseUtils.isHovered(modalX + 8.0f, modalY + 6.0f, 48.0f, 18.0f, mouseX, mouseY);
        RoundedUtils.drawRoundOutline(modalX + 8.0f, modalY + 6.0f, 48.0f, 18.0f, 2.0f, 0.5f, backHover ? new Color(32, 36, 46, 255) : new Color(20, 23, 29, 240), new Color(255, 255, 255, 25));
        Fonts.interMedium.get(10).drawString("← BACK", modalX + 11.0f, modalY + 10.5f, Color.white.getRGB());

        Fonts.interBold.get(12).drawString(focusedModule.getModule().getName() + " Settings", modalX + 64.0f, modalY + 10.0f, Color.white.getRGB());

        // interactive module keybind capsule button placed in modal header row
        int bindKey = focusedModule.getModule().getKeyBind();
        String keyName = bindKey == 0 ? "NONE" : Keyboard.getKeyName(bindKey);
        if (keyName == null) keyName = "NONE";
        String bindText = bindingListening ? "BIND: ..." : "BIND: " + keyName;
        float bindBtnW = Math.max(68.0f, Fonts.interMedium.get(10).getStringWidth(bindText) + 16.0f);
        float bindBtnX = modalX + modalW - bindBtnW - 10.0f;
        float bindBtnY = modalY + 6.0f;
        float bindBtnH = 18.0f;

        boolean bindHover = MouseUtils.isHovered(bindBtnX, bindBtnY, bindBtnW, bindBtnH, mouseX, mouseY);
        Color bindBg = bindingListening
                ? new Color(50, 56, 72, 255)
                : (bindHover ? new Color(32, 36, 46, 255) : new Color(20, 23, 29, 240));
        Color bindBorder = bindingListening
                ? new Color(220, 225, 235, 160)
                : new Color(255, 255, 255, 25);
        RoundedUtils.drawRoundOutline(bindBtnX, bindBtnY, bindBtnW, bindBtnH, 2.0f, 0.5f, bindBg, bindBorder);
        Fonts.interMedium.get(10).drawCenteredString(bindText, bindBtnX + bindBtnW / 2.0f, bindBtnY + 4.5f, bindingListening ? Color.white.getRGB() : new Color(200, 205, 215).getRGB());

        RenderUtils.drawRect(modalX, modalY + 28.0f, modalW, 1.0f, new Color(255, 255, 255, 12).getRGB());

        // render module description text below modal header line
        String desc = focusedModule.getModule().getDescription();
        boolean hasDesc = desc != null && !desc.isEmpty();
        if (hasDesc) {
            Fonts.interRegular.get(9).drawString(desc, modalX + 12.0f, modalY + 34.0f, new Color(150, 155, 170).getRGB());
        }

        float settingsStartY = modalY + (hasDesc ? 48.0f : 34.0f);
        float settingsViewH = modalH - (hasDesc ? 54.0f : 40.0f);

        // empty state message for modules that only configure keybinds
        if (focusedModule.getSettings().isEmpty()) {
            Fonts.interRegular.get(10).drawString("No configurable values for this module.", modalX + 12.0f, settingsStartY + 10.0f, new Color(140, 145, 158).getRGB());
            Fonts.interRegular.get(9).drawString("Click the BIND button in the top-right above to change its key.", modalX + 12.0f, settingsStartY + 24.0f, new Color(100, 105, 115).getRGB());
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

            comp.setX(modalX + 10.0f);
            comp.setY(currY);
            comp.setWidth(modalW - 20.0f);

            comp.drawScreen(mouseX, mouseY);
            currY += comp.getHeight() + 4.0f;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @EventPriority(100)
    @EventTarget
    public void onShader2D(ShaderEvent e) {
        if (mc.currentScreen != this) return;

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtils.scaleStart(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, interpolatedScale);
        if (e.getShaderType() != ShaderEvent.ShaderType.GLOW) {
            RoundedUtils.drawShaderRound(posX, posY, width, height, 10.0f, Color.black);
        } else {
            RoundedUtils.drawGradientPreset(posX, posY, width, height, 10.0f);
        }
        RenderUtils.scaleEnd();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float closeBtnX = posX + width - 30.0f;
        float closeBtnY = posY + 9.0f;
        if (MouseUtils.isHovered(closeBtnX, closeBtnY, 20.0f, 20.0f, mouseX, mouseY) && mouseButton == 0) {
            closing = true;
            return;
        }

        if (mouseButton == 0 && MouseUtils.isHovered(posX, posY, width, 40.0f, mouseX, mouseY)) {
            float navCenterX = posX + (width / 2.0f) - 95.0f;
            float navY = posY + 9.0f;
            
            String modsText = "M O D S";
            float modsWidth = Fonts.interMedium.get(10).getStringWidth(modsText) + 20.0f;

            if (MouseUtils.isHovered(navCenterX, navY, modsWidth, 22.0f, mouseX, mouseY)) {
                selectedConfigCategory = null;
                selectedSearchCategory = null;
                focusedModule = null;
                return;
            }
            
            String cfgText = "C O N F I G S";
            float cfgWidth = Fonts.interMedium.get(10).getStringWidth(cfgText) + 20.0f;

            if (MouseUtils.isHovered(navCenterX + modsWidth + 8.0f, navY, cfgWidth, 22.0f, mouseX, mouseY)) {
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

        float sidebarW = 125.0f;
        float sidebarY = posY + 41.0f;
        if (MouseUtils.isHovered(posX, sidebarY, sidebarW, height - 41.0f, mouseX, mouseY) && mouseButton == 0) {
            float itemY = sidebarY + 6.0f;
            for (Category cat : categories) {
                if (MouseUtils.isHovered(posX + 6.0f, itemY, sidebarW - 12.0f, 19.0f, mouseX, mouseY)) {
                    if (selectedCategory != cat) {
                        cat.initCategory();
                    }
                    selectedCategory = cat;
                    selectedConfigCategory = null;
                    selectedSearchCategory = null;
                    focusedModule = null;
                    return;
                }
                itemY += 22.0f;
            }

            float btnH = 17.0f;
            float cfgBtnY = posY + height - 24.0f;
            float saveBtnY = cfgBtnY - 21.0f;

            if (MouseUtils.isHovered(posX + 6.0f, saveBtnY, sidebarW - 12.0f, btnH, mouseX, mouseY)) {
                Demise.INSTANCE.getConfigManager().saveConfigs();
                return;
            }

            if (MouseUtils.isHovered(posX + 6.0f, cfgBtnY, sidebarW - 12.0f, btnH, mouseX, mouseY)) {
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
        float searchX = posX + width - searchW - 12.0f;
        float searchY = posY + 46.0f;
        if (MouseUtils.isHovered(searchX, searchY, searchW, searchH, mouseX, mouseY) && mouseButton == 0) {
            if (selectedSearchCategory == null) {
                searchCategoryComponent.initCategory();
            }
            selectedSearchCategory = searchCategoryComponent;
            selectedCategory = null;
            selectedConfigCategory = null;
            focusedModule = null;
            searchCategoryComponent.setInputting(true);
            return;
        } else if (selectedSearchCategory != null) {
            searchCategoryComponent.setInputting(false);
        }

        if (focusedModule != null) {
            float modalX = posX + 134.0f;
            float modalY = posY + 44.0f;
            if (MouseUtils.isHovered(modalX + 8.0f, modalY + 6.0f, 48.0f, 18.0f, mouseX, mouseY) && mouseButton == 0) {
                focusedModule = null;
                bindingListening = false;
                return;
            }

            // detect clicks on the bind capsule button
            float modalW = width - 142.0f;
            int bindKey = focusedModule.getModule().getKeyBind();
            String keyName = bindKey == 0 ? "NONE" : Keyboard.getKeyName(bindKey);
            if (keyName == null) keyName = "NONE";
            String bindText = bindingListening ? "BIND: ..." : "BIND: " + keyName;
            float bindBtnW = Math.max(68.0f, Fonts.interMedium.get(10).getStringWidth(bindText) + 16.0f);
            float bindBtnX = modalX + modalW - bindBtnW - 10.0f;
            float bindBtnY = modalY + 6.0f;
            float bindBtnH = 18.0f;

            if (MouseUtils.isHovered(bindBtnX, bindBtnY, bindBtnW, bindBtnH, mouseX, mouseY) && mouseButton == 0) {
                bindingListening = !bindingListening;
                return;
            }

            // clicking outside the bind button cancels listening mode
            if (bindingListening) {
                bindingListening = false;
            }

            for (Component comp : focusedModule.getSettings()) {
                if (comp.isVisible()) {
                    comp.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
            return;
        }

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
            closing = true;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            selectedCategory = categories.get((categories.indexOf(selectedCategory) + 1) % categories.size());
            selectedConfigCategory = null;
            selectedSearchCategory = null;
            focusedModule = null;
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