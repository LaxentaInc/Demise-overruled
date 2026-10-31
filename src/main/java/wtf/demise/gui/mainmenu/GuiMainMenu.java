package wtf.demise.gui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.MainMenu;
import wtf.demise.utils.render.shader.impl.Shadow;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static wtf.demise.features.modules.impl.visual.Shaders.stencilFramebuffer;

public class GuiMainMenu extends GuiScreen {
    private final List<MenuButton> buttons = List.of(
            new MenuButton("Singleplayer"),
            new MenuButton("Multiplayer"),
            new MenuButton("Alt manager"),
            new MenuButton("Options"),
            new MenuButton("Quit")
    );

    public static boolean fade = true;
    private int alpha = 255;
    private boolean funny;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void initGui() {
        buttons.forEach(MenuButton::initGui);
        funny = Math.random() > 0.99;
        timer.reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // render live animated wallpaper video background covering the full screen
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        // subtle dark overlay across viewport to balance high contrast video halves
        RenderUtils.drawGradientVertical(0, 0, width, height,
                new Color(8, 12, 18, 95).getRGB(),
                new Color(8, 12, 18, 165).getRGB());

        String buttonStyle = Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get();
        boolean isCustom = "Custom".equalsIgnoreCase(buttonStyle);

        float buttonWidth = isCustom ? 190.0f : 200.0f;
        float buttonHeight = isCustom ? 28.0f : 20.0f;

        // card dimensions for custom glassmorphic layout
        float cardWidth = 240.0f;
        float cardPaddingTop = 18.0f;
        float titleBlockHeight = 56.0f;
        float buttonSpacing = 7.0f;
        float buttonsTotalHeight = buttons.size() * buttonHeight + (buttons.size() - 1) * buttonSpacing;
        float cardHeight = cardPaddingTop + titleBlockHeight + 12.0f + buttonsTotalHeight + 18.0f;

        float cardX = (width - cardWidth) / 2.0f;
        float cardY = (height - cardHeight) / 2.0f;

        Shaders shadersModule = Demise.INSTANCE.getModuleManager().getModule(Shaders.class);

        // optional background blur pass when enabled in shaders module
        if (shadersModule != null && shadersModule.isEnabled() && shadersModule.blur.get()) {
            MenuButton.shader = true;
            Blur.startBlur();
            if (isCustom) {
                RoundedUtils.drawRound(cardX, cardY, cardWidth, cardHeight, 14.0f, Color.black);
            }
            renderButtons(buttonWidth, buttonHeight, cardX, cardY, cardWidth, cardPaddingTop, titleBlockHeight, buttonSpacing, mouseX, mouseY);
            Blur.endBlur(25, 1);
        }

        // optional bloom shadow pass when enabled in shaders module
        if (shadersModule != null && shadersModule.isEnabled() && shadersModule.shadow.get()) {
            MenuButton.shader = true;
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            if (isCustom) {
                RoundedUtils.drawRound(cardX, cardY, cardWidth, cardHeight, 14.0f, Color.black);
            }
            renderButtons(buttonWidth, buttonHeight, cardX, cardY, cardWidth, cardPaddingTop, titleBlockHeight, buttonSpacing, mouseX, mouseY);
            stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 25, 1);
        }

        MenuButton.shader = false;

        if (isCustom) {
            // central frosted glass card backdrop with dark slate glass tint
            RoundedUtils.drawRoundOutline(cardX, cardY, cardWidth, cardHeight, 14.0f, 1.0f,
                    new Color(9, 13, 22, 195), new Color(38, 50, 72, 160));

            // primary brand title using clean modern urbanist typography
            String titleText = funny ? "DIMAISE" : "DEMISE";
            float centerX = cardX + cardWidth / 2.0f;
            Fonts.urbanist.get(30.0f).drawCenteredString(titleText, centerX, cardY + 16.0f, Color.white.getRGB());

            // cursive calligraphy subtitle using bilboregular font
            Fonts.bilbo.get(26.0f).drawCenteredString("overruled", centerX, cardY + 44.0f, new Color(56, 189, 248, 245).getRGB());

            // subtle hairline separator between title block and navigation buttons
            float dividerY = cardY + cardPaddingTop + titleBlockHeight + 4.0f;
            RenderUtils.drawRect(cardX + 22.0f, dividerY, cardWidth - 44.0f, 1.0f, new Color(38, 50, 72, 130).getRGB());
        }

        renderButtons(buttonWidth, buttonHeight, cardX, cardY, cardWidth, cardPaddingTop, titleBlockHeight, buttonSpacing, mouseX, mouseY);

        // bottom left user profile card with avatar and session status
        float profileW = 145.0f;
        float profileH = 32.0f;
        float profileX = 18.0f;
        float profileY = height - profileH - 16.0f;
        RoundedUtils.drawRoundOutline(profileX, profileY, profileW, profileH, 8.0f, 0.8f,
                new Color(9, 13, 22, 210), new Color(38, 50, 72, 160));

        ResourceLocation avatarLoc = PanelGui.getAvatarTexture();
        if (avatarLoc != null) {
            RenderUtils.drawImage(avatarLoc, profileX + 6.0f, profileY + 6.0f, 20, 20);
        }

        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        Fonts.urbanist.get(14.0f).drawString(username, profileX + 32.0f, profileY + 5.5f, Color.white.getRGB());
        RenderUtils.drawCircle(profileX + 34.0f, profileY + 22.5f, 0, 360, 2.0f, 0.1f, true, new Color(76, 217, 100, 255).getRGB());
        Fonts.urbanist.get(11.5f).drawString("Online", profileX + 40.0f, profileY + 19.0f, new Color(148, 163, 184, 255).getRGB());

        // bottom right client build info and fps counter
        float statusW = 140.0f;
        float statusH = 32.0f;
        float statusX = width - statusW - 18.0f;
        float statusY = height - statusH - 16.0f;
        RoundedUtils.drawRoundOutline(statusX, statusY, statusW, statusH, 8.0f, 0.8f,
                new Color(9, 13, 22, 210), new Color(38, 50, 72, 160));

        Fonts.urbanist.get(12.0f).drawString("DEMISE OVERRULED", statusX + 10.0f, statusY + 6.0f, new Color(205, 218, 238, 255).getRGB());
        Fonts.urbanist.get(11.5f).drawString(Minecraft.getDebugFPS() + " FPS", statusX + 10.0f, statusY + 19.0f, new Color(56, 189, 248, 255).getRGB());

        // top left discreet client watermark
        Fonts.urbanist.get(13.0f).drawString("demise by laxenta LLC", 18.0f, 16.0f, new Color(148, 163, 184, 180).getRGB());

        // smooth fade in overlay on screen entrance
        if (fade) {
            RenderUtils.drawRect(0, 0, width, height, new Color(0, 0, 0, Math.max(0, Math.min(255, alpha))).getRGB());
            alpha -= 2;
            if (alpha < 0) {
                fade = false;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderButtons(float buttonWidth, float buttonHeight, float cardX, float cardY,
                               float cardWidth, float cardPaddingTop, float titleBlockHeight,
                               float buttonSpacing, int mouseX, int mouseY) {
        String buttonStyle = Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get();
        boolean isCustom = "Custom".equalsIgnoreCase(buttonStyle);

        float startY = isCustom
                ? cardY + cardPaddingTop + titleBlockHeight + 12.0f
                : (height / 2.0f) + 20.0f - (buttons.size() * buttonHeight) / 2.0f;

        float btnX = isCustom
                ? cardX + (cardWidth - buttonWidth) / 2.0f
                : width / 2.0f - buttonWidth / 2.0f;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            button.x = btnX;
            button.y = startY + i * (buttonHeight + (isCustom ? buttonSpacing : 6.0f));
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.radius = isCustom ? 7.0f : 0.0f;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer" -> mc.displayGuiScreen(new GuiSelectWorld(this));
                    case "Multiplayer" -> mc.displayGuiScreen(new GuiMultiplayer(this));
                    case "Alt manager" -> mc.displayGuiScreen(Demise.INSTANCE.getAltRepositoryGUI());
                    case "Options" -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                    case "Quit" -> mc.shutdown();
                }
            };

            button.drawScreen(mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }
}