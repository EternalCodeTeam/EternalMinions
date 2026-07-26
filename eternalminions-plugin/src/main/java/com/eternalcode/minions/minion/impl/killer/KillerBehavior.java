package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public final class KillerBehavior implements MinionBehavior {

    private static final Set<EntityType> UNDEAD = EnumSet.of(
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.WITHER_SKELETON,
        EntityType.ZOMBIE_VILLAGER,
        EntityType.HUSK,
        EntityType.STRAY,
        EntityType.DROWNED,
        EntityType.PHANTOM,
        EntityType.WITHER,
        EntityType.ZOGLIN
    );
    private static final Set<EntityType> ARTHROPODS = EnumSet.of(
        EntityType.SPIDER,
        EntityType.CAVE_SPIDER,
        EntityType.SILVERFISH,
        EntityType.ENDERMITE
    );

    private final KillerConfig config;
    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final NearestMobFinder finder = new NearestMobFinder();
    private final KillerLootingListener looting;
    private final Set<EntityType> allowedMobs;

    public KillerBehavior(
        KillerConfig config,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator,
        KillerLootingListener looting
    ) {
        this.config = config;
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
        this.looting = looting;
        this.allowedMobs = config.allowedMobs.isEmpty()
            ? Set.of(EntityType.ZOMBIE)
            : Set.copyOf(config.allowedMobs);
        if (config.attackRangeBlocks < 1 || config.attackCooldownTicks < 1 || config.baseDamage <= 0.0D) {
            throw new IllegalArgumentException("Killer range, cooldown and damage must be positive");
        }
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
        Minion minion = context.retireWornTool(
            context.minion(),
            this.toolRequirement,
            this.toolValidation,
            this.toolLocator
        );
        ToolCheck toolCheck = this.toolValidation.validate(
            this.toolRequirement,
            minion.equipment().tool(),
            KillerStatuses.NO_WEAPON
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }

        ItemStack tool = toolCheck instanceof ToolCheck.Ready ready ? ready.tool() : null;
        Location center = context.location();
        List<LivingEntity> nearby = this.nearbyTargets(context, center);
        List<NearestMobFinder.Candidate> candidates = this.candidates(nearby);
        int nearestIndex = this.finder.findNearest(center.getX(), center.getY(), center.getZ(), candidates);
        if (nearestIndex < 0) {
            return MinionResult.idle(minion, KillerStatuses.NO_ENEMIES);
        }

        LivingEntity target = nearby.get(nearestIndex);
        double damage = this.damage(tool, target);
        int lootingLevel = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LOOTING);
        this.looting.trackHit(target.getUniqueId(), lootingLevel);
        target.damage(damage);

        Minion updated = this.consumeTool(minion, tool);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        float yaw = (float) Math.toDegrees(Math.atan2(
            -(target.getLocation().getX() - center.getX()),
            target.getLocation().getZ() - center.getZ()
        ));
        context.scheduledMinion().face(yaw);
        return MinionResult.worked(updated, KillerStatuses.ATTACKING)
            .withDelay(this.config.attackCooldownTicks);
    }

    private List<LivingEntity> nearbyTargets(MinionContext context, Location center) {
        double range = this.config.attackRangeBlocks;
        List<LivingEntity> nearby = new ArrayList<>();
        for (Entity entity : context.world().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isValid() && !livingEntity.isDead()) {
                nearby.add(livingEntity);
            }
        }
        return nearby;
    }

    private List<NearestMobFinder.Candidate> candidates(List<LivingEntity> nearby) {
        List<NearestMobFinder.Candidate> candidates = new ArrayList<>(nearby.size());
        for (int entityIndex = 0; entityIndex < nearby.size(); entityIndex++) {
            LivingEntity entity = nearby.get(entityIndex);
            Location location = entity.getLocation();
            candidates.add(new NearestMobFinder.Candidate(
                entityIndex,
                location.getX(),
                location.getY(),
                location.getZ(),
                this.allowedMobs.contains(entity.getType())
            ));
        }
        return candidates;
    }

    private double damage(ItemStack tool, LivingEntity target) {
        if (tool == null) {
            return this.config.baseDamage;
        }

        double damage = this.config.baseDamage;
        int sharpness = tool.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpness > 0) {
            damage += sharpness * 0.5D + 0.5D;
        }
        int smite = tool.getEnchantmentLevel(Enchantment.SMITE);
        if (smite > 0 && UNDEAD.contains(target.getType())) {
            damage += smite * 2.5D;
        }
        int bane = tool.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
        if (bane > 0 && ARTHROPODS.contains(target.getType())) {
            damage += bane * 2.5D;
        }
        int fireAspect = tool.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
        if (fireAspect > 0) {
            target.setFireTicks(fireAspect * 80);
        }
        return damage;
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }
        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        return minion.withEquipment(minion.equipment().withTool(damagedTool));
    }

}
