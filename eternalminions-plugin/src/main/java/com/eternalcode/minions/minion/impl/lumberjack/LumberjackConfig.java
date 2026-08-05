package com.eternalcode.minions.minion.impl.lumberjack;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class LumberjackConfig extends AbstractMinionConfig {

    @Comment("Maximum complete trees felled per cycle. Zero fells every tree found at a work station.")
    public int maxTreesPerCycle = 1;

    @Comment({
            "Log materials treated as parts of a tree.",
            "Covers every vanilla tree species so the minion is not limited to oak.",
            "Add more materials here to support custom or modded trees."
    })
    public List<XMaterial> logMaterials = List.of(
            XMaterial.OAK_LOG, XMaterial.STRIPPED_OAK_LOG, XMaterial.OAK_WOOD, XMaterial.STRIPPED_OAK_WOOD,
            XMaterial.SPRUCE_LOG, XMaterial.STRIPPED_SPRUCE_LOG, XMaterial.SPRUCE_WOOD, XMaterial.STRIPPED_SPRUCE_WOOD,
            XMaterial.BIRCH_LOG, XMaterial.STRIPPED_BIRCH_LOG, XMaterial.BIRCH_WOOD, XMaterial.STRIPPED_BIRCH_WOOD,
            XMaterial.JUNGLE_LOG, XMaterial.STRIPPED_JUNGLE_LOG, XMaterial.JUNGLE_WOOD, XMaterial.STRIPPED_JUNGLE_WOOD,
            XMaterial.ACACIA_LOG, XMaterial.STRIPPED_ACACIA_LOG, XMaterial.ACACIA_WOOD, XMaterial.STRIPPED_ACACIA_WOOD,
            XMaterial.DARK_OAK_LOG, XMaterial.STRIPPED_DARK_OAK_LOG, XMaterial.DARK_OAK_WOOD, XMaterial.STRIPPED_DARK_OAK_WOOD,
            XMaterial.CHERRY_LOG, XMaterial.STRIPPED_CHERRY_LOG, XMaterial.CHERRY_WOOD, XMaterial.STRIPPED_CHERRY_WOOD
    );

    @Comment({
            "Leaf materials removed together with the tree.",
            "The list is ignored when breakLeaves is disabled."
    })
    public List<XMaterial> leafMaterials = List.of(
            XMaterial.OAK_LEAVES,
            XMaterial.SPRUCE_LEAVES,
            XMaterial.BIRCH_LEAVES,
            XMaterial.JUNGLE_LEAVES,
            XMaterial.ACACIA_LEAVES,
            XMaterial.DARK_OAK_LEAVES,
            XMaterial.CHERRY_LEAVES
    );

    @Comment({
            "Sapling replanted for each log/wood variant, matching the species that was cut.",
            "A log material without an entry here falls back to the first sapling below."
    })
    public Map<XMaterial, XMaterial> saplingByLog = defaultSaplingByLog();

    @Comment({
            "Whether natural leaves should be removed immediately.",
            "Leaves placed manually by players are protected by default."
    })
    public boolean breakLeaves = true;

    @Comment("Whether persistent leaves placed by players may be removed.")
    public boolean breakPersistentLeaves = false;

    @Comment({
            "Whether every block in a 2x2 trunk footprint should be replanted.",
            "This allows large trees to grow again after being cut."
    })
    public boolean replantFullTrunkFootprint = true;

    @Comment("Maximum number of connected log blocks in one tree.")
    public int maxLogsPerTree = 256;

    @Comment("Maximum number of leaves removed from one tree.")
    public int maxLeavesPerTree = 1024;

    @Comment("Maximum horizontal distance from the tree base.")
    public int maxTreeRadius = 12;

    @Comment("Maximum height above the tree base included in the scan.")
    public int maxTreeHeight = 40;

    @Comment({
            "Maximum distance between the tree logs and removed leaves.",
            "Higher values support wider canopies but scan more blocks."
    })
    public int leafSearchRadius = 6;

    public LumberjackConfig() {
        this.displayName = "<color:#BE7B00:#E09712:#BE7B00>ʟᴜᴍʙᴇʀᴊᴀᴄᴋ";

        this.tool.category = ToolCategory.AXE;
        this.tool.required = true;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjMGE5MTk5MGU3NmM2ZDY1ODA1MGI3YWM3ZTQ4MmJjNTgyYjI0NTg5YmI3ZjE0NmJkMWMwM2I5Yzg0Y2RkOSJ9fX0=";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvODkzNjFiMTYyMzIzYzYyYS5wbmcifX19";

        this.items.setLeatherArmorColor(Color.fromRGB(190, 123, 0));

        this.statuses = defaultStatuses();

        this.usageInstructions.lore = List.of(
                "<gray>1. Włóż siekierę do slotu narzędzia.",
                "<gray>2. Ustaw kierunek pracy w panelu.",
                "<gray>3. Posadź sadzonki w linii przed minionem.",
                "<gray>4. Zaczekaj, aż wyrosną drzewa.",
                "",
                "<dark_gray>Działanie:",
                "<aqua>• Drwal ścina również duże drzewa.",
                "<aqua>• Naturalne liście są usuwane automatycznie.",
                "<aqua>• Ule i gniazda pszczół przy drzewie są usuwane.",
                "<aqua>• Pnie 2x2 są ponownie sadzone jako 2x2.",
                "<aqua>• Fortune i Silk Touch wpływają na drop.",
                "",
                "<yellow>Liście postawione przez gracza są chronione."
        );
    }

    public int stationCount(MinionUpgrades upgrades) {
        return this.upgradeTierValueOrHigher(upgrades, DefaultUpgradeKinds.RANGE, 1);
    }

    public int maximumLogs() {
        return Math.max(
                1,
                Math.min(this.maxLogsPerTree, 4096)
        );
    }

    public int maximumLeaves() {
        return Math.max(
                1,
                Math.min(this.maxLeavesPerTree, 16_384)
        );
    }

    public int maximumTreeRadius() {
        return Math.max(
                1,
                Math.min(this.maxTreeRadius, 32)
        );
    }

    public int maximumTreeHeight() {
        return Math.max(
                1,
                Math.min(this.maxTreeHeight, 128)
        );
    }

    public int maximumLeafRadius() {
        return Math.max(
                1,
                Math.min(this.leafSearchRadius, 16)
        );
    }

    private static Map<XMaterial, XMaterial> defaultSaplingByLog() {
        Map<XMaterial, XMaterial> saplingByLog = new LinkedHashMap<>();

        putSpecies(saplingByLog, XMaterial.OAK_SAPLING,
                XMaterial.OAK_LOG, XMaterial.STRIPPED_OAK_LOG, XMaterial.OAK_WOOD, XMaterial.STRIPPED_OAK_WOOD);
        putSpecies(saplingByLog, XMaterial.SPRUCE_SAPLING,
                XMaterial.SPRUCE_LOG, XMaterial.STRIPPED_SPRUCE_LOG, XMaterial.SPRUCE_WOOD, XMaterial.STRIPPED_SPRUCE_WOOD);
        putSpecies(saplingByLog, XMaterial.BIRCH_SAPLING,
                XMaterial.BIRCH_LOG, XMaterial.STRIPPED_BIRCH_LOG, XMaterial.BIRCH_WOOD, XMaterial.STRIPPED_BIRCH_WOOD);
        putSpecies(saplingByLog, XMaterial.JUNGLE_SAPLING,
                XMaterial.JUNGLE_LOG, XMaterial.STRIPPED_JUNGLE_LOG, XMaterial.JUNGLE_WOOD, XMaterial.STRIPPED_JUNGLE_WOOD);
        putSpecies(saplingByLog, XMaterial.ACACIA_SAPLING,
                XMaterial.ACACIA_LOG, XMaterial.STRIPPED_ACACIA_LOG, XMaterial.ACACIA_WOOD, XMaterial.STRIPPED_ACACIA_WOOD);
        putSpecies(saplingByLog, XMaterial.DARK_OAK_SAPLING,
                XMaterial.DARK_OAK_LOG, XMaterial.STRIPPED_DARK_OAK_LOG, XMaterial.DARK_OAK_WOOD, XMaterial.STRIPPED_DARK_OAK_WOOD);
        putSpecies(saplingByLog, XMaterial.CHERRY_SAPLING,
                XMaterial.CHERRY_LOG, XMaterial.STRIPPED_CHERRY_LOG, XMaterial.CHERRY_WOOD, XMaterial.STRIPPED_CHERRY_WOOD);

        return saplingByLog;
    }

    private static void putSpecies(
            Map<XMaterial, XMaterial> saplingByLog,
            XMaterial sapling,
            XMaterial... logs
    ) {
        for (XMaterial log : logs) {
            saplingByLog.put(log, sapling);
        }
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses =
                new LinkedHashMap<>();

        statuses.put(
                LumberjackStatuses.CUTTING,
                "<green>Ścinanie drzewa..."
        );

        statuses.put(
                LumberjackStatuses.WAITING_FOR_TREE,
                "<yellow>Czekam, aż drzewo wyrośnie"
        );

        statuses.put(
                LumberjackStatuses.NO_SAPLING,
                "<red>Brak sadzonki na stanowisku"
        );

        statuses.put(
                LumberjackStatuses.INVALID_STATION,
                "<red>Stanowisko jest zablokowane"
        );

        statuses.put(
                LumberjackStatuses.TREE_TOO_LARGE,
                "<red>Drzewo jest zbyt duże"
        );

        statuses.put(
                LumberjackStatuses.CANOPY_TOO_LARGE,
                "<red>Korona drzewa jest zbyt duża"
        );

        statuses.put(
                LumberjackStatuses.NO_AXE,
                "<red>Brak siekiery"
        );

        return statuses;
    }
}
