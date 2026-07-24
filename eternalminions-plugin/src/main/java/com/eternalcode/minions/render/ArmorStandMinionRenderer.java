package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperEntityEquipment;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;

public final class ArmorStandMinionRenderer extends AbstractEntityLibMinionRenderer {

    private static final Vector3f RESTING_ARM_ROTATION = new Vector3f(-15.0F, 0.0F, 10.0F);
    private static final Vector3f MINING_ARM_ROTATION = new Vector3f(-100.0F, 0.0F, 10.0F);

    private static final int SWING_FRAME_COUNT = 10;
    private static final Vector3f[] SWING_FRAMES = createSwingFrames();

    private final MinionTypeService types;
    private final Long2LongOpenHashMap swingStartTicks = new Long2LongOpenHashMap();

    private long currentTick;

    public ArmorStandMinionRenderer(
            EntityLibHologramRenderer holograms,
            MinionEntityIndex entityIndex,
            MinionTypeService types
    ) {
        super(holograms, entityIndex);
        this.types = types;
    }

    private static Vector3f[] createSwingFrames() {
        Vector3f[] frames = new Vector3f[SWING_FRAME_COUNT];

        for (int index = 0; index < SWING_FRAME_COUNT; index++) {
            double progress = (double) index / (SWING_FRAME_COUNT - 1);
            double swingProgress = Math.sin(Math.PI * progress);

            float pitch = (float) (
                    RESTING_ARM_ROTATION.x
                            + (MINING_ARM_ROTATION.x - RESTING_ARM_ROTATION.x)
                            * swingProgress
            );

            frames[index] = new Vector3f(
                    pitch,
                    RESTING_ARM_ROTATION.y,
                    RESTING_ARM_ROTATION.z
            );
        }

        return frames;
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperLivingEntity body = new WrapperLivingEntity(EntityTypes.ARMOR_STAND);
        ArmorStandMeta meta = body.getEntityMeta(ArmorStandMeta.class);

        meta.setSmall(true);
        meta.setHasArms(true);
        meta.setHasNoBasePlate(true);
        meta.setMarker(false);
        meta.setRightArmRotation(RESTING_ARM_ROTATION);

        body.setHasNoGravity(true);

        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        WrapperEntityEquipment equipment = body.getEquipment();
        if (type != null) {
            equipment.setHelmet(equipmentItem(type.helmet()));
            equipment.setChestplate(equipmentItem(type.chestplate()));
            equipment.setLeggings(equipmentItem(type.leggings()));
            equipment.setBoots(equipmentItem(type.boots()));
        }
        equipment.setMainHand(equipmentItem(minion.equipment().tool()));

        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        this.faceTarget(minion, targetYaw);

        if (!this.swingStartTicks.containsKey(minionId)) {
            this.swingStartTicks.put(minionId, this.currentTick);
        }
    }

    @Override
    public void tick(long currentTick) {
        this.currentTick = currentTick;

        ObjectIterator<Long2LongMap.Entry> swings =
                this.swingStartTicks.long2LongEntrySet().fastIterator();

        while (swings.hasNext()) {
            Long2LongMap.Entry swing = swings.next();

            long minionId = swing.getLongKey();
            long elapsedTicks = currentTick - swing.getLongValue();

            RenderedMinion minion = this.renderedMinion(minionId);

            if (minion == null) {
                swings.remove();
                continue;
            }

            ArmorStandMeta meta =
                    minion.body().getEntityMeta(ArmorStandMeta.class);

            if (elapsedTicks >= SWING_FRAME_COUNT) {
                meta.setRightArmRotation(RESTING_ARM_ROTATION);
                swings.remove();
                continue;
            }

            meta.setRightArmRotation(SWING_FRAMES[(int) elapsedTicks]);
        }
    }
}