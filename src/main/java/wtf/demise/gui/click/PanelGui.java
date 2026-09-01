package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.components.Category;
import wtf.demise.gui.click.components.SearchCategory;
import wtf.demise.gui.click.components.config.ConfigCategoryComponent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PanelGui extends GuiScreen {
    private final List<Category> categories = new ArrayList<>();
    public static Category selectedCategory;
    public static ConfigCategoryComponent selectedConfigCategory;
    public static SearchCategory selectedSearchCategory;
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = -1, posY = -1;
    public static float width = 500, height = 330;
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    public static float interpolatedScale;
    private boolean closing;
    private boolean initializedPosition;

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

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        width = Math.min(520.0f, Math.max(380.0f, screenW - 40.0f));
        height = Math.min(340.0f, Math.max(260.0f, screenH - 40.0f));

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
        interpolatedScale = MathUtils.interpolate(interpolatedScale, !closing ? 1.0f : 0.0f, 0.25f);

        if (interpolatedScale < 0.01f && closing) {
            mc.displayGuiScreen(null);
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        width = Math.min(520.0f, Math.max(380.0f, screenW - 40.0f));
        height = Math.min(340.0f, Math.max(260.0f, screenH - 40.0f));

        if (dragging) {
            posX = MathHelper.clamp_float(mouseX - dragX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(mouseY - dragY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        } else {
            posX = MathHelper.clamp_float(posX, 10.0f, Math.max(10.0f, screenW - width - 10.0f));
            posY = MathHelper.clamp_float(posY, 10.0f, Math.max(10.0f, screenH - height - 10.0f));
        }

        RenderUtils.scaleStart(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, interpolatedScale);

        Color mainBg = new Color(20, 22, 26, 235);
        RoundedUtils.drawRound(posX, posY, width, height, 10.0f, mainBg);

        Color headerBg = new Color(26, 29, 35, 255);
        RoundedUtils.drawRound(posX, posY, width, 38.0f, 10.0f, headerBg);
        RenderUtils.drawRect(posX, posY + 28.0f, width, 10.0f, headerBg.getRGB());

        RenderUtils.drawRect(posX, posY + 38.0f, width, 1.0f, new Color(40, 44, 52, 180).getRGB());

        Fonts.interBold.get(18).drawString(Demise.INSTANCE.getClientName().toLowerCase(), posX + 12.0f, posY + 13.0f, new Color(255, 255, 255, 240).getRGB());
        Fonts.interMedium.get(11).drawString("v" + Demise.INSTANCE.getVersion(), posX + 14.0f + Fonts.interBold.get(18).getStringWidth(Demise.INSTANCE.getClientName().toLowerCase()), posY + 16.0f, new Color(140, 145, 155, 200).getRGB());

        float tabX = posX + 95.0f;
        float tabY = posY + 9.0f;

        for (Category category : categories) {
            String name = category.getCategory().getName();
            float textWidth = Fonts.interMedium.get(13).getStringWidth(name);
            float tabWidth = textWidth + 14.0f;
            float tabHeight = 20.0f;

            boolean hovered = MouseUtils.isHovered(tabX, tabY, tabWidth, tabHeight, mouseX, mouseY);
            boolean selected = (selectedCategory == category && selectedConfigCategory == null && selectedSearchCategory == null);

            category.setX(tabX);
            category.setY(tabY);
            category.setHovered(hovered);
            category.setSelected(selected);

            if (selected) {
                RoundedUtils.drawRound(tabX, tabY, tabWidth, tabHeight, 5.0f, new Color(255, 255, 255, 35));
                Fonts.interMedium.get(13).drawString(name, tabX + 7.0f, tabY + 6.0f, new Color(255, 255, 255, 255).getRGB());
            } else if (hovered) {
                RoundedUtils.drawRound(tabX, tabY, tabWidth, tabHeight, 5.0f, new Color(255, 255, 255, 15));
                Fonts.interMedium.get(13).drawString(name, tabX + 7.0f, tabY + 6.0f, new Color(220, 225, 235, 220).getRGB());
            } else {
                Fonts.interMedium.get(13).drawString(name, tabX + 7.0f, tabY + 6.0f, new Color(150, 155, 165, 200).getRGB());
            }

            tabX += tabWidth + 4.0f;
        }

        float configTextWidth = Fonts.interMedium.get(13).getStringWidth("Configs");
        float configTabWidth = configTextWidth + 14.0f;
        boolean configHovered = MouseUtils.isHovered(tabX, tabY, configTabWidth, 20.0f, mouseX, mouseY);
        boolean configSelected = (selectedConfigCategory != null);

        configCategoryComponent.setX(tabX);
        configCategoryComponent.setY(tabY);
        configCategoryComponent.setHovered(configHovered);
        configCategoryComponent.setSelected(configSelected);

        if (configSelected) {
            RoundedUtils.drawRound(tabX, tabY, configTabWidth, 20.0f, 5.0f, new Color(255, 255, 255, 35));
            Fonts.interMedium.get(13).drawString("Configs", tabX + 7.0f, tabY + 6.0f, new Color(255, 255, 255, 255).getRGB());
        } else if (configHovered) {
            RoundedUtils.drawRound(tabX, tabY, configTabWidth, 20.0f, 5.0f, new Color(255, 255, 255, 15));
            Fonts.interMedium.get(13).drawString("Configs", tabX + 7.0f, tabY + 6.0f, new Color(220, 225, 235, 220).getRGB());
        } else {
            Fonts.interMedium.get(13).drawString("Configs", tabX + 7.0f, tabY + 6.0f, new Color(150, 155, 165, 200).getRGB());
        }

        float searchW = 100.0f;
        float searchH = 20.0f;
        float searchX = posX + width - searchW - 10.0f;
        float searchY = posY + 9.0f;

        boolean searchHovered = MouseUtils.isHovered(searchX, searchY, searchW, searchH, mouseX, mouseY);
        RoundedUtils.drawRound(searchX, searchY, searchW, searchH, 5.0f, new Color(15, 17, 20, 200));

        if (selectedSearchCategory != null && searchCategoryComponent.isInputting()) {
            String cursor = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
            String query = searchCategoryComponent.getFilter();
            String display = query.isEmpty() ? "Search..." + cursor : query + cursor;
            Fonts.interRegular.get(12).drawString(display, searchX + 7.0f, searchY + 6.0f, Color.white.getRGB());
        } else if (selectedSearchCategory != null && !searchCategoryComponent.getFilter().isEmpty()) {
            Fonts.interRegular.get(12).drawString(searchCategoryComponent.getFilter(), searchX + 7.0f, searchY + 6.0f, Color.white.getRGB());
        } else {
            Fonts.interRegular.get(12).drawString("Search...", searchX + 7.0f, searchY + 6.0f, new Color(130, 135, 145, 180).getRGB());
        }

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

        RenderUtils.drawRect(posX, posY + height - 20.0f, width, 1.0f, new Color(35, 38, 45, 160).getRGB());
        String modCount = "Modules: " + Demise.INSTANCE.getModuleManager().getEnabledModules().size() + "/" + Demise.INSTANCE.getModuleManager().getAllModules().size();
        Fonts.interRegular.get(11).drawString(modCount, posX + width - Fonts.interRegular.get(11).getStringWidth(modCount) - 10.0f, posY + height - 14.0f, new Color(150, 155, 165, 190).getRGB());
        Fonts.interRegular.get(11).drawString(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), posX + 10.0f, posY + height - 14.0f, new Color(150, 155, 165, 190).getRGB());

        RenderUtils.scaleEnd();
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
        if (mouseButton == 0 && MouseUtils.isHovered(posX, posY, width, 38.0f, mouseX, mouseY)) {
            float tabX = posX + 95.0f;
            float tabY = posY + 9.0f;
            boolean clickedTab = false;

            for (Category category : categories) {
                float textWidth = Fonts.interMedium.get(13).getStringWidth(category.getCategory().getName());
                float tabWidth = textWidth + 14.0f;
                if (MouseUtils.isHovered(tabX, tabY, tabWidth, 20.0f, mouseX, mouseY)) {
                    if (selectedCategory != category) {
                        category.initCategory();
                    }
                    selectedCategory = category;
                    selectedConfigCategory = null;
                    selectedSearchCategory = null;
                    clickedTab = true;
                    break;
                }
                tabX += tabWidth + 4.0f;
            }

            if (!clickedTab) {
                float configTextWidth = Fonts.interMedium.get(13).getStringWidth("Configs");
                float configTabWidth = configTextWidth + 14.0f;
                if (MouseUtils.isHovered(tabX, tabY, configTabWidth, 20.0f, mouseX, mouseY)) {
                    if (selectedConfigCategory == null) {
                        configCategoryComponent.initCategory();
                    }
                    selectedConfigCategory = configCategoryComponent;
                    selectedCategory = null;
                    selectedSearchCategory = null;
                    clickedTab = true;
                }
            }

            float searchW = 100.0f;
            float searchX = posX + width - searchW - 10.0f;
            if (MouseUtils.isHovered(searchX, tabY, searchW, 20.0f, mouseX, mouseY)) {
                if (selectedSearchCategory == null) {
                    searchCategoryComponent.initCategory();
                }
                selectedSearchCategory = searchCategoryComponent;
                selectedCategory = null;
                selectedConfigCategory = null;
                searchCategoryComponent.setInputting(true);
                clickedTab = true;
            } else if (selectedSearchCategory != null) {
                searchCategoryComponent.setInputting(false);
            }

            if (!clickedTab) {
                dragging = true;
                dragX = mouseX - posX;
                dragY = mouseY - posY;
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
            closing = true;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            selectedCategory = categories.get((categories.indexOf(selectedCategory) + 1) % categories.size());
            selectedConfigCategory = null;
            selectedSearchCategory = null;
        }

        if (closing) {
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
    public boolean doesGuiPauseGame() {
        return false;
    }
}