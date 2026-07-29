package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

abstract class AbstractEntityLibMinionRenderer implements MinionRenderer {

    private final Long2ObjectOpenHashMap<RenderedMinion> rendered = new Long2ObjectOpenHashMap<>();
    private final EntityLibHologramRenderer holograms;
    private final MinionEntityIndex entityIndex;

    AbstractEntityLibMinionRenderer(EntityLibHologramRenderer holograms, MinionEntityIndex entityIndex) {
        this.holograms = holograms;
        this.entityIndex = entityIndex;
    }

    static com.github.retrooper.packetevents.protocol.item.ItemStack equipmentItem(ItemStack tool) {
        return tool == null
                ? com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY
                : SpigotConversionUtil.fromBukkitItemStack(tool);
    }

    @Override
    public final void show(Player player, Minion minion) {
        RenderedMinion view = this.rendered.get(minion.id().value());
        if (view == null) {
            WrapperEntity body = this.createBody(minion);
            WrapperEntity hologram = this.holograms.create(minion);
            view = new RenderedMinion(body, hologram);
            this.rendered.put(minion.id().value(), view);
            this.entityIndex.register(body.getEntityId(), minion.id());
        }

        view.body().addViewer(player.getUniqueId());
        view.hologram().addViewer(player.getUniqueId());

        if (!view.body().isSpawned()) {
            view.body().spawn(RenderLocation.of(minion, 0.0D));
            view.hologram().spawn(this.holograms.location(minion));
        }
    }

    @Override
    public final void hide(Player player, MinionId minionId) {
        RenderedMinion view = this.rendered.get(minionId.value());
        if (view == null) {
            return;
        }

        view.body().removeViewer(player.getUniqueId());
        view.hologram().removeViewer(player.getUniqueId());
    }

    @Override
    public final void remove(MinionId minionId) {
        RenderedMinion view = this.rendered.remove(minionId.value());
        if (view == null) {
            return;
        }

        this.entityIndex.remove(view.body().getEntityId());
        view.body().remove();
        view.hologram().remove();
    }

    @Override
    public final void animate(MinionId minionId, float targetYaw) {
        RenderedMinion view = this.rendered.get(minionId.value());
        if (view != null) {
            this.animate(minionId.value(), view, targetYaw);
        }
    }

    @Override
    public final void refreshEquipment(MinionId minionId, ItemStack tool) {
        RenderedMinion view = this.rendered.get(minionId.value());
        if (view == null || !(view.body() instanceof WrapperLivingEntity body)) {
            return;
        }

        body.getEquipment().setMainHand(equipmentItem(tool));
        body.getEquipment().refresh();
    }

    @Override
    public final void refreshHologram(Minion minion) {
        RenderedMinion view = this.rendered.get(minion.id().value());
        if (view != null) {
            view.hologram().getEntityMeta(TextDisplayMeta.class).setText(this.holograms.text(minion));
        }
    }

    @Override
    public final void refreshRotation(Minion minion) {
        RenderedMinion view = this.rendered.get(minion.id().value());
        if (view != null) {
            this.faceTarget(view, minion.settings().direction().yaw());
        }
    }

    final void faceTarget(RenderedMinion minion, float targetYaw) {
        if (Float.isNaN(targetYaw)) {
            return;
        }

        WrapperEntity body = minion.body();
        body.getLocation().setYaw(targetYaw);
        body.getLocation().setPitch(0.0F);
        body.sendPacketsToViewersIfSpawned(
                new WrapperPlayServerEntityRotation(body.getEntityId(), targetYaw, 0.0F, true),
                new WrapperPlayServerEntityHeadLook(body.getEntityId(), targetYaw)
        );
    }

    final Iterable<RenderedMinion> renderedMinions() {
        return this.rendered.values();
    }

    final RenderedMinion renderedMinion(long minionId) {
        return this.rendered.get(minionId);
    }

    abstract WrapperEntity createBody(Minion minion);

    abstract void animate(long minionId, RenderedMinion minion, float targetYaw);
}
