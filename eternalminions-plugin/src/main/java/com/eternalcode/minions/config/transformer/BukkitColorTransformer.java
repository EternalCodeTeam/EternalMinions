package com.eternalcode.minions.config.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import java.util.Locale;
import org.bukkit.Color;

public final class BukkitColorTransformer extends BidirectionalTransformer<String, Color> {

    private static final String FORMAT_ERROR = "Color must use the #RRGGBB format: ";

    @Override
    public GenericsPair<String, Color> getPair() {
        return this.genericsPair(String.class, Color.class);
    }

    @Override
    public Color leftToRight(String value, SerdesContext context) {
        if (!value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(FORMAT_ERROR + value);
        }

        return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
    }

    @Override
    public String rightToLeft(Color value, SerdesContext context) {
        return String.format(Locale.ROOT, "#%06X", value.asRGB());
    }
}
