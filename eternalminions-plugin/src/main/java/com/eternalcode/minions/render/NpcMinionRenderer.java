package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import me.tofaa.entitylib.meta.types.MannequinMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperEntityEquipment;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class NpcMinionRenderer extends AbstractEntityLibMinionRenderer {

    private static final double DEFAULT_SCALE = 0.55D;

    private final MinionTypeService types;

    public NpcMinionRenderer(EntityLibHologramRenderer holograms, MinionEntityIndex entityIndex, MinionTypeService types) {
        super(holograms, entityIndex);
        this.types = types;
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperLivingEntity body = new WrapperLivingEntity(EntityTypes.MANNEQUIN);
        MannequinMeta meta = body.getEntityMeta(MannequinMeta.class);
        meta.setImmovable(true);
        body.setHasNoGravity(true);

        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        body.getAttributes().setAttribute(Attributes.SCALE, type == null ? DEFAULT_SCALE : type.npcScale());
        WrapperEntityEquipment equipment = body.getEquipment();
        if (type != null) {
            if (!type.headTexture().isEmpty()) {
                meta.setProfile(createSkinProfile(type.headTexture()));
            }
            this.equipArmor(equipment, type);
        }
        equipment.setMainHand(equipmentItem(minion.equipment().tool()));
        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        this.faceTarget(minion, targetYaw);
        ((WrapperLivingEntity) minion.body()).swingMainHand();
    }

    private void equipArmor(WrapperEntityEquipment equipment, MinionType type) {
        // A PLAYER_HEAD helmet would cover the NPC skin with a block model, so the skin stays the head.
        ItemStack helmet = type.helmet();
        if (helmet == null || helmet.getType() != Material.PLAYER_HEAD) {
            equipment.setHelmet(equipmentItem(helmet));
        }
        equipment.setChestplate(equipmentItem(type.chestplate()));
        equipment.setLeggings(equipmentItem(type.leggings()));
        equipment.setBoots(equipmentItem(type.boots()));
    }

    private static ItemProfile createSkinProfile(String headTexture) {
        UUID profileId = UUID.nameUUIDFromBytes(headTexture.getBytes(StandardCharsets.UTF_8));
        return new ItemProfile("minion", profileId, List.of(new ItemProfile.Property("textures", headTexture, null)));
    }
}
