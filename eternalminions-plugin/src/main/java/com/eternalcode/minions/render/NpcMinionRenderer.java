package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
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

    private final MinionBehaviorRegistry behaviors;
    private final MinionAppearanceItems appearance;

    public NpcMinionRenderer(
        EntityLibHologramRenderer holograms,
        MinionEntityIndex entityIndex,
        MinionBehaviorRegistry behaviors,
        MinionAppearanceItems appearance
    ) {
        super(holograms, entityIndex);
        this.behaviors = behaviors;
        this.appearance = appearance;
    }

    @Override
    WrapperEntity createBody(Minion minion) {
        WrapperLivingEntity body = new WrapperLivingEntity(EntityTypes.MANNEQUIN);
        MannequinMeta meta = body.getEntityMeta(MannequinMeta.class);
        meta.setImmovable(true);
        body.setHasNoGravity(true);

        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        body.getAttributes().setAttribute(
            Attributes.SCALE,
            behavior == null ? DEFAULT_SCALE : behavior.config().npcScale
        );
        WrapperEntityEquipment equipment = body.getEquipment();
        if (behavior != null) {
            String headTexture = behavior.config().items.helmet.texture;
            String skin = behavior.config().npcSkin.isEmpty() ? headTexture : behavior.config().npcSkin;
            if (!skin.isEmpty()) {
                meta.setProfile(createSkinProfile(skin));
            }
            this.equipArmor(equipment, behavior);
        }
        equipment.setMainHand(equipmentItem(minion.equipment().tool()));
        return body;
    }

    @Override
    void animate(long minionId, RenderedMinion minion, float targetYaw) {
        this.faceTarget(minion, targetYaw);
        ((WrapperLivingEntity) minion.body()).swingMainHand();
    }

    private void equipArmor(WrapperEntityEquipment equipment, MinionBehavior behavior) {
        ItemStack helmet = this.appearance.helmet(behavior.config());
        if (helmet == null || helmet.getType() != Material.PLAYER_HEAD) {
            equipment.setHelmet(equipmentItem(helmet));
        }
        equipment.setChestplate(equipmentItem(this.appearance.chestplate(behavior.config())));
        equipment.setLeggings(equipmentItem(this.appearance.leggings(behavior.config())));
        equipment.setBoots(equipmentItem(this.appearance.boots(behavior.config())));
    }

    private static ItemProfile createSkinProfile(String headTexture) {
        UUID profileId = UUID.nameUUIDFromBytes(headTexture.getBytes(StandardCharsets.UTF_8));
        return new ItemProfile("minion", profileId, List.of(new ItemProfile.Property("textures", headTexture, null)));
    }
}
