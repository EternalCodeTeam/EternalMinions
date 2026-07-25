package com.eternalcode.minions.minion.killer;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

// Attacks the nearest allowed mob in range with a real, enchant-aware damage roll and cooldown.
// Never collects loot itself — the mob's own death event drops normally on the ground.
public final class KillerBehavior extends AbstractMinionBehavior {

    private static final Set<EntityType> UNDEAD = EnumSet.of(
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON, EntityType.ZOMBIE_VILLAGER,
        EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED, EntityType.PHANTOM, EntityType.WITHER, EntityType.ZOGLIN
    );
    private static final Set<EntityType> ARTHROPODS = EnumSet.of(
        EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMITE
    );

    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final NearestMobFinder finder = new NearestMobFinder();
    private final KillerLootingListener looting;

    public KillerBehavior(
        MinionRegistry registry,
        MinionPersistenceService persistence,
        MinionRenderer renderer,
        MinionStatusTracker statuses,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator,
        KillerLootingListener looting
    ) {
        super(registry, persistence, renderer, statuses);
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.looting = looting;
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        minion = this.retireWornToolIfNeeded(minion, type, world, this.toolValidation, this.toolLocator);

        ToolCheck toolCheck = this.toolValidation.validate(
            type.work().toolRequirement(), minion.equipment().tool(), KillerStatuses.NO_WEAPON);
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(minion, stopped.reason());
            return false;
        }
        ItemStack tool = toolCheck instanceof ToolCheck.Ready ready ? ready.tool() : null;

        KillerWork work = type.work().killer();
        Location center = this.minionLocation(minion, world);
        double range = work.attackRangeBlocks();

        List<LivingEntity> nearby = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(center, range, range, range)) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isValid() && !livingEntity.isDead()) {
                nearby.add(livingEntity);
            }
        }

        List<NearestMobFinder.Candidate> candidates = new ArrayList<>(nearby.size());
        for (int index = 0; index < nearby.size(); index++) {
            LivingEntity entity = nearby.get(index);
            boolean allowed = work.allowedMobs().contains(entity.getType());
            Location location = entity.getLocation();
            candidates.add(new NearestMobFinder.Candidate(index, location.getX(), location.getY(), location.getZ(), allowed));
        }
        int nearestIndex = this.finder.findNearest(center.getX(), center.getY(), center.getZ(), candidates);
        if (nearestIndex < 0) {
            this.refreshStatusIfChanged(minion, KillerStatuses.NO_ENEMIES);
            return false;
        }
        LivingEntity target = nearby.get(nearestIndex);

        long worldTime = world.getFullTime();
        if (scheduledMinion.isBusyUntil(worldTime)) {
            this.refreshStatusIfChanged(minion, KillerStatuses.ATTACKING);
            return false;
        }
        scheduledMinion.busyUntil(worldTime + work.attackCooldownTicks());

        double damage = work.baseDamage();
        int lootingLevel = 0;
        if (tool != null) {
            int sharpness = tool.getEnchantmentLevel(Enchantment.SHARPNESS);
            if (sharpness > 0) {
                damage += sharpness * 0.5D + 0.5D;
            }
            int smite = tool.getEnchantmentLevel(Enchantment.SMITE);
            if (smite > 0 && UNDEAD.contains(target.getType())) {
                damage += smite * 2.5D;
            }
            int baneOfArthropods = tool.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
            if (baneOfArthropods > 0 && ARTHROPODS.contains(target.getType())) {
                damage += baneOfArthropods * 2.5D;
            }
            int fireAspect = tool.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
            if (fireAspect > 0) {
                target.setFireTicks(fireAspect * 80);
            }
            lootingLevel = tool.getEnchantmentLevel(Enchantment.LOOTING);
        }

        this.looting.trackHit(target.getUniqueId(), lootingLevel);
        target.damage(damage);

        Minion updated = this.consumeTool(minion, tool);
        float yaw = (float) Math.toDegrees(Math.atan2(
            -(target.getLocation().getX() - center.getX()), target.getLocation().getZ() - center.getZ()));
        this.commit(updated, type, updated.storage(), yaw);
        this.refreshStatusIfChanged(updated, KillerStatuses.ATTACKING);
        return true;
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }

        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        Minion updated = minion.withEquipment(minion.equipment().withTool(damagedTool));
        this.registry.replace(updated);
        this.persistence.saveEquipment(updated);
        this.renderer.refreshEquipment(updated.id(), damagedTool);
        return updated;
    }
}
