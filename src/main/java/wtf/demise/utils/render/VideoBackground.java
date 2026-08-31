package wtf.demise.utils.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.utils.InstanceAccess;

public class VideoBackground implements InstanceAccess {
    private static final int TOTAL_FRAMES = 360;
    private static final double FPS = 24.0;
    private static final double FRAME_DURATION_MS = 1000.0 / FPS;
    private static final ResourceLocation[] FRAME_LOCATIONS = new ResourceLocation[TOTAL_FRAMES];

    static {
        // preallocate resource locations to avoid runtime string allocation and gc pressure
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            FRAME_LOCATIONS[i] = new ResourceLocation(String.format("demise/video/frame_%03d.jpg", i + 1));
        }
    }

    public static void draw() {
        if (mc == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();

        // calculate current looping frame index based on system time elapsed since client startup
        long elapsedMs = System.currentTimeMillis() - Demise.INSTANCE.getStartTimeLong();
        int frameIndex = (int) ((elapsedMs / FRAME_DURATION_MS) % TOTAL_FRAMES);
        if (frameIndex < 0) frameIndex = 0;

        ResourceLocation currentFrame = FRAME_LOCATIONS[frameIndex];

        // compute aspect ratio cover scaling so 16:9 video fills entire viewport without distortion
        float videoAspect = 16.0f / 9.0f;
        float screenAspect = screenWidth / screenHeight;

        float renderWidth = screenWidth;
        float renderHeight = screenHeight;
        float offsetX = 0.0f;
        float offsetY = 0.0f;

        if (screenAspect > videoAspect) {
            // screen is wider than 16:9 - match width and crop top/bottom
            renderHeight = screenWidth / videoAspect;
            offsetY = (screenHeight - renderHeight) / 2.0f;
        } else {
            // screen is taller than 16:9 - match height and crop left/right
            renderWidth = screenHeight * videoAspect;
            offsetX = (screenWidth - renderWidth) / 2.0f;
        }

        // setup opengl state for full screen texture blitting
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        mc.getTextureManager().bindTexture(currentFrame);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(offsetX, offsetY + renderHeight, 0.0).tex(0.0, 1.0).endVertex();
        worldrenderer.pos(offsetX + renderWidth, offsetY + renderHeight, 0.0).tex(1.0, 1.0).endVertex();
        worldrenderer.pos(offsetX + renderWidth, offsetY, 0.0).tex(1.0, 0.0).endVertex();
        worldrenderer.pos(offsetX, offsetY, 0.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
    }
}
