package com.eternalcode.minions.config;

import com.eternalcode.minions.minion.activity.config.ActivityBypassConfig;
import com.eternalcode.minions.minion.activity.rule.loadedchunk.LoadedChunkConfig;
import com.eternalcode.minions.minion.activity.rule.offline.OfflineConfig;
import com.eternalcode.minions.minion.activity.rule.proximity.ProximityConfig;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.render.MinionRendererType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;

public final class MinionsConfig extends ConfigurationFile {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("config.yml");
    }

    @Comment("Renderer used by every minion. Changing it requires a server restart.")
    public MinionRendererType minionRenderer = MinionRendererType.ARMOR_STAND;

    @Comment({
        "Status text shared by every minion profession. Supports MiniMessage.",
        "Profession-specific statuses (e.g. farmer's HARVESTING) live in that profession's own config."
    })
    public Map<MinionStatus, String> statuses = defaultStatuses();

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(CoreMinionStatuses.IDLE, "<gray>Bezczynny");
        statuses.put(CoreMinionStatuses.WORKING, "<green>Pracuje");
        statuses.put(CoreMinionStatuses.NO_TOOL, "<red>Brak narzędzia");
        statuses.put(CoreMinionStatuses.STORAGE_FULL, "<red>Magazyn pełny");
        statuses.put(CoreMinionStatuses.OFFLINE, "<dark_gray>Właściciel offline");
        statuses.put(CoreMinionStatuses.AWAY, "<dark_gray>Właściciel poza zasięgiem");
        return statuses;
    }

    @Comment("Maximum distance at which a client-side minion representation is spawned.")
    public int renderDistanceBlocks = 64;

    @Comment("Maximum distance at which action animations and particles are sent.")
    public int animationDistanceBlocks = 32;

    @Comment("Maximum scheduler work budget during one server tick, in microseconds.")
    public int schedulerBudgetMicros = 2_000;

    @Comment("Maximum number of physical world actions executed during one server tick.")
    public int physicalActionsPerTick = 250;

    @Comment("Maximum amount of offline progress credited after loading a minion.")
    public int maximumOfflineHours = 168;

    @Comment("Maximum distance between a minion and its linked chest, in blocks.")
    public int chestLinkDistanceBlocks = 10;

    @Comment({
            "Drops items on the ground when the linked chest and internal storage cannot hold them.",
            "When disabled, the minion waits with the STORAGE_FULL status until space is available."
    })
    public boolean dropOverflowItems = true;

    @Comment("Seconds the player has to click a chest after starting link mode.")
    public int chestLinkTimeoutSeconds = 10;

    @Comment("Hologram lines rendered above every minion. Supports MiniMessage.")
    public HologramConfig hologram = new HologramConfig();

    @Comment("Limits on how many minions can be placed.")
    public LimitsConfig limits = new LimitsConfig();

    @Comment("Rules that reduce or stop minion activity based on the owner's status.")
    public ActivityConfig activity = new ActivityConfig();

    public static class LimitsConfig extends OkaeriConfig {

        @Comment({
            "Maximum number of minions a player can place, grantable through permissions.",
            "Grant eternalminions.limit.<amount> (e.g. eternalminions.limit.20) to set a player's limit -",
            "the highest granted number wins. Grant eternalminions.limit.* for no limit at all.",
            "Players without any eternalminions.limit.<amount> permission fall back to this default."
        })
        public int defaultLimit = 5;
    }

    public static class HologramConfig extends OkaeriConfig {

        @Comment({
                "Hologram lines rendered above every minion. Supports MiniMessage.",
                "",
                "Available placeholders:",
                "{TYPE}  - minion type display name",
                "{OWNER} - minion owner name",
                "{LEVEL} - current minion level",
                "{STATUS} - current runtime work status"
        })
        public List<String> hologramLines = List.of(
                "<b><gradient:#FACC15:#FFE15F:#FACC15>{TYPE}</gradient></b>",
                "<#FFE15F>Właściciel: <white>{OWNER}",
                "<#FFE15F>Poziom: <white>{LEVEL}",
                "<#FFE15F>Status: <white>{STATUS}"
        );

        @Comment({
                "Vertical position of the hologram relative to the minion.",
                "Higher values move the hologram upwards."
        })
        public double verticalOffset = 1.75D;

        @Comment("Advanced appearance and rendering properties of the hologram.")
        public HologramSettings settings = new HologramSettings();

        public enum HologramAlignment {
            LEFT,
            CENTER,
            RIGHT
        }

        public static class HologramSettings extends OkaeriConfig {

            @Comment({
                    "Controls how the hologram rotates relative to the player.",
                    "",
                    "Available values:",
                    "FIXED      - does not rotate towards the player",
                    "VERTICAL   - rotates only around the vertical axis",
                    "HORIZONTAL - rotates only around the horizontal axis",
                    "CENTER     - always fully faces the player"
            })
            public AbstractDisplayMeta.BillboardConstraints billboard =
                    AbstractDisplayMeta.BillboardConstraints.CENTER;

            @Comment({
                    "Horizontal alignment of the hologram text.",
                    "Available values: LEFT, CENTER, RIGHT."
            })
            public HologramAlignment alignment = HologramAlignment.CENTER;

            @Comment({
                    "Maximum width of a text line before Minecraft wraps it.",
                    "The value is measured in text-display pixels."
            })
            public int lineWidth = 160;

            @Comment({
                    "Opacity of the hologram text.",
                    "Allowed range: 0-255.",
                    "0 = completely transparent, 255 = fully visible."
            })
            public int textOpacity = 255;

            @Comment("Whether the text itself should have a shadow.")
            public boolean textShadow = true;

            @Comment({
                    "Custom hologram background color.",
                    "Supported formats: #RRGGBB or #AARRGGBB.",
                    "AA represents opacity: 00 = transparent, FF = fully visible.",
                    "Example: #80000000 creates a semi-transparent black background."
            })
            public String backgroundColor = "#00000000";

            @Comment({
                    "Client-side rendering range multiplier.",
                    "Higher values allow the hologram to be rendered from farther away."
            })
            public float viewRange = 1.0F;

            @Comment({
                    "Visual scale of the entire hologram.",
                    "1.0 keeps the normal size, 0.5 makes it half-sized, 2.0 doubles it."
            })
            public float scale = 1.0F;

            @Comment({
                    "Width of the hologram's client-side culling box.",
                    "This does not resize the text.",
                    "Use 0.0 to keep Minecraft's default behavior."
            })
            public float displayWidth = 0.0F;

            @Comment({
                    "Height of the hologram's client-side culling box.",
                    "This does not resize the text.",
                    "Use 0.0 to keep Minecraft's default behavior."
            })
            public float displayHeight = 0.0F;

            @Comment({
                    "Radius of the display entity's ground shadow.",
                    "This is separate from the text-shadow option.",
                    "Use 0.0 to disable the ground shadow."
            })
            public float entityShadowRadius = 0.0F;

            @Comment({
                    "Strength of the display entity's ground shadow.",
                    "Allowed range: 0.0-1.0.",
                    "This has no visible effect when entity-shadow-radius is 0."
            })
            public float entityShadowStrength = 1.0F;

            @Comment("Whether the hologram should have the Minecraft glowing outline.")
            public boolean glowing = false;

            @Comment({
                    "Custom color of the glowing outline.",
                    "Use an empty value to use Minecraft's default or scoreboard-team color.",
                    "Format: #RRGGBB."
            })
            public String glowColor = "";
        }
    }

    public static class ActivityConfig extends OkaeriConfig {

        @Comment("Nerf applied while the minion's owner is offline.")
        public OfflineConfig offline = new OfflineConfig();

        @Comment("Nerf applied while nobody required is standing near the minion.")
        public ProximityConfig proximity = new ProximityConfig();

        @Comment("Nerf applied while the chunk the minion stands in is not loaded.")
        public LoadedChunkConfig loadedChunk = new LoadedChunkConfig();

        @Comment("Ways a minion can be fully exempted from every activity rule.")
        public ActivityBypassConfig bypass = new ActivityBypassConfig();
    }
}
