package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionRotation;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public final class KillerBehavior implements MinionBehavior {

    private final KillerConfig config;

    private final MinionToolService tools;
    private final ToolRequirement toolRequirement;

    private final NearestMobFinder finder =
            new NearestMobFinder();

    private final KillerLootingListener looting;
    private final Set<EntityType> allowedMobs;

    public static KillerBehavior create(
            ConfigService configs,
            File directory,
            MinionToolService tools,
            KillerLootingListener looting
    ) {
        KillerConfig config = configs.load(
                KillerConfig.class,
                new File(directory, "killer.yml")
        );

        return new KillerBehavior(
                config,
                tools,
                looting
        );
    }

    public KillerBehavior(
            KillerConfig config,
            MinionToolService tools,
            KillerLootingListener looting
    ) {
        this.config = config;

        this.tools = tools;
        this.toolRequirement = config.toolRequirement();

        this.looting = looting;

        this.allowedMobs = config.allowedMobs.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(config.allowedMobs));
    }

    @Override
    public String id() {
        return "killer";
    }

    @Override
    public AbstractMinionConfig config() {
        return this.config;
    }

    @Override
    public MinionResult execute(MinionContext context) {
        MinionToolPreparation preparation = this.tools.prepare(
                context,
                this.toolRequirement,
                KillerStatuses.NO_WEAPON
        );
        if (preparation.check() instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(
                    preparation.minion(),
                    stopped.reason()
            );
        }
        Minion minion = preparation.minion();
        ToolCheck toolCheck = preparation.check();

        ItemStack weapon = toolCheck instanceof ToolCheck.Ready ready
                ? ready.tool()
                : null;

        Location origin = context.location();
        int range = this.config.attackRange(minion.upgrades());

        Collection<Entity> nearbyEntities =
                context.world().getNearbyEntities(
                        origin,
                        range,
                        range,
                        range
                );

        NearestMobFinder.SearchResult searchResult =
                this.finder.find(
                        origin,
                        nearbyEntities,
                        this.allowedMobs,
                        this.config.attackAllMonstersWhenEmpty,
                        this.config.ignoreNamedMobs,
                        this.config.ignoreInvulnerableMobs
                );

        if (
                searchResult.state()
                        == NearestMobFinder.SearchResult.State.PROTECTED_MOBS
        ) {
            return MinionResult.idle(
                    minion,
                    KillerStatuses.PROTECTED_MOBS_NEARBY
            );
        }

        LivingEntity target = searchResult.target();

        if (target == null) {
            return MinionResult.idle(
                    minion,
                    KillerStatuses.NO_ENEMIES
            );
        }

        this.faceTarget(context, origin, target);

        double damage = this.calculateDamage(
                weapon,
                target
        );

        int lootingLevel = enchantmentLevel(
                weapon,
                Enchantment.LOOTING
        );

        this.looting.trackHit(
                target.getUniqueId(),
                lootingLevel
        );

        this.applyFireAspect(weapon, target);

        target.damage(damage);

        if (!target.isDead()) {
            this.applyKnockback(
                    weapon,
                    origin,
                    target
            );
        }

        Minion updated = this.tools.consume(minion, 1);

        updated = updated.withProgress(
                updated.progress().advanced(this.config)
        );

        return MinionResult
                .worked(updated, KillerStatuses.ATTACKING)
                .withDelay(this.config.attackCooldown());
    }

    private double calculateDamage(
            ItemStack weapon,
            LivingEntity target
    ) {
        double baseDamage =
                this.config.baseAttackDamage();

        if (weapon == null || weapon.getType().isAir()) {
            return baseDamage;
        }

        Collection<AttributeModifier> modifiers =
                this.attackDamageModifiers(weapon);

        double weaponDamage = applyModifiers(
                baseDamage,
                modifiers
        );

        double enchantmentDamage =
                this.enchantmentDamage(weapon, target);

        return Math.max(
                0.0D,
                weaponDamage + enchantmentDamage
        );
    }

    private Collection<AttributeModifier> attackDamageModifiers(
            ItemStack weapon
    ) {
        ItemMeta meta = weapon.getItemMeta();

        if (meta != null) {
            Collection<AttributeModifier> customModifiers =
                    meta.getAttributeModifiers(
                            Attribute.ATTACK_DAMAGE
                    );

            if (
                    customModifiers != null
                            && !customModifiers.isEmpty()
            ) {
                return customModifiers;
            }
        }

        return weapon.getType()
                .getDefaultAttributeModifiers(EquipmentSlot.HAND)
                .get(Attribute.ATTACK_DAMAGE);
    }

    @SuppressWarnings("deprecation")
    private double enchantmentDamage(
            ItemStack weapon,
            LivingEntity target
    ) {
        double damage = 0.0D;

        for (
                Map.Entry<Enchantment, Integer> entry
                : weapon.getEnchantments().entrySet()
        ) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();

            damage += enchantment.getDamageIncrease(
                    level,
                    target.getType()
            );
        }

        return damage;
    }

    private void applyFireAspect(
            ItemStack weapon,
            LivingEntity target
    ) {
        int level = enchantmentLevel(
                weapon,
                Enchantment.FIRE_ASPECT
        );

        if (level <= 0) {
            return;
        }

        int fireTicks = level * 80;

        target.setFireTicks(
                Math.max(target.getFireTicks(), fireTicks)
        );
    }

    private void applyKnockback(
            ItemStack weapon,
            Location origin,
            LivingEntity target
    ) {
        int level = enchantmentLevel(
                weapon,
                Enchantment.KNOCKBACK
        );

        if (level <= 0) {
            return;
        }

        Vector direction = target.getLocation()
                .toVector()
                .subtract(origin.toVector());

        direction.setY(0.0D);

        if (direction.lengthSquared() <= 0.0001D) {
            return;
        }

        direction.normalize().multiply(
                Math.max(0.0D, this.config.knockbackStrengthPerLevel)
                        * level
        );

        direction.setY(
                Math.max(0.0D, this.config.knockbackVerticalStrength)
        );

        target.setVelocity(
                target.getVelocity().add(direction)
        );
    }

    private static double applyModifiers(
            double baseValue,
            Collection<AttributeModifier> modifiers
    ) {
        double addedValue = 0.0D;
        double multipliedBase = 0.0D;
        double multipliedTotal = 1.0D;

        for (AttributeModifier modifier : modifiers) {
            switch (modifier.getOperation()) {
                case ADD_NUMBER ->
                        addedValue += modifier.getAmount();

                case ADD_SCALAR ->
                        multipliedBase += modifier.getAmount();

                case MULTIPLY_SCALAR_1 ->
                        multipliedTotal *= 1.0D + modifier.getAmount();
            }
        }

        double value = baseValue + addedValue;

        value += value * multipliedBase;

        return value * multipliedTotal;
    }

    private static int enchantmentLevel(
            ItemStack item,
            Enchantment enchantment
    ) {
        if (item == null) {
            return 0;
        }

        return item.getEnchantmentLevel(enchantment);
    }

    private static void faceTarget(
            MinionContext context,
            Location origin,
            LivingEntity target
    ) {
        Location targetLocation = target.getLocation();

        float yaw = MinionRotation.yawTowards(
                origin.getX(),
                origin.getZ(),
                targetLocation.getX(),
                targetLocation.getZ()
        );

        context.scheduledMinion().face(yaw);
    }
}
