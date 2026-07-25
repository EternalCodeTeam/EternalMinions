package com.eternalcode.minions.render;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3f;
import java.util.List;
import java.util.Locale;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;

public final class EntityLibHologramRenderer {

    private final Server server;
    private final MiniMessage miniMessage;
    private final MinionTypeService types;
    private final MinionsConfig config;
    private final MinionStatusTracker statusTracker;

    public EntityLibHologramRenderer(
            Server server,
            MiniMessage miniMessage,
            MinionTypeService types,
            MinionsConfig config,
            MinionStatusTracker statusTracker
    ) {
        this.server = server;
        this.miniMessage = miniMessage;
        this.types = types;
        this.config = config;
        this.statusTracker = statusTracker;
    }

    private static int parseBackgroundColor(String color) {
        String hexadecimal = normalizeColor(color);

        if (hexadecimal.length() == 6) {
            hexadecimal = "FF" + hexadecimal;
        }

        if (hexadecimal.length() != 8) {
            throw new IllegalArgumentException(
                    "Invalid hologram background color: " + color
                            + ". Expected #RRGGBB or #AARRGGBB."
            );
        }

        try {
            return (int) Long.parseLong(hexadecimal, 16);
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid hologram background color: " + color,
                    exception
            );
        }
    }

    private static int parseGlowColor(String color) {
        if (color == null || color.isBlank()) {
            return -1;
        }

        String hexadecimal = normalizeColor(color);

        if (hexadecimal.length() != 6) {
            throw new IllegalArgumentException(
                    "Invalid hologram glow color: " + color
                            + ". Expected #RRGGBB."
            );
        }

        try {
            return Integer.parseInt(hexadecimal, 16);
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid hologram glow color: " + color,
                    exception
            );
        }
    }

    private static String normalizeColor(String color) {
        if (color == null) {
            return "";
        }

        String normalized = color.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public WrapperEntity create(Minion minion) {
        WrapperEntity hologram = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = hologram.getEntityMeta(TextDisplayMeta.class);

        MinionsConfig.HologramConfig.HologramSettings settings =
                this.config.hologram.settings;

        meta.setText(this.text(minion));

        meta.setBillboardConstraints(settings.billboard);
        meta.setLineWidth(Math.max(1, settings.lineWidth));
        meta.setTextOpacity((byte) clamp(settings.textOpacity, 0, 255));

        meta.setShadow(settings.textShadow);
        meta.setBackgroundColor(parseBackgroundColor(settings.backgroundColor));

        meta.setViewRange(Math.max(0.0F, settings.viewRange));

        float scale = Math.max(0.0F, settings.scale);
        meta.setScale(new Vector3f(scale, scale, scale));

        meta.setWidth(Math.max(0.0F, settings.displayWidth));
        meta.setHeight(Math.max(0.0F, settings.displayHeight));

        meta.setShadowRadius(Math.max(0.0F, settings.entityShadowRadius));
        meta.setShadowStrength(clamp(settings.entityShadowStrength, 0.0F, 1.0F));

        meta.setGlowing(settings.glowing);
        meta.setGlowColorOverride(parseGlowColor(settings.glowColor));

        this.applyAlignment(meta, settings.alignment);

        hologram.setHasNoGravity(true);
        return hologram;
    }

    public Location location(Minion minion) {
        return RenderLocation.of(
                minion,
                this.config.hologram.verticalOffset
        );
    }

    public Component text(Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        String typeName = type == null ? "Minion #" + minion.id().value() : type.displayName();

        String ownerName = this.server
                .getOfflinePlayer(minion.ownerId())
                .getName();

        String level = Integer.toString(minion.progress().level());
        MinionStatus currentStatus = this.statusTracker.status(minion.id());
        String status = type == null ? currentStatus.key() : type.statusText(currentStatus);

        List<String> lines = this.config.hologram.hologramLines;
        Component text = Component.empty();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index)
                    .replace("{TYPE}", typeName)
                    .replace("{OWNER}", ownerName == null ? "?" : ownerName)
                    .replace("{LEVEL}", level)
                    .replace("{STATUS}", status);

            if (index > 0) {
                text = text.append(Component.newline());
            }

            text = text.append(this.miniMessage.deserialize(line));
        }

        return text;
    }

    private void applyAlignment(
            TextDisplayMeta meta,
            MinionsConfig.HologramConfig.HologramAlignment alignment
    ) {
        meta.setAlignLeft(false);
        meta.setAlignRight(false);

        switch (alignment) {
            case LEFT -> meta.setAlignLeft(true);
            case RIGHT -> meta.setAlignRight(true);
            case CENTER -> {
                // Center alignment is represented by both flags being disabled.
            }
        }
    }
}
