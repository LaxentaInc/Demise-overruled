package wtf.demise.gui.button;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class MenuButton implements Button, InstanceAccess {
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    public final String text;
    public float x, y, width, height;
    public float radius;
    public Runnable clickAction;
    private float interpolatedX;
    private float interpolatedY;
    private float interpolatedWidth;
    private float interpolatedHeight;
    public static boolean shader;

    // dark glass palette constants matching client theme
    private static final Color BTN_BG = new Color(13, 18, 28, 215);
    private static final Color BTN_BG_HOVER = new Color(20, 30, 48, 240);
    private static final Color BTN_BORDER = new Color(38, 50, 72, 170);
    private static final Color BTN_BORDER_HOVER = new Color(56, 189, 248, 235);
    private static final Color TEXT_IDLE = new Color(225, 235, 248, 235);
    private static final Color TEXT_HOVER = new Color(255, 255, 255, 255);

    public MenuButton(String text) {
        this.text = text;
    }

    @Override
    public void initGui() {
        interpolatedX = x;
        interpolatedY = y;
        interpolatedWidth = width;
        interpolatedHeight = height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        switch (Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get()) {
            case "Vanilla": {
                FontRenderer fontrenderer = mc.fontRendererObj;
                mc.getTextureManager().bindTexture(buttonTextures);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);

                int i = this.getHoverState(hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.blendFunc(770, 771);
                Gui.drawTexturedModalRect(x, y, 0, 46 + i * 20, width / 2, height);
                Gui.drawTexturedModalRect(x + width / 2, y, 200 - width / 2, 46 + i * 20, width / 2, height);
                int j = 14737632;

                if (hovered) {
                    j = 16777120;
                }

                drawCenteredString(fontrenderer, text, x + width / 2, y + (height - 8) / 2, j);
            }
            break;
            case "Custom": {
                boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);

                if (interpolatedX == 0 || interpolatedY == 0 || interpolatedWidth == 0 || interpolatedHeight == 0) {
                    interpolatedX = x;
                    interpolatedY = y;
                    interpolatedWidth = width;
                    interpolatedHeight = height;
                }

                float interpolation = 0.25f;

                // smooth animated lift on hover without shrinking button bounding box
                interpolatedX = MathUtils.interpolate(interpolatedX, x, interpolation);
                interpolatedY = MathUtils.interpolate(interpolatedY, hovered ? y - 1.0f : y, interpolation);
                interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width, interpolation);
                interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height, interpolation);

                if (!shader) {
                    Color bg = hovered ? BTN_BG_HOVER : BTN_BG;
                    Color border = hovered ? BTN_BORDER_HOVER : BTN_BORDER;
                    RoundedUtils.drawRoundOutline(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, radius, 1.0f, bg, border);

                    float fontY = interpolatedY + (interpolatedHeight - Fonts.urbanist.get(15.0f).getHeight()) / 2.0f + 0.5f;
                    Fonts.urbanist.get(15.0f).drawCenteredString(text, interpolatedX + interpolatedWidth / 2.0f, fontY, (hovered ? TEXT_HOVER : TEXT_IDLE).getRGB());
                } else {
                    RoundedUtils.drawShaderRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, radius, Color.black);
                }
            }
        }
    }

    protected int getHoverState(boolean mouseOver) {
        return mouseOver ? 2 : 1;
    }

    private void drawCenteredString(FontRenderer fontRendererIn, String text, float x, float y, int color) {
        fontRendererIn.drawStringWithShadow(text, x - fontRendererIn.getStringWidth(text) / 2, y, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        if (hovered) clickAction.run();
    }
}