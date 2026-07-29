package com.eternalcode.minions.render;

import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperEntityEquipment;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;

public final class ArmorStandMinionRenderer extends AbstractEntityLibMinionRenderer {

    private static final Vector3f RESTING_ARM_ROTATION = new Vector3f(
            -15.0F, 0.0F, 10.0F
    );

    private static final Vector3f WIND_UP_ARM_ROTATION = new Vector3f(
            5.0F, -3.0F, 16.0F
    );

    private static final Vector3f IMPACT_ARM_ROTATION = new Vector3f(
            -105.0F, 3.0F, -4.0F
    );

    private static final int WIND_UP_FRAME_COUNT = 2;
    private static final int STRIKE_FRAME_COUNT = 4;
    private static final int RECOVERY_FRAME_COUNT = 6;

    private static final int SWING_FRAME_COUNT = WIND_UP_FRAME_COUNT
            + STRIKE_FRAME_COUNT
            + RECOVERY_FRAME_COUNT;

    private static final int SWING_DURATION_TICKS = SWING_FRAME_COUNT;

    private static final int FRAME_BITS = 4;
    private static final long FRAME_MASK = (1L << FRAME_BITS) - 1L;
    private static final int NO_FRAME = -1;

    private static final Vector3f[] SWING_FRAMES = createSwingFrames();

    private final MinionBehaviorRegistry behaviors;
    private final MinionAppearanceItems appearance;
    private final Long2LongOpenHashMap swingStates = new Long2LongOpenHashMap();
    private final Set<Object> touchedChannels = new HashSet<>();
    private final ProtocolManager protocolManager = PacketEvents.getAPI().getProtocolManager();

    private long currentTick;

    public ArmorStandMinionRenderer(
            EntityLibHologramRenderer holograms,
            MinionEntityIndex entityIndex,
            MinionBehaviorRegistry behaviors,
            MinionAppearanceItems appearance
    ) {
        super(holograms, entityIndex);
        this.behaviors = behaviors;
        this.appearance = appearance;
    }

    private static Vector3f[] createSwingFrames() {
        Vector3f[] frames = new Vector3f[SWING_FRAME_COUNT];

        int frameIndex = 0;

        for (int index = 0; index < WIND_UP_FRAME_COUNT; index++) {
            double progress = (index + 1.0D) / WIND_UP_FRAME_COUNT;

            frames[frameIndex++] = interpolateRotation(
                    RESTING_ARM_ROTATION,
                    WIND_UP_ARM_ROTATION,
                    smoothStep(progress)
            );
        }

        for (int index = 0; index < STRIKE_FRAME_COUNT; index++) {
            double progress = (index + 1.0D) / STRIKE_FRAME_COUNT;

            frames[frameIndex++] = interpolateRotation(
                    WIND_UP_ARM_ROTATION,
                    IMPACT_ARM_ROTATION,
                    smoothStep(progress)
            );
        }

        for (int index = 0; index < RECOVERY_FRAME_COUNT; index++) {
            double progress = (index + 1.0D) / (RECOVERY_FRAME_COUNT + 1.0D);

            frames[frameIndex++] = interpolateRotation(
                    IMPACT_ARM_ROTATION,
                    RESTING_ARM_ROTATION,
                    smoothStep(progress)
            );
        }

        return frames;
    }

    private static Vector3f interpolateRotation(
            Vector3f from,
            Vector3f to,
            double progress
    ) {
        return new Vector3f(
                interpolate(from.x, to.x, progress),
                interpolate(from.y, to.y, progress),
                interpolate(from.z, to.z, progress)
        );
    }

    private static float interpolate(float from, float to, double progress) {
        return (float) (from + (to - from) * progress);
    }

    private static double smoothStep(double progress) {
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static ArmorStandMeta armorStandMeta(RenderedMinion minion) {
        return minion.body().getEntityMeta(ArmorStandMeta.class);
    }

    private static long packState(long startTick, int lastFrame) {
        return (startTick << FRAME_BITS) | (lastFrame + 1L);
    }

    private static long unpackStartTick(long state) {
        return state >>> FRAME_BITS;
    }

    private static int unpackLastFrame(long state) {
        return (int) (state & FRAME_MASK) - 1;
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperLivingEntity body = new WrapperLivingEntity(EntityTypes.ARMOR_STAND);
        ArmorStandMeta meta = body.getEntityMeta(ArmorStandMeta.class);

        meta.setSmall(true);
        meta.setHasArms(true);
        meta.setHasNoBasePlate(true);
        meta.setRightArmRotation(RESTING_ARM_ROTATION);

        body.setHasNoGravity(true);

        WrapperEntityEquipment equipment = body.getEquipment();
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);

        if (behavior != null) {
            equipment.setHelmet(equipmentItem(this.appearance.helmet(behavior.config())));
            equipment.setChestplate(equipmentItem(this.appearance.chestplate(behavior.config())));
            equipment.setLeggings(equipmentItem(this.appearance.leggings(behavior.config())));
            equipment.setBoots(equipmentItem(this.appearance.boots(behavior.config())));
        }

        equipment.setMainHand(equipmentItem(minion.equipment().tool()));

        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        this.faceTarget(minion, targetYaw);

        this.swingStates.putIfAbsent(
                minionId,
                packState(this.currentTick, NO_FRAME)
        );
    }

    @Override
    public void tick(long currentTick) {
        this.currentTick = currentTick;

        if (this.swingStates.isEmpty()) {
            return;
        }

        ObjectIterator<Long2LongMap.Entry> iterator =
                this.swingStates.long2LongEntrySet().fastIterator();

        while (iterator.hasNext()) {
            Long2LongMap.Entry entry = iterator.next();

            long minionId = entry.getLongKey();
            long state = entry.getLongValue();

            long startTick = unpackStartTick(state);
            long elapsedTicks = currentTick - startTick;

            if (elapsedTicks < 0) {
                entry.setValue(packState(currentTick, NO_FRAME));
                continue;
            }

            RenderedMinion minion = this.renderedMinion(minionId);

            if (minion == null) {
                iterator.remove();
                continue;
            }

            if (elapsedTicks >= SWING_DURATION_TICKS) {
                this.queueArmRotation(minion, RESTING_ARM_ROTATION);
                iterator.remove();
                continue;
            }

            int frameIndex = (int) (
                    elapsedTicks
                            * SWING_FRAME_COUNT
                            / SWING_DURATION_TICKS
            );

            int previousFrame = unpackLastFrame(state);

            if (frameIndex == previousFrame) {
                continue;
            }

            this.queueArmRotation(minion, SWING_FRAMES[frameIndex]);
            entry.setValue(packState(startTick, frameIndex));
        }

        this.flushTouchedChannels();
    }

    private void queueArmRotation(RenderedMinion minion, Vector3f rotation) {
        ArmorStandMeta meta = armorStandMeta(minion);

        meta.getMetadata().setNotifyAboutChanges(false);
        meta.setRightArmRotation(rotation);

        PacketWrapper<?> packet = meta.createPacket();

        for (UUID viewer : minion.body().getViewers()) {
            Object channel = this.protocolManager.getChannel(viewer);
            if (channel == null) {
                continue;
            }

            this.protocolManager.writePacket(channel, packet);
            this.touchedChannels.add(channel);
        }
    }

    private void flushTouchedChannels() {
        if (this.touchedChannels.isEmpty()) {
            return;
        }

        for (Object channel : this.touchedChannels) {
            ChannelHelper.flush(channel);
        }

        this.touchedChannels.clear();
    }
}
