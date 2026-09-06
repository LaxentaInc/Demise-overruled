package wtf.demise.gui.widget.impl;

import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;

public class PotionHUDWidget extends Widget {
    public PotionHUDWidget() {
        super("Potion HUD");
        this.x = 0.01f;
        this.y = 0.14f;
    }

    @Override
    public void render() {
        ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());

        this.width = 96;
        this.height = Math.max(22, 22 + potions.size() * 11);

        // prevent collision with the top left client watermark
        if (renderX < 140 && renderY < 48) {
            renderY = 48;
        }
        clampToBounds();

        RoundedUtils.drawRound(renderX, renderY, width, height, 4, new Color(setting.bgColor(), true));

        Fonts.interSemiBold.get(12).drawString("Potions", renderX + 8, renderY + 6, -1);
        Fonts.nursultan.get(13).drawString("E", renderX + width - 15, renderY + 7, setting.color(0));

        float offset = renderY + 18;

        for (PotionEffect potion : potions) {
            String name = I18n.format(Potion.potionTypes[potion.getPotionID()].getName()) + " " + (potion.getAmplifier() > 0 ? I18n.format("enchantment.level." + (potion.getAmplifier() + 1)) : "");
            String duration = Potion.getDurationString(potion);

            Fonts.interRegular.get(11).drawStringWithShadow(name, renderX + 8, offset, -1);
            Fonts.interRegular.get(11).drawStringWithShadow(duration, renderX + width - 8 - Fonts.interRegular.get(11).getStringWidth(duration), offset, -1);

            offset += 11;
        }
    }

    @Override
    public void onShader(ShaderEvent event) {
        ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
        this.width = 96;
        this.height = Math.max(22, 22 + potions.size() * 11);

        // prevent collision with top left client watermark in shader passes
        if (renderX < 140 && renderY < 48) {
            renderY = 48;
        }
        clampToBounds();

        if (event.getShaderType() != ShaderEvent.ShaderType.GLOW) {
            RoundedUtils.drawShaderRound(renderX, renderY, width, height, 4, Color.black);
        } else {
            RoundedUtils.drawGradientPreset(renderX, renderY, width, height, 4);
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Potion HUD");
    }
}