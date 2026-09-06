package wtf.demise.gui.widget.impl;

import wtf.demise.Demise;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleListWidget extends Widget {
    private static final int xPadding = 7;
    private static final int yPadding = 5;
    public static float currX;
    public static float currY;

    public ModuleListWidget() {
        super("ModuleList");
        this.x = 0.89062506f;
        this.y = 0.009259259f;
    }

    @Override
    public void render() {
        if (!shouldRender()) return;

        List<Module> enabledModules = getEnabledModules();
        int maxW = 0;
        for (Module m : enabledModules) {
            int w = getModuleWidth(m);
            if (w > maxW) maxW = w;
        }
        this.width = Math.max(60, maxW);
        this.height = enabledModules.size() * getModuleHeight();
        clampToBounds();

        currX = renderX;
        currY = renderY;

        int middle = sr.getScaledWidth() / 2;
        float offset = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            int width = getModuleWidth(module);
            int height = getModuleHeight();

            renderModule(module, renderX, renderY, offset, width, height, middle, i, false, false);
            offset += height;
        }
    }

    @Override
    public void onShader(ShaderEvent event) {
        if (!shouldRender()) return;

        List<Module> enabledModules = getEnabledModules();
        int maxW = 0;
        for (Module m : enabledModules) {
            int w = getModuleWidth(m);
            if (w > maxW) maxW = w;
        }
        this.width = Math.max(60, maxW);
        this.height = enabledModules.size() * getModuleHeight();
        clampToBounds();

        int middle = sr.getScaledWidth() / 2;
        float offset = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            int width = getModuleWidth(module);
            int height = getModuleHeight();

            renderModule(module, renderX, renderY, offset, width, height, middle, i, true, event.getShaderType() == ShaderEvent.ShaderType.GLOW);
            offset += height;
        }
    }

    public static List<Module> getEnabledModules() {
        // retrieves currently active modules sorted by rendered text width for clean descending layout
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : INSTANCE.getModuleManager().getModules()) {
            if (module.isHidden() || (setting.hideRender.get() && Demise.INSTANCE.getModuleManager().getModulesByCategory().get(ModuleCategory.Visual).contains(module))) {
                continue;
            }
            if (!module.isEnabled()) continue;
            enabledModules.add(module);
        }
        enabledModules.sort(Comparator.comparing(ModuleListWidget::getModuleWidth).reversed());
        return enabledModules;
    }

    private static int getModuleWidth(Module module) {
        return setting.getFr().getStringWidth(module.getName() + module.getTag()) + xPadding;
    }

    public static int getModuleHeight() {
        return setting.getFr().getHeight() + yPadding;
    }

    private void renderModule(Module module, float localX, float localY, float offset, int width, int height, int middle, int index, boolean shader, boolean isGlow) {
        renderBackground(localX, localY, offset, width, height, middle, shader, isGlow);
        renderText(module, localX, localY, offset, width, middle, index, shader);
    }

    private void renderBackground(float localX, float localY, float offset, int width, int height, int middle, boolean shader, boolean isGlow) {
        // computes horizontal anchor based on which half of the screen the widget occupies
        float rectX = localX < middle ? localX : (localX + this.width - width);
        float rectY = localY + offset;

        if (!shader) {
            RenderUtils.drawRect(rectX, rectY, width, height, setting.bgColor());
        } else {
            if (!isGlow) {
                RenderUtils.drawRect(rectX, rectY, width, height, Color.black.getRGB());
            } else {
                int color = Demise.INSTANCE.getModuleManager().getModule(Shaders.class).syncColor.get() ? setting.color((int) localY) : Demise.INSTANCE.getModuleManager().getModule(Shaders.class).bloomColor.get().getRGB();
                RenderUtils.drawRect(rectX, rectY, width, height, color);
            }
        }
    }

    private void renderText(Module module, float localX, float localY, float offset, int width, int middle, int index, boolean shader) {
        String text = module.getName() + module.getTag();
        int color = setting.color(index);
        float textY = localY + offset + 3 + (yPadding / 2f);
        // aligns text with padding inside the rendered rectangular segment
        float textX = localX < middle ? (localX + (xPadding / 2f)) : (localX + this.width - width + (xPadding / 2f));

        if (!shader) {
            setting.getFr().drawString(text, textX, textY, color);
        } else {
            setting.getFr().drawString(text, textX, textY, Color.black.getRGB());
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Module list");
    }
}