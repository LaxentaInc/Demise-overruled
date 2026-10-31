package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.components.Category;
import wtf.demise.gui.click.components.ModuleComponent;
import wtf.demise.gui.click.components.SearchCategory;
import wtf.demise.gui.click.components.config.ConfigCategoryComponent;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;
import wtf.demise.gui.font.Fonts;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * main clickgui window: sidebar navigation, top action bar,
 * draggable window, search mode, config manager and about dialog.
 */
public class PanelGui extends GuiScreen {

    // ---- static state shared with components ----
    private final List<Category> categories = new ArrayList<>();
    public static Category selectedCategory;
    public static Category previousCategory;
    public static ConfigCategoryComponent selectedConfigCategory;
    public static SearchCategory selectedSearchCategory;
    public static ModuleComponent focusedModule = null;
    public static boolean bindingListening = false;
    public static boolean dragging;
    public static float posX = -1, posY = -1;
    public static float width = 560.0f, height = 360.0f;
    public static float interpolatedScale;
    public static boolean showAboutModal = false;

    // ---- layout constants (single source of truth for draw + click) ----
    public static final float SIDEBAR_WIDTH = 118.0f;
    public static final float TOP_BAR_HEIGHT = 32.0f;
    public static final float NAV_START = 42.0f;
    public static final float NAV_ITEM_HEIGHT = 24.0f;
    public static final float NAV_STEP = 28.0f;
    public static final float PILL_HEIGHT = 22.0f;

    // cached avatar texture from widgets.svg
    private static ResourceLocation avatarTexture = null;

    public static ResourceLocation getAvatarTexture() {
        if (avatarTexture != null) return avatarTexture;
        try {
            File file = new File("C:/Users/MY-PC/Documents/demise-534fadf93d0842db6ca75064f2025650e3c4a3b4/src/main/resources/assets/minecraft/demise/img/widgets.png");
            if (file.exists()) {
                BufferedImage image = ImageIO.read(file);
                DynamicTexture dyn = new DynamicTexture(image);
                avatarTexture = net.minecraft.client.Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("widgets_avatar", dyn);
                return avatarTexture;
            }
        } catch (Exception ignored) {
            // fallback to classpath resource if direct file io fails
        }
        avatarTexture = new ResourceLocation("demise/img/widgets.png");
        return avatarTexture;
    }

    // ---- client info shown in the about dialog ----
    private static final String CLIENT_NAME = "DEMISE OVERRULED";
    private static final String CLIENT_VERSION = "DEV 2.1.0";
    private static final String CLIENT_BUILD = "#0001";
    private static final String CLIENT_BUILD_DATE = "2026-10-31";

    // ---- palette ----
    private static final Color BG_WINDOW = new Color(10, 13, 19, 255);
    private static final Color BG_SIDEBAR = new Color(7, 10, 15, 255);
    private static final Color BORDER_WINDOW = new Color(38, 48, 68, 255);
    private static final Color ACCENT_BLUE = new Color(0, 132, 255, 255);
    private static final Color ACCENT_CYAN = new Color(33, 150, 243, 255);
    private static final Color PILL_ACTIVE_BG = new Color(0, 132, 255, 45);
    private static final Color PILL_ACTIVE_BORDER = new Color(0, 150, 255, 220);
    private static final Color BUTTON_BG = new Color(18, 24, 34, 250);
    private static final Color BUTTON_BG_HOVER = new Color(26, 36, 52, 250);
    private static final Color BUTTON_BORDER = new Color(42, 54, 76, 255);
    private static final Color BUTTON_BORDER_HOVER = new Color(0, 132, 255, 180);
    private static final Color TEXT_PRIMARY = new Color(245, 248, 255, 255);
    private static final Color TEXT_BODY = new Color(215, 225, 240, 255);
    private static final Color TEXT_MUTED = new Color(155, 168, 188, 255);
    private static final Color TEXT_FAINT = new Color(110, 122, 142, 255);
    private static final Color STATUS_GREEN = new Color(76, 217, 100, 255);
    private static final Color DIVIDER = new Color(42, 54, 76, 160);

