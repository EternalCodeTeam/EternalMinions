package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.Minion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class EntityLibHologramRenderer {

    public WrapperEntity create(Minion minion) {
        WrapperEntity hologram = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = hologram.getEntityMeta(TextDisplayMeta.class);
        meta.setText(Component.text("Minion #" + minion.id().value(), NamedTextColor.GREEN));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setSeeThrough(true);
        meta.setLineWidth(160);
        hologram.setHasNoGravity(true);
        return hologram;
    }

    public Location location(Minion minion) {
        return RenderLocation.of(minion, 1.75D);
    }
}
