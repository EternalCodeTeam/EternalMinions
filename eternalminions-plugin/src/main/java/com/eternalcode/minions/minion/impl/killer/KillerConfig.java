package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.entity.EntityType;

@Include(AbstractMinionConfig.class)
public final class KillerConfig extends AbstractMinionConfig {

    @Comment({
            "Mob types the killer is allowed to attack.",
            "When empty and attackAllMonstersWhenEmpty is enabled,",
            "the killer attacks every entity implementing the Monster interface."
    })
    public List<EntityType> allowedMobs = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON
    );

    @Comment({
            "When enabled, an empty allowedMobs list permits every hostile monster.",
            "When disabled, an empty list prevents the killer from attacking anything."
    })
    public boolean attackAllMonstersWhenEmpty = true;

    @Comment("Prevents the killer from attacking mobs with a custom name.")
    public boolean ignoreNamedMobs = true;

    @Comment("Prevents the killer from attacking invulnerable entities.")
    public boolean ignoreInvulnerableMobs = true;

    @Comment("Base attack range before applying the range upgrade.")
    public int attackRangeBlocks = 4;

    @Comment("Ticks between attacks.")
    public int attackCooldownTicks = 20;

    @Comment({
            "Base damage used before applying weapon attribute modifiers.",
            "Minecraft players normally have 1 base attack damage."
    })
    public double baseAttackDamage = 1.0D;

    @Comment("Range around the primary target used by Sweeping Edge.")
    public double sweepingRangeBlocks = 2.5D;

    @Comment("Horizontal knockback applied per Knockback enchantment level.")
    public double knockbackStrengthPerLevel = 0.4D;

    @Comment("Vertical knockback applied when the weapon has Knockback.")
    public double knockbackVerticalStrength = 0.15D;

    public KillerConfig() {
        this.displayName =
                "<color:#F11919:#FF3F3F:#F11919>ᴋɪʟʟᴇʀ";

        this.tool.category = ToolCategory.WEAPON;
        this.tool.required = true;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTc1MzJlOTBjNTczYTM5NGM3ODAyYWE0MTU4MzA1ODAyYjU5ZTY3ZjJhMmI3ZTNmZDAzNjNhYTZlYTQyYjg0MSJ9fX0=";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOWMxOTM4MDVmMWY4OTQ2OC5wbmcifX19";

        this.items.setLeatherArmorColor(
                Color.fromRGB(241, 25, 25)
        );

        this.statuses = defaultStatuses();

        this.usageInstructions.lore = List.of(
                "<gray>1. Postaw zabójcę przy miejscu pojawiania się mobów.",
                "<gray>2. Włóż broń do slotu narzędzia.",
                "<gray>3. Zabójca automatycznie zaatakuje najbliższy dozwolony cel.",
                "<gray>4. Drop trafia do magazynu lub podpiętej skrzyni.",
                "",
                "<dark_gray>Obsługiwane enchanty:",
                "<red>• Sharpness <gray>— zwiększa obrażenia przeciw wszystkim mobom.",
                "<red>• Smite <gray>— zwiększa obrażenia przeciw nieumarłym.",
                "<red>• Bane of Arthropods <gray>— zwiększa obrażenia przeciw stawonogom.",
                "<red>• Fire Aspect <gray>— podpala cele; poziom wydłuża podpalenie.",
                "<red>• Knockback <gray>— odrzuca cele; poziom zwiększa siłę.",
                "<red>• Looting <gray>— zwiększa ilość przedmiotów z zabitych mobów.",
                "<red>• Sweeping Edge I <gray>— trafia 1 dodatkowy cel za 50% obrażeń.",
                "<red>• Sweeping Edge II <gray>— trafia 2 dodatkowe cele za 67% obrażeń.",
                "<red>• Sweeping Edge III <gray>— trafia 3 dodatkowe cele za 75% obrażeń.",
                "<red>• Unbreaking <gray>— zmniejsza zużycie wytrzymałości broni.",
                "",
                "<yellow>Moby z nametagiem i niewrażliwe moby są chronione."
        );
    }

    public int attackRange(MinionUpgrades upgrades) {
        int baseRange = Math.max(
                1,
                this.attackRangeBlocks
        );

        return this.upgradeTierValueOrHigher(
                upgrades,
                CoreUpgradeKinds.RANGE,
                baseRange
        );
    }

    public int attackCooldown() {
        return Math.max(
                1,
                this.attackCooldownTicks
        );
    }

    public double baseAttackDamage() {
        return Math.max(
                0.0D,
                this.baseAttackDamage
        );
    }

    public double sweepingRange() {
        return Math.max(
                0.0D,
                this.sweepingRangeBlocks
        );
    }

    public double sweepingDamageMultiplier(int level) {
        if (level <= 0) {
            return 0.0D;
        }

        return (double) level / (level + 1.0D);
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses =
                new LinkedHashMap<>();

        statuses.put(
                KillerStatuses.ATTACKING,
                "<red>Atakowanie..."
        );

        statuses.put(
                KillerStatuses.NO_ENEMIES,
                "<yellow>Brak przeciwników w zasięgu"
        );

        statuses.put(
                KillerStatuses.PROTECTED_MOBS_NEARBY,
                "<yellow>W pobliżu są tylko chronione moby"
        );

        statuses.put(
                KillerStatuses.NO_WEAPON,
                "<red>Brak broni"
        );

        return statuses;
    }
}