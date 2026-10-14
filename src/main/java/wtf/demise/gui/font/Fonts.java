package wtf.demise.gui.font;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.floats.Float2ObjectArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import wtf.demise.Demise;

import java.awt.*;
import java.io.InputStream;

public enum Fonts {
    interBold("inter/Inter_Bold"),
    interMedium("inter/Inter_Medium"),
    interRegular("inter/Inter_Regular"),
    interSemiBold("inter/Inter_SemiBold"),
    nursultan("others/Nursultan"),
    urbanist("urbanist/Urbanist"),
    // geometric outfit fonts used for thin headers, descriptions, and sleek micro-typography
    outfitThin("outfit/Outfit-Thin"),
    outfitRegular("outfit/Outfit-Regular"),
    outfitMedium("outfit/Outfit-Medium");

    private final String file;
    private final Float2ObjectMap<FontRenderer> fontMap = new Float2ObjectArrayMap<>();

    public FontRenderer get(float size) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, true);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer get(float size, boolean antiAlias) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, antiAlias);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer create(String file, float size, boolean antiAlias) {
        Font font = null;

        try {
            // resolve font binary from client assets directory with fallback protection
            String path = "/assets/minecraft/" + Demise.INSTANCE.getClientName().toLowerCase() + "/font/" + file + ".ttf";
            InputStream in = Demise.class.getResourceAsStream(path);
            if (in == null) {
                in = Demise.class.getResourceAsStream("/assets/minecraft/" + Demise.INSTANCE.getClientName().toLowerCase() + "/font/inter/Inter_Regular.ttf");
            }
            if (in != null) {
                font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, size);
            }
        } catch (Exception ex) {
            // fallback to standard system sans-serif font if font rasterizer encounters an io anomaly
            font = new Font("SansSerif", Font.PLAIN, (int) size);
        }
        if (font != null) {
            return new FontRenderer(font, antiAlias);
        } else {
            throw new RuntimeException("Failed to create font: " + file);
        }
    }

    Fonts(String file) {
        this.file = file;
    }
}
