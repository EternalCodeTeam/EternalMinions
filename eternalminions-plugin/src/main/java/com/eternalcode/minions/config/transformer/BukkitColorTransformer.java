package com.eternalcode.minions.config.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import java.util.Locale;
import org.bukkit.Color;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public final class BukkitColorTransformer extends BidirectionalTransformer<String, Color> {

    private static final String FORMAT_ERROR = "Color must use the #RRGGBB format: ";

    @Override
    public GenericsPair<String, Color> getPair() {
        return this.genericsPair(String.class, Color.class);
    }

    @Override
    public @NonNull Color leftToRight(@NonNull String value, @NonNull SerdesContext context) {
        if (!value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(FORMAT_ERROR + value);
        }

        return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
    }

    @Contract("null, _ -> fail")
    @Override
    public @NonNull String rightToLeft(@NonNull Color value, @NonNull SerdesContext context) {
        return String.format(Locale.ROOT, "#%06X", value.asRGB());
    }
}