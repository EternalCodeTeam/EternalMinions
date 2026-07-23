package com.eternalcode.minions.render;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.Minion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.types.MannequinMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;

public final class NpcMinionRenderer extends AbstractEntityLibMinionRenderer {

    public NpcMinionRenderer(EntityLibHologramRenderer holograms, MinionEntityIndex entityIndex) {
        super(holograms, entityIndex);
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperLivingEntity body = new WrapperLivingEntity(EntityTypes.MANNEQUIN);
        MannequinMeta meta = body.getEntityMeta(MannequinMeta.class);
        meta.setImmovable(true);
        body.setHasNoGravity(true);
        body.getAttributes().setAttribute(Attributes.SCALE, 0.55D);
        body.getEquipment().setMainHand(SpigotConversionUtil.fromBukkitItemStack(XMaterial.DIAMOND_PICKAXE.parseItem()));
        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        this.faceTarget(minion, targetYaw);
        ((WrapperLivingEntity) minion.body()).swingMainHand();
    }
}
