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
        interpolatedScale = 0;
        focusedModule = null;

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
        interpolatedScale = MathUtils.interpolate(interpolatedScale, !closing ? 1.0f : 0.0f, 0.25f);

        if (interpolatedScale < 0.01f && closing) {
            mc.displayGuiScreen(null);
            return;
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

        Color mainBg = new Color(25, 25, 25, 160);
        RoundedUtils.drawRoundOutline(posX, posY, width, height, 8.0f, 1.0f, mainBg, new Color(255, 255, 255, 30));

        // Horizontal line separator for header
        RenderUtils.drawRect(posX + 1.0f, posY + 40.0f, width - 2.0f, 1.0f, new Color(255, 255, 255, 30).getRGB());

        // Header Title
        Fonts.interBold.get(18).drawString("DEMISE", posX + 16.0f, posY + 16.0f, Color.white.getRGB());
        Fonts.interRegular.get(18).drawString("CLIENT", posX + 18.0f + Fonts.interBold.get(18).getStringWidth("DEMISE"), posY + 16.0f, new Color(180, 185, 195, 200).getRGB());

        float navCenterX = posX + (width / 2.0f) - 95.0f;
        float navY = posY + 9.0f;

        boolean modsSelected = (selectedConfigCategory == null);
        boolean configsSelected = (selectedConfigCategory != null);

        // Tabs: MODS and CONFIGS
        String modsText = "M O D S";
        float modsWidth = Fonts.interMedium.get(10).getStringWidth(modsText) + 20.0f;
        if (modsSelected) {
            RoundedUtils.drawRound(navCenterX, navY, modsWidth, 22.0f, 4.0f, new Color(255, 255, 255, 40));
        } else {
            RoundedUtils.drawRoundOutline(navCenterX, navY, modsWidth, 22.0f, 4.0f, 1.0f, new Color(0, 0, 0, 0), new Color(255, 255, 255, 40));
        }
        Fonts.interMedium.get(10).drawString(modsText, navCenterX + 10.0f, navY + 7.0f, Color.white.getRGB());

        String cfgText = "C O N F I G S";
        float cfgWidth = Fonts.interMedium.get(10).getStringWidth(cfgText) + 20.0f;
        if (configsSelected) {
            RoundedUtils.drawRound(navCenterX + modsWidth + 8.0f, navY, cfgWidth, 22.0f, 4.0f, new Color(255, 255, 255, 40));
        } else {
            RoundedUtils.drawRoundOutline(navCenterX + modsWidth + 8.0f, navY, cfgWidth, 22.0f, 4.0f, 1.0f, new Color(0, 0, 0, 0), new Color(255, 255, 255, 40));
        }
        Fonts.interMedium.get(10).drawString(cfgText, navCenterX + modsWidth + 18.0f, navY + 7.0f, Color.white.getRGB());

        // Close Button
        float closeBtnX = posX + width - 30.0f;
        float closeBtnY = posY + 9.0f;
        boolean closeHovered = MouseUtils.isHovered(closeBtnX, closeBtnY, 20.0f, 20.0f, mouseX, mouseY);
        RoundedUtils.drawRoundOutline(closeBtnX, closeBtnY, 20.0f, 20.0f, 4.0f, 1.0f, closeHovered ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 10), new Color(255, 255, 255, 40));
        Fonts.interBold.get(11).drawString("X", closeBtnX + 6.5f, closeBtnY + 5.0f, Color.white.getRGB());

        float sidebarW = 125.0f;
        float sidebarY = posY + 41.0f;
        float sidebarH = height - 42.0f;
        
        // Vertical line separator for sidebar
        RenderUtils.drawRect(posX + sidebarW, sidebarY, 1.0f, sidebarH, new Color(255, 255, 255, 30).getRGB());

        float itemY = sidebarY + 8.0f;
        for (Category cat : categories) {
            boolean isSelected = (selectedCategory == cat && selectedConfigCategory == null && selectedSearchCategory == null);
            boolean isHovered = MouseUtils.isHovered(posX + 6.0f, itemY, sidebarW - 12.0f, 24.0f, mouseX, mouseY);

            cat.setX(posX + 6.0f);
            cat.setY(itemY);
            cat.setHovered(isHovered);
            cat.setSelected(isSelected);

            String catName = cat.getCategory().getName().substring(0, 1).toUpperCase() + cat.getCategory().getName().substring(1).toLowerCase();
            
            if (isSelected) {
                RoundedUtils.drawRoundOutline(posX + 6.0f, itemY, sidebarW - 12.0f, 24.0f, 2.0f, 1.0f, new Color(255, 255, 255, 20), new Color(255, 255, 255, 40));
                RenderUtils.drawRect(posX + 6.0f, itemY, 2.0f, 24.0f, new Color(255, 170, 0, 255).getRGB()); // Highlight left
                Fonts.interMedium.get(12).drawString(catName, posX + 16.0f, itemY + 6.5f, Color.white.getRGB());
            } else {
                if (isHovered) {
                    RoundedUtils.drawRound(posX + 6.0f, itemY, sidebarW - 12.0f, 24.0f, 2.0f, new Color(255, 255, 255, 10));
                }
                Fonts.interMedium.get(12).drawString(catName, posX + 16.0f, itemY + 6.5f, new Color(180, 185, 195, 200).getRGB());
            }

            itemY += 27.0f;
        }

        float saveBtnY = posY + height - 48.0f;
        float saveBtnW = sidebarW - 12.0f;
        boolean saveHover = MouseUtils.isHovered(posX + 6.0f, saveBtnY, saveBtnW, 18.0f, mouseX, mouseY);
        RoundedUtils.drawRoundOutline(posX + 6.0f, saveBtnY, saveBtnW, 18.0f, 4.0f, 1.0f, saveHover ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 0), new Color(255, 255, 255, 40));
        Fonts.interMedium.get(9).drawString("SAVE AS PROFILE", posX + 12.0f, saveBtnY + 5.0f, new Color(180, 185, 195, 220).getRGB());

        float cfgBtnY = posY + height - 26.0f;
        boolean cfgHover = MouseUtils.isHovered(posX + 6.0f, cfgBtnY, saveBtnW, 18.0f, mouseX, mouseY);
        RoundedUtils.drawRound(posX + 6.0f, cfgBtnY, saveBtnW, 18.0f, 4.0f, cfgHover ? new Color(70, 130, 246, 255) : new Color(59, 130, 246, 220));
        Fonts.interMedium.get(9).drawString("CONFIG MANAGER", posX + 12.0f, cfgBtnY + 5.0f, Color.white.getRGB());

        float contentStartX = posX + 138.0f;
        float topPillY = posY + 46.0f;

        float searchW = 110.0f;
        float searchH = 18.0f;
        float searchX = posX + width - searchW - 10.0f;
        RoundedUtils.drawRoundOutline(searchX, topPillY, searchW, searchH, 4.0f, 1.0f, new Color(0, 0, 0, 40), new Color(255, 255, 255, 40));

        if (selectedSearchCategory != null && searchCategoryComponent.isInputting()) {
            String cursor = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
            String query = searchCategoryComponent.getFilter();
            String display = query.isEmpty() ? "Search..." + cursor : query + cursor;
            Fonts.interRegular.get(10).drawString(display, searchX + 6.0f, topPillY + 5.0f, Color.white.getRGB());
        } else if (selectedSearchCategory != null && !searchCategoryComponent.getFilter().isEmpty()) {
            Fonts.interRegular.get(10).drawString(searchCategoryComponent.getFilter(), searchX + 6.0f, topPillY + 5.0f, Color.white.getRGB());
        } else {
            Fonts.interRegular.get(10).drawString("Search...", searchX + 6.0f, topPillY + 5.0f, new Color(120, 125, 135, 180).getRGB());
        }

        searchCategoryComponent.setSelected(selectedSearchCategory != null);
        configCategoryComponent.setSelected(selectedConfigCategory != null);

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

        if (focusedModule != null) {
            drawFocusedSettingsModal(mouseX, mouseY);
        }

        RenderUtils.scaleEnd();
    }

    private void drawFocusedSettingsModal(int mouseX, int mouseY) {
        float modalX = posX + 138.0f;
        float modalY = posY + 46.0f;
        float modalW = width - 146.0f;
        float modalH = height - 54.0f;

        RoundedUtils.drawRound(modalX, modalY, modalW, modalH, 8.0f, new Color(18, 20, 25, 250));
        RenderUtils.drawRect(modalX + 1.0f, modalY + 1.0f, modalW - 2.0f, 1.0f, new Color(50, 55, 68, 160).getRGB());

        boolean backHover = MouseUtils.isHovered(modalX + 8.0f, modalY + 8.0f, 50.0f, 18.0f, mouseX, mouseY);
        RoundedUtils.drawRound(modalX + 8.0f, modalY + 8.0f, 50.0f, 18.0f, 4.0f, backHover ? new Color(48, 53, 65, 240) : new Color(32, 35, 44, 200));
        Fonts.interMedium.get(10).drawString("← BACK", modalX + 13.0f, modalY + 13.0f, Color.white.getRGB());

        Fonts.interBold.get(13).drawString(focusedModule.getModule().getName() + " Settings", modalX + 66.0f, modalY + 12.0f, Color.white.getRGB());
        RenderUtils.drawRect(modalX, modalY + 30.0f, modalW, 1.0f, new Color(38, 42, 50, 160).getRGB());

        float settingsStartY = modalY + 36.0f;
        float settingsViewH = modalH - 42.0f;

        float totalH = 0.0f;
        for (Component comp : focusedModule.getSettings()) {
            if (comp.isVisible()) {
                totalH += comp.getHeight() + 4.0f;
            }
        }

        maxSettingsScroll = Math.max(0.0f, totalH - settingsViewH);
        settingsScroll = MathUtils.interpolate(settingsScroll, targetSettingsScroll, 0.15f);

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
            float itemY = sidebarY + 8.0f;
            for (Category cat : categories) {
                if (MouseUtils.isHovered(posX + 6.0f, itemY, sidebarW - 12.0f, 24.0f, mouseX, mouseY)) {
                    if (selectedCategory != cat) {
                        cat.initCategory();
                    }
                    selectedCategory = cat;
                    selectedConfigCategory = null;
                    selectedSearchCategory = null;
                    focusedModule = null;
                    return;
                }
                itemY += 27.0f;
            }

            float saveBtnY = posY + height - 48.0f;
            if (MouseUtils.isHovered(posX + 6.0f, saveBtnY, sidebarW - 12.0f, 18.0f, mouseX, mouseY)) {
                Demise.INSTANCE.getConfigManager().saveConfigs();
                return;
            }

            float cfgBtnY = posY + height - 26.0f;
            if (MouseUtils.isHovered(posX + 6.0f, cfgBtnY, sidebarW - 12.0f, 18.0f, mouseX, mouseY)) {
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

        // search capsule click
        float searchW = 110.0f;
        float searchX = posX + width - searchW - 10.0f;
        float topPillY = posY + 46.0f;
        if (MouseUtils.isHovered(searchX, topPillY, searchW, 18.0f, mouseX, mouseY) && mouseButton == 0) {
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
            float modalX = posX + 138.0f;
            float modalY = posY + 46.0f;
            if (MouseUtils.isHovered(modalX + 8.0f, modalY + 8.0f, 50.0f, 18.0f, mouseX, mouseY) && mouseButton == 0) {
                focusedModule = null;
                return;
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
        if (keyCode == Keyboard.KEY_RSHIFT) {
            closing = !closing;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (focusedModule != null) {
                focusedModule = null;
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