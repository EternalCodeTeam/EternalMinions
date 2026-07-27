package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.MinionRotation;
import com.eternalcode.minions.minion.tool.EnchantmentLevels;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Tag;
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

// TODO: need more logic in scaling damange/loot based on enchantments, currently this is "prowizorka"
public final class KillerBehavior implements MinionBehavior {

    private final KillerConfig config;
    private final MinionToolService tools;
    private final ToolRequirement toolRequirement;
    private final KillerLootingListener looting;
    private final Set<EntityType> allowedMobs;

    private final NearestMobFinder finder = new NearestMobFinder();

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

        return new KillerBehavior(config, tools, looting);
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

        ItemStack weapon = preparation.check() instanceof ToolCheck.Ready ready
                ? ready.tool()
                : null;

        Location origin = context.location();
        int attackRange = this.config.attackRange(minion.upgrades());

        Collection<Entity> nearbyEntities = context.world().getNearbyEntities(
                origin,
                attackRange,
                attackRange,
                attackRange
        );

        NearestMobFinder.SearchResult searchResult = this.finder.find(
                origin,
                nearbyEntities,
                this.allowedMobs,
                this.config.attackAllMonstersWhenEmpty,
                this.config.ignoreNamedMobs,
                this.config.ignoreInvulnerableMobs
        );

        if (searchResult.state() == NearestMobFinder.SearchResult.State.PROTECTED_MOBS) {
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

        int lootingLevel = EnchantmentLevels.level(
                weapon,
                Enchantment.LOOTING
        );

        int sweepingLevel = Math.min(
                Math.max(
                        0,
                        EnchantmentLevels.level(
                                weapon,
                                Enchantment.SWEEPING_EDGE
                        )
                ),
                Enchantment.SWEEPING_EDGE.getMaxLevel()
        );

        List<LivingEntity> sweepingTargets = this.findSweepingTargets(
                context,
                target,
                sweepingLevel
        );

        this.attack(
                weapon,
                origin,
                target,
                1.0D,
                lootingLevel
        );

        double sweepingDamageMultiplier =
                this.config.sweepingDamageMultiplier(sweepingLevel);

        for (LivingEntity sweepingTarget : sweepingTargets) {
            if (!sweepingTarget.isValid() || sweepingTarget.isDead()) {
                continue;
            }

            this.attack(
                    weapon,
                    origin,
                    sweepingTarget,
                    sweepingDamageMultiplier,
                    lootingLevel
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

    private List<LivingEntity> findSweepingTargets(
            MinionContext context,
            LivingEntity primaryTarget,
            int sweepingLevel
    ) {
        if (sweepingLevel <= 0) {
            return List.of();
        }

        double range = this.config.sweepingRange();

        Collection<Entity> nearbyEntities = context.world().getNearbyEntities(
                primaryTarget.getLocation(),
                range,
                range,
                range
        );

        return this.finder.findAdditional(
                primaryTarget.getLocation(),
                nearbyEntities,
                primaryTarget,
                sweepingLevel,
                range,
                this.allowedMobs,
                this.config.attackAllMonstersWhenEmpty,
                this.config.ignoreNamedMobs,
                this.config.ignoreInvulnerableMobs
        );
    }

    private void attack(
            ItemStack weapon,
            Location origin,
            LivingEntity target,
            double damageMultiplier,
            int lootingLevel
    ) {
        double damage =
                this.calculateDamage(weapon, target) * damageMultiplier;

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
    }

    private double calculateDamage(
            ItemStack weapon,
            LivingEntity target
    ) {
        double baseDamage = this.config.baseAttackDamage();

        if (weapon == null || weapon.getType().isAir()) {
            return baseDamage;
        }

        double weaponDamage = applyModifiers(
                baseDamage,
                this.attackDamageModifiers(weapon)
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
                    meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);

            if (customModifiers != null && !customModifiers.isEmpty()) {
                return customModifiers;
            }
        }

        return weapon.getType()
                .getDefaultAttributeModifiers(EquipmentSlot.HAND)
                .get(Attribute.ATTACK_DAMAGE);
    }

    @SuppressWarnings("deprecation")
    private double enchantmentDamage(ItemStack weapon, LivingEntity target) {
        double damage = 0.0D;

        int sharpness = EnchantmentLevels.level(
                weapon,
                Enchantment.SHARPNESS
        );

        if (sharpness > 0) {
            damage += 0.5D * sharpness + 0.5D;
        }

        int smite = EnchantmentLevels.level(
                weapon,
                Enchantment.SMITE
        );

        if (
                smite > 0
                        && Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(target.getType())
        ) {
            damage += 2.5D * smite;
        }

        int baneOfArthropods = EnchantmentLevels.level(
                weapon,
                Enchantment.BANE_OF_ARTHROPODS
        );

        if (
                baneOfArthropods > 0
                        && Tag.ENTITY_TYPES_SENSITIVE_TO_BANE_OF_ARTHROPODS.isTagged(target.getType())
        ) {
            damage += 2.5D * baneOfArthropods;
        }

        return damage;
    }

    private void applyFireAspect(
            ItemStack weapon,
            LivingEntity target
    ) {
        int level = EnchantmentLevels.level(
                weapon,
                Enchantment.FIRE_ASPECT
        );

        if (level <= 0) {
            return;
        }

        int fireTicks = level * 80;

        target.setFireTicks(
                Math.max(
                        target.getFireTicks(),
                        fireTicks
                )
        );
    }

    private void applyKnockback(
            ItemStack weapon,
            Location origin,
            LivingEntity target
    ) {
        int level = EnchantmentLevels.level(
                weapon,
                Enchantment.KNOCKBACK
        );

        if (level <= 0) {
            return;
        }

        Vector direction = target.getLocation()
                .toVector()
                .subtract(origin.toVector())
                .setY(0.0D);

        if (direction.lengthSquared() <= 0.0001D) {
            return;
        }

        direction.normalize().multiply(
                Math.max(
                        0.0D,
                        this.config.knockbackStrengthPerLevel
                ) * level
        );

        direction.setY(
                Math.max(
                        0.0D,
                        this.config.knockbackVerticalStrength
                )
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