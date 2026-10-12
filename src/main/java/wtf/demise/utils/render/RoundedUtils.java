package wtf.demise.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.render.shader.ShaderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtils implements InstanceAccess {
    public static ShaderUtils roundedShader;
    public static ShaderUtils roundedOutlineShader;
    private static ShaderUtils roundedGradientShader;

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, color);
    }

    public static void drawGradientPreset(float x, float y, float width, float height, float radius) {
        Shaders shaders = Demise.INSTANCE.getModuleManager().getModule(Shaders.class);

        if (shaders != null && shaders.syncColor.get()) {
            Interface anInterface = Demise.INSTANCE.getModuleManager().getModule(Interface.class);
            if (anInterface != null) {
                drawGradientRound(x, y, width, height, radius,
                        new Color(anInterface.color((int) x)),
                        new Color(anInterface.color((int) (x + height))),
                        new Color(anInterface.color((int) (x + width))),
                        new Color(anInterface.color((int) (x + width + height))));
                return;
            }
        }
        Color fallbackColor = (shaders != null && shaders.bloomColor != null) ? shaders.bloomColor.get() : Color.cyan;
        drawGradientRound(x, y, width, height, radius, fallbackColor, fallbackColor, fallbackColor, fallbackColor);
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        if (width <= 0 || height <= 0) return;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        glShadeModel(GL_SMOOTH);

        radius = Math.max(0, Math.min(radius, Math.min(width / 2.0f, height / 2.0f)));

        if (radius <= 0.5f) {
            glBegin(GL_QUADS);
            glColor4f(topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
            glVertex2f(x, y);
            glColor4f(bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
            glVertex2f(x, y + height);
            glColor4f(bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);
            glVertex2f(x + width, y + height);
            glColor4f(topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
            glVertex2f(x + width, y);
            glEnd();
        } else {
            // direct native 2d tessellated gradient with smooth corner vertices
            glBegin(GL_POLYGON);
            glColor4f(topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
            for (int i = 180; i <= 270; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
            glColor4f(topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
            for (int i = 270; i <= 360; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + width - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
            glColor4f(bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);
            for (int i = 0; i <= 90; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + width - radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
            glColor4f(bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
            for (int i = 90; i <= 180; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
            glEnd();
        }

        glShadeModel(GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        RenderUtils.resetColor();
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        if (width <= 0 || height <= 0 || color == null || color.getAlpha() <= 0) return;

        radius = Math.max(0, Math.min(radius, Math.min(width / 2.0f, height / 2.0f)));

        if (radius <= 0.5f) {
            RenderUtils.drawRect(x, y, width, height, color.getRGB());
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderUtils.color(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0, color.getAlpha() / 255.0);

        // native opengl polygon drawing for rounded rectangle geometry
        glBegin(GL_POLYGON);
        for (int i = 180; i <= 270; i += 15) {
            double rad = Math.toRadians(i);
            glVertex2d(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 15) {
            double rad = Math.toRadians(i);
            glVertex2d(x + width - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
        }
        for (int i = 0; i <= 90; i += 15) {
            double rad = Math.toRadians(i);
            glVertex2d(x + width - radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 15) {
            double rad = Math.toRadians(i);
            glVertex2d(x + radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
        }
        glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        RenderUtils.resetColor();
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        drawRound(x, y, width, height, radius, blur, color);
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        if (width <= 0 || height <= 0) return;

        // render inner background quad
        drawRound(x, y, width, height, radius, color);

        if (outlineThickness <= 0 || outlineColor == null || outlineColor.getAlpha() <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width / 2.0f, height / 2.0f)));

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(Math.max(1.0f, outlineThickness));
        RenderUtils.color(outlineColor.getRed() / 255.0, outlineColor.getGreen() / 255.0, outlineColor.getBlue() / 255.0, outlineColor.getAlpha() / 255.0);

        if (radius <= 0.5f) {
            glBegin(GL_LINE_LOOP);
            glVertex2d(x, y);
            glVertex2d(x + width, y);
            glVertex2d(x + width, y + height);
            glVertex2d(x, y + height);
            glEnd();
        } else {
            glBegin(GL_LINE_LOOP);
            for (int i = 180; i <= 270; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
            for (int i = 270; i <= 360; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + width - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
            for (int i = 0; i <= 90; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + width - radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
            for (int i = 90; i <= 180; i += 15) {
                double rad = Math.toRadians(i);
                glVertex2d(x + radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
            glEnd();
        }

        GL11.glLineWidth(1.0f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        RenderUtils.resetColor();
    }
}
