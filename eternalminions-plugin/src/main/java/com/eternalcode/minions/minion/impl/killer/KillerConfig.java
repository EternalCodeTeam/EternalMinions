package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
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

    @Comment("Horizontal knockback applied per Knockback enchantment level.")
    public double knockbackStrengthPerLevel = 0.4D;

    @Comment("Vertical knockback applied when the weapon has Knockback.")
    public double knockbackVerticalStrength = 0.15D;

    public KillerConfig() {
        this.displayName = "<red>Zabójca";

        this.tool.category = ToolCategory.WEAPON;
        this.tool.required = true;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE3NTMyZTkwYzU3M2EzOTRjNzgwMmFhNDE1ODMwNTgwMmI1OWU2N2YyYTJiN2UzZmQwMzYzYWE2ZWE0MmI4NDEifX19";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOWMxOTM4MDVmMWY4OTQ2OC5wbmcifX19";

        this.items.setLeatherArmorColor(Color.fromRGB(255, 0, 0));

        this.statuses = defaultStatuses();

        this.usageInstructions.lore = List.of(
                "<gray>1. Postaw zabójcę przy miejscu pojawiania się mobów.",
                "<gray>2. Włóż broń do slotu narzędzia.",
                "<gray>3. Upewnij się, że mob znajduje się w zasięgu.",
                "<gray>4. Odbieraj drop z magazynu lub podpiętej skrzyni.",
                "",
                "<dark_gray>Obsługiwane właściwości broni:",
                "<red>• Obrażenia zależne od rodzaju broni.",
                "<red>• Sharpness, Smite i Bane of Arthropods.",
                "<red>• Fire Aspect, Knockback i Looting.",
                "<red>• Unbreaking obsługiwany przez system narzędzi.",
                "",
                "<yellow>Moby z nametagiem są chronione."
        );
    }

    public int attackRange(MinionUpgrades upgrades) {
        int baseRange = Math.max(1, this.attackRangeBlocks);
        int purchasedTier = upgrades.tier(CoreUpgradeKinds.RANGE);

        List<MinionUpgradeTierConfig> tiers =
                this.upgrades.get(CoreUpgradeKinds.RANGE);

        if (purchasedTier < 1 || tiers == null || tiers.isEmpty()) {
            return baseRange;
        }

        int tierIndex = Math.min(purchasedTier, tiers.size()) - 1;
        int upgradedRange = tiers.get(tierIndex).value;

        return Math.max(baseRange, upgradedRange);
    }

    public int attackCooldown() {
        return Math.max(1, this.attackCooldownTicks);
    }

    public double baseAttackDamage() {
        return Math.max(0.0D, this.baseAttackDamage);
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
