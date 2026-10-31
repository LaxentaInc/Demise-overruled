package wtf.demise.gui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.render.RenderUtils;
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

        String buttonStyle = Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get();
        boolean isCustom = "Custom".equalsIgnoreCase(buttonStyle);

        float buttonWidth = isCustom ? 160.0f : 200.0f;
        float buttonHeight = isCustom ? 24.0f : 20.0f;
        float buttonSpacing = 6.0f;

        float totalButtonsHeight = buttons.size() * buttonHeight + (buttons.size() - 1) * buttonSpacing;
        float centerY = (height / 2.0f);
        float buttonsStartY = centerY - totalButtonsHeight / 2.0f + 25.0f;

        Shaders shadersModule = Demise.INSTANCE.getModuleManager().getModule(Shaders.class);

        // optional background blur pass when enabled in shaders module
        if (shadersModule != null && shadersModule.isEnabled() && shadersModule.blur.get()) {
            MenuButton.shader = true;
            Blur.startBlur();
            renderButtons(buttonWidth, buttonHeight, buttonsStartY, buttonSpacing, mouseX, mouseY);
            Blur.endBlur(25, 1);
        }

        // optional bloom shadow pass when enabled in shaders module
        if (shadersModule != null && shadersModule.isEnabled() && shadersModule.shadow.get()) {
            MenuButton.shader = true;
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            renderButtons(buttonWidth, buttonHeight, buttonsStartY, buttonSpacing, mouseX, mouseY);
            stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 25, 1);
        }

        MenuButton.shader = false;

        // brand title lockup with drop shadow for contrast over white background sections
        String titleText = funny ? "DIMAISE" : "DEMISE";
        float titleCenterY = buttonsStartY - 56.0f;
        float titleCenterX = width / 2.0f;

        // shadow pass
        Fonts.urbanist.get(40.0f).drawCenteredString(titleText, titleCenterX + 1.0f, titleCenterY + 1.0f, new Color(0, 0, 0, 160).getRGB());
        Fonts.bilbo.get(34.0f).drawCenteredString("overruled", titleCenterX + 1.0f, titleCenterY + 31.0f, new Color(0, 0, 0, 160).getRGB());

        // foreground text pass
        Fonts.urbanist.get(40.0f).drawCenteredString(titleText, titleCenterX, titleCenterY, Color.white.getRGB());
        Fonts.bilbo.get(34.0f).drawCenteredString("overruled", titleCenterX, titleCenterY + 30.0f, new Color(56, 189, 248, 255).getRGB());

        // render the clean floating navigation buttons
        renderButtons(buttonWidth, buttonHeight, buttonsStartY, buttonSpacing, mouseX, mouseY);

        // top left discreet watermark
        Fonts.urbanist.get(13.0f).drawString("demise by laxenta LLC", 16.0f, 14.0f, new Color(255, 255, 255, 190).getRGB());

        // debug fps counter in top right
        String fpsText = Minecraft.getDebugFPS() + " fps";
        Fonts.urbanist.get(12.5f).drawString(fpsText, width - Fonts.urbanist.get(12.5f).getStringWidth(fpsText) - 16.0f, 14.0f, new Color(255, 255, 255, 170).getRGB());

        // smooth fade in overlay on screen entrance
        if (fade) {
            RenderUtils.drawRect(0, 0, width, height, new Color(0, 0, 0, Math.max(0, Math.min(255, alpha))).getRGB());
            alpha -= 4;
            if (alpha < 0) {
                fade = false;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderButtons(float buttonWidth, float buttonHeight, float startY, float buttonSpacing, int mouseX, int mouseY) {
        String buttonStyle = Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get();
        boolean isCustom = "Custom".equalsIgnoreCase(buttonStyle);
        float btnX = width / 2.0f - buttonWidth / 2.0f;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            button.x = btnX;
            button.y = startY + i * (buttonHeight + (isCustom ? buttonSpacing : 4.0f));
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.radius = isCustom ? 6.0f : 0.0f;
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