package com.eternalcode.minions.render;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.particles.XParticle;
import com.eternalcode.minions.minion.Minion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.UUID;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class RotatingHeadMinionRenderer extends AbstractEntityLibMinionRenderer {

    private static final Quaternion4f[] ROTATIONS = rotations();
    private final Server server;
    private final double animationDistanceSquared;

    public RotatingHeadMinionRenderer(
        EntityLibHologramRenderer holograms,
        MinionEntityIndex entityIndex,
        Server server,
        int animationDistanceBlocks
    ) {
        super(holograms, entityIndex);
        this.server = server;
        this.animationDistanceSquared = (double) animationDistanceBlocks * animationDistanceBlocks;
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperEntity body = new WrapperEntity(EntityTypes.ITEM_DISPLAY);
        ItemDisplayMeta meta = body.getEntityMeta(ItemDisplayMeta.class);
        meta.setItem(SpigotConversionUtil.fromBukkitItemStack(XMaterial.PLAYER_HEAD.parseItem()));
        meta.setDisplayType(ItemDisplayMeta.DisplayType.HEAD);
        meta.setScale(new Vector3f(0.55F, 0.55F, 0.55F));
        meta.setTranslation(new Vector3f(0.0F, 0.55F, 0.0F));
        meta.setWidth(0.8F);
        meta.setHeight(0.8F);
        meta.setTransformationInterpolationDuration(4);
        body.setHasNoGravity(true);
        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        minion.body().getEntityMeta(ItemDisplayMeta.class).setGlowColorOverride(0x55FFAA);
        Location location = minion.body().getLocation();
        for (UUID viewerId : minion.body().getViewers()) {
            Player player = this.server.getPlayer(viewerId);
            if (player == null) {
                continue;
            }

            double xDistance = player.getX() - location.getX();
            double yDistance = player.getY() - location.getY();
            double zDistance = player.getZ() - location.getZ();
            if (xDistance * xDistance + yDistance * yDistance + zDistance * zDistance > this.animationDistanceSquared) {
                continue;
            }

            for (int step = 1; step <= 5; step++) {
                player.spawnParticle(
                    XParticle.END_ROD.get(),
                    location.getX(),
                    location.getY() + 0.65D,
                    location.getZ() + step * 0.2D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
                );
            }
        }
    }

    @Override
    public void tick(long currentTick) {
        if ((currentTick & 3L) != 0L) {
            return;
        }

        Quaternion4f rotation = ROTATIONS[(int) ((currentTick >>> 2) & (ROTATIONS.length - 1))];
        for (RenderedMinion minion : this.renderedMinions()) {
            if (minion.body().getViewers().isEmpty()) {
                continue;
            }
            minion.body().getEntityMeta(ItemDisplayMeta.class).setLeftRotation(rotation);
        }
    }

    private static Quaternion4f[] rotations() {
        Quaternion4f[] rotations = new Quaternion4f[64];
        for (int index = 0; index < rotations.length; index++) {
            double angle = (Math.PI * 2.0D * index) / rotations.length;
            rotations[index] = new Quaternion4f(0.0F, (float) Math.sin(angle / 2.0D), 0.0F, (float) Math.cos(angle / 2.0D));
        }
        return rotations;
    }
}