    // ---- sidebar navigation ----
    private static class NavItem {
        final String name;
        final ModuleCategory category;
        final boolean isConfig;

        NavItem(String name, ModuleCategory category, boolean isConfig) {
            this.name = name;
            this.category = category;
            this.isConfig = isConfig;
        }
    }

    private final List<NavItem> navItems = new ArrayList<>();
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    private float dragX, dragY;
    private boolean closing;
    private boolean initializedPosition;
    private long lastSaveTime;

    public PanelGui() {
        for (ModuleCategory category : ModuleCategory.values()) {
            categories.add(new Category(category, 0, 0));
        }

        configCategoryComponent = new ConfigCategoryComponent(0, 0);
        searchCategoryComponent = new SearchCategory();

        navItems.add(new NavItem("Combat", ModuleCategory.Combat, false));
        navItems.add(new NavItem("Movement", ModuleCategory.Movement, false));
        navItems.add(new NavItem("Player", ModuleCategory.Player, false));
        navItems.add(new NavItem("Render", ModuleCategory.Visual, false));
        navItems.add(new NavItem("Config", null, true));

        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
            selectedCategory.setSelected(true);
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

    // ---- button rects: shared by drawscreen and mouseclicked so hitboxes never drift ----
    private float[] saveButtonRect() {
        return new float[]{posX + SIDEBAR_WIDTH + 14.0f, posY + (TOP_BAR_HEIGHT - PILL_HEIGHT) / 2.0f, 58.0f, PILL_HEIGHT};
    }

    private float[] searchButtonRect() {
        return new float[]{posX + width - 14.0f - 62.0f, posY + (TOP_BAR_HEIGHT - PILL_HEIGHT) / 2.0f, 62.0f, PILL_HEIGHT};
    }

    private float[] aboutButtonRect() {
        return new float[]{posX + width - 14.0f - 62.0f - 8.0f - 56.0f, posY + (TOP_BAR_HEIGHT - PILL_HEIGHT) / 2.0f, 56.0f, PILL_HEIGHT};
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

        // auto-fit inside the active window resolution
        width = Math.max(340.0f, Math.min(560.0f, screenW - 20.0f));
        height = Math.max(220.0f, Math.min(370.0f, screenH - 20.0f));

        if (!initializedPosition || posX < 0 || posY < 0) {
            posX = (screenW - width) / 2.0f;
            posY = (screenH - height) / 2.0f;
            initializedPosition = true;
        } else {
            posX = MathHelper.clamp_float(posX, 5.0f, Math.max(5.0f, screenW - width - 5.0f));
            posY = MathHelper.clamp_float(posY, 5.0f, Math.max(5.0f, screenH - height - 5.0f));
        }

        if (selectedCategory != null) {
            selectedCategory.setSelected(true);
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

        width = Math.max(340.0f, Math.min(560.0f, screenW - 20.0f));
        height = Math.max(220.0f, Math.min(370.0f, screenH - 20.0f));

        if (dragging) {
            posX = MathHelper.clamp_float(mouseX - dragX, 5.0f, Math.max(5.0f, screenW - width - 5.0f));
            posY = MathHelper.clamp_float(mouseY - dragY, 5.0f, Math.max(5.0f, screenH - height - 5.0f));
        }

        RenderUtils.scaleStart(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, interpolatedScale);

        // ---- window frame ----
        RoundedUtils.drawRound(posX - 3.0f, posY - 3.0f, width + 6.0f, height + 6.0f, 10.0f, new Color(0, 0, 0, 90));
        RoundedUtils.drawRound(posX, posY, width, height, 10.0f, BG_WINDOW);

        // ---- sidebar ----
        RoundedUtils.drawRound(posX, posY, SIDEBAR_WIDTH, height, 10.0f, BG_SIDEBAR);
        // square off the sidebar right edge so it meets the content area cleanly
        RenderUtils.drawRect(posX + SIDEBAR_WIDTH - 8.0f, posY, 8.0f, height, BG_SIDEBAR.getRGB());
        // clean 1px hairline divider between sidebar and main grid
        RenderUtils.drawRect(posX + SIDEBAR_WIDTH, posY, 1.0f, height, DIVIDER.getRGB());

        // re-stroke the border on top so the sidebar doesn't swallow the frame
        RoundedUtils.drawRoundOutline(posX, posY, width, height, 10.0f, 1.0f, new Color(0, 0, 0, 0), BORDER_WINDOW);

        // brand wordmark: stylish two-line edition lockup
        Fonts.urbanist.get(19.0f).drawString("DEMISE", posX + 14.0f, posY + 8.5f, Color.white.getRGB());
        Fonts.urbanist.get(11.5f).drawString("OVERRULED", posX + 14.0f, posY + 23.5f, ACCENT_CYAN.getRGB());

        // ---- navigation ----
        float itemY = posY + NAV_START;
        for (NavItem item : navItems) {
            boolean selected = item.isConfig
                    ? selectedConfigCategory != null
                    : selectedConfigCategory == null
                    && selectedSearchCategory == null
                    && selectedCategory != null
                    && selectedCategory.getCategory() == item.category;

            float itemX = posX + 8.0f;
            float itemW = SIDEBAR_WIDTH - 16.0f;
            boolean hovered = MouseUtils.isHovered(itemX, itemY, itemW, NAV_ITEM_HEIGHT, mouseX, mouseY);

            if (selected) {
                RoundedUtils.drawRoundOutline(itemX, itemY, itemW, NAV_ITEM_HEIGHT, 5.0f, 0.8f, PILL_ACTIVE_BG, PILL_ACTIVE_BORDER);
                Fonts.urbanist.get(16.0f).drawString(item.name, itemX + 10.0f, itemY + (NAV_ITEM_HEIGHT - 16.0f) / 2.0f + 1.0f, Color.white.getRGB());
            } else {
                if (hovered) {
                    RoundedUtils.drawRound(itemX, itemY, itemW, NAV_ITEM_HEIGHT, 5.0f, new Color(255, 255, 255, 12));
                }
                Fonts.urbanist.get(16.0f).drawString(item.name, itemX + 10.0f, itemY + (NAV_ITEM_HEIGHT - 16.0f) / 2.0f + 1.0f,
                        (hovered ? Color.white : TEXT_BODY).getRGB());
            }

            itemY += NAV_STEP;
        }

        // ---- profile card ----
        float profileY = posY + height - 38.0f;
        float avatarX = posX + 12.0f;
        float avatarY = profileY + 2.0f;
        float avatarSize = 20.0f;

        // rounded avatar badge
        RoundedUtils.drawRound(avatarX - 1.0f, avatarY - 1.0f, avatarSize + 2.0f, avatarSize + 2.0f, 5.0f, new Color(18, 24, 34, 255));
        RoundedUtils.drawRoundOutline(avatarX - 1.0f, avatarY - 1.0f, avatarSize + 2.0f, avatarSize + 2.0f, 5.0f, 0.8f, new Color(0, 0, 0, 0), BORDER_WINDOW);

        // render rasterized widgets svg icon
        ResourceLocation avatarLoc = getAvatarTexture();
        if (avatarLoc != null) {
            RenderUtils.drawImage(avatarLoc, avatarX, avatarY, (int) avatarSize, (int) avatarSize);
        }

        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        Fonts.urbanist.get(15.0f).drawString(username, avatarX + 26.0f, avatarY + 2.0f, Color.white.getRGB());
        RenderUtils.drawCircle(avatarX + 27.5f, avatarY + 16.5f, 0, 360, 2.0f, 0.1f, true, STATUS_GREEN.getRGB());
        Fonts.urbanist.get(13.0f).drawString("Online", avatarX + 33.0f, avatarY + 12.5f, TEXT_MUTED.getRGB());

        // ---- content area (single scissored draw pass with mouse coords) ----
        if (selectedSearchCategory != null) {
            selectedSearchCategory.drawScreen(mouseX, mouseY);
        } else if (selectedConfigCategory != null) {
            selectedConfigCategory.drawScreen(mouseX, mouseY);
        } else if (selectedCategory != null) {
            selectedCategory.setSelected(true);
            selectedCategory.drawScreen(mouseX, mouseY);
        }

        // ---- top bar strip ----
        float stripX = posX + SIDEBAR_WIDTH + 1.0f;
        float stripW = width - SIDEBAR_WIDTH - 2.0f;
        // subtle hairline divider below top bar
        RenderUtils.drawRect(stripX, posY + TOP_BAR_HEIGHT, stripW, 1.0f, new Color(38, 48, 68, 140).getRGB());

        // save button with brief "saved" feedback
        float[] save = saveButtonRect();
        boolean saveHover = MouseUtils.isHovered(save[0], save[1], save[2], save[3], mouseX, mouseY);
        boolean justSaved = System.currentTimeMillis() - lastSaveTime < 1200L;
        drawPill(justSaved ? "Saved" : "Save", save[0], save[1], save[2], save[3],
                saveHover || justSaved, justSaved ? STATUS_GREEN : null);

        // about + search buttons
        float[] about = aboutButtonRect();
        drawPill("About", about[0], about[1], about[2], about[3], showAboutModal, showAboutModal ? ACCENT_CYAN : null);

        float[] search = searchButtonRect();
        drawPill("Search", search[0], search[1], search[2], search[3],
                selectedSearchCategory != null, selectedSearchCategory != null ? ACCENT_CYAN : null);

        // ---- about dialog ----
        if (showAboutModal) {
            drawAboutModal();
        }

        RenderUtils.scaleEnd();
    }

    /**
     * text pill button: rounded outline + horizontally and vertically centered label.
     * pass a non-null accent to force the label color (active state).
     */
    private void drawPill(String label, float x, float y, float w, float h, boolean hovered, Color accent) {
        Color fill = accent != null ? PILL_ACTIVE_BG : (hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        Color border = accent != null ? PILL_ACTIVE_BORDER : (hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER);
        RoundedUtils.drawRoundOutline(x, y, w, h, 5.0f, 0.8f, fill, border);
        Color textColor = accent != null ? accent : (hovered ? Color.white : TEXT_PRIMARY);
        float textY = y + (h - Fonts.interBold.get(13.0f).getHeight()) / 2.0f + 0.5f;
        Fonts.interBold.get(13.0f).drawCenteredString(label, x + w / 2.0f, textY, textColor.getRGB());
    }

    private void drawAboutModal() {
        // dim the window behind the dialog
        RoundedUtils.drawRound(posX, posY, width, height, 10.0f, new Color(0, 0, 0, 150));

        float modalW = 200.0f;
        float modalH = 170.0f;
        float modalX = posX + (width - modalW) / 2.0f;
        float modalY = posY + (height - modalH) / 2.0f;

        RoundedUtils.drawRound(modalX - 2.0f, modalY - 2.0f, modalW + 4.0f, modalH + 4.0f, 10.0f, new Color(0, 0, 0, 100));
        RoundedUtils.drawRoundOutline(modalX, modalY, modalW, modalH, 8.0f, 0.8f, new Color(13, 17, 24, 255), BORDER_WINDOW);

        Fonts.urbanist.get(13.0f).drawString("ABOUT", modalX + 16.0f, modalY + 14.0f, TEXT_MUTED.getRGB());
        Fonts.urbanist.get(18.0f).drawCenteredString(CLIENT_NAME, modalX + modalW / 2.0f, modalY + 30.0f, Color.white.getRGB());

        RenderUtils.drawRect(modalX + 16.0f, modalY + 54.0f, modalW - 32.0f, 1.0f, DIVIDER.getRGB());

        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        String[][] rows = {
                {"Version", CLIENT_VERSION},
                {"Build", CLIENT_BUILD},
                {"Date", CLIENT_BUILD_DATE},
                {"User", username}
        };

        float rowY = modalY + 64.0f;
        for (String[] row : rows) {
            Fonts.urbanist.get(14.0f).drawString(row[0], modalX + 16.0f, rowY, TEXT_MUTED.getRGB());
            Fonts.urbanist.get(14.0f).drawString(row[1], modalX + 88.0f, rowY, TEXT_PRIMARY.getRGB());
            rowY += 15.0f;
        }

        Fonts.urbanist.get(12.0f).drawCenteredString("\u00A9 2026 Demise", modalX + modalW / 2.0f, modalY + modalH - 16.0f, TEXT_FAINT.getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            forwardClick(mouseX, mouseY, mouseButton);
            return;
        }

        // while the about dialog is open it captures all clicks
        if (showAboutModal) {
            float[] about = aboutButtonRect();
            if (MouseUtils.isHovered(about[0], about[1], about[2], about[3], mouseX, mouseY)) {
                showAboutModal = false;
                return;
            }
            float modalW = 200.0f, modalH = 170.0f;
            float modalX = posX + (width - modalW) / 2.0f;
            float modalY = posY + (height - modalH) / 2.0f;
            if (!MouseUtils.isHovered(modalX, modalY, modalW, modalH, mouseX, mouseY)) {
                showAboutModal = false;
            }
            return;
        }

        // save button
        float[] save = saveButtonRect();
        if (MouseUtils.isHovered(save[0], save[1], save[2], save[3], mouseX, mouseY)) {
            Demise.INSTANCE.getConfigManager().saveConfigs();
            lastSaveTime = System.currentTimeMillis();
            return;
        }

        // about button
        float[] about = aboutButtonRect();
        if (MouseUtils.isHovered(about[0], about[1], about[2], about[3], mouseX, mouseY)) {
            showAboutModal = true;
            return;
        }

        // search button
        float[] search = searchButtonRect();
        if (MouseUtils.isHovered(search[0], search[1], search[2], search[3], mouseX, mouseY)) {
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
                selectedCategory.setSelected(true);
            }
            return;
        }

        // drag from the top bar
        if (MouseUtils.isHovered(posX + SIDEBAR_WIDTH, posY, width - SIDEBAR_WIDTH, TOP_BAR_HEIGHT, mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
            return;
        }

        // sidebar navigation
        if (MouseUtils.isHovered(posX, posY, SIDEBAR_WIDTH, height, mouseX, mouseY)) {
            float itemY = posY + NAV_START;
            for (NavItem item : navItems) {
                float itemX = posX + 8.0f;
                float itemW = SIDEBAR_WIDTH - 16.0f;
                if (MouseUtils.isHovered(itemX, itemY, itemW, NAV_ITEM_HEIGHT, mouseX, mouseY)) {
                    if (item.isConfig) {
                        if (selectedConfigCategory == null) {
                            configCategoryComponent.initCategory();
                        }
                        selectedConfigCategory = configCategoryComponent;
                        selectedCategory = null;
                        selectedSearchCategory = null;
                    } else {
                        Category target = getCategoryByModuleCategory(item.category);
                        if (selectedCategory != target) {
                            target.initCategory();
                        }
                        selectedCategory = target;
                        selectedCategory.setSelected(true);
                        selectedConfigCategory = null;
                        selectedSearchCategory = null;
                    }
                    return;
                }
                itemY += NAV_STEP;
            }
            return;
        }

        forwardClick(mouseX, mouseY, mouseButton);
    }

    private void forwardClick(int mouseX, int mouseY, int mouseButton) {
        if (selectedSearchCategory != null) {
            selectedSearchCategory.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (selectedConfigCategory != null) {
            selectedConfigCategory.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (selectedCategory != null) {
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
                }
                selectedSearchCategory = null;
                searchCategoryComponent.setInputting(false);
                selectedCategory = (previousCategory != null ? previousCategory : categories.get(0));
                selectedCategory.setSelected(true);
                return;
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
    public boolean doesGuiPauseGame() {
        return false;
    }
}