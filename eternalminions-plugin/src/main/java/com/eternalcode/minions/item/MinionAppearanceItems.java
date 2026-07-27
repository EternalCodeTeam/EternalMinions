package com.eternalcode.minions.item;

import com.cryptomorin.xseries.XMaterial;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MinionArmorPieceConfig;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class MinionAppearanceItems {

    private final Server server;

    public MinionAppearanceItems(Server server) {
        this.server = server;
    }

    public ItemStack head(AbstractMinionConfig config) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        String texture = resolveHeadTexture(config);
        if (texture.isEmpty()) {
            return head;
        }

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        UUID profileId = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = this.server.createProfile(profileId, "minion");
        profile.setProperty(new ProfileProperty("textures", texture));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    public ItemStack helmet(AbstractMinionConfig config) {
        return this.armor(config.items.helmet, this.head(config));
    }

    public ItemStack chestplate(AbstractMinionConfig config) {
        return this.armor(config.items.chestplate, null);
    }

    public ItemStack leggings(AbstractMinionConfig config) {
        return this.armor(config.items.leggings, null);
    }

    public ItemStack boots(AbstractMinionConfig config) {
        return this.armor(config.items.boots, null);
    }

    // The ARMOR_STAND renderer only ever shows this head texture (never npcSkin directly), so if an
    // admin configures only npcSkin - the field meant for the NPC renderer - it still shows up here
    // instead of silently falling back to a blank Steve head.
    private static String resolveHeadTexture(AbstractMinionConfig config) {
        String helmetTexture = config.items.helmet.texture;
        return helmetTexture.isEmpty() ? config.npcSkin : helmetTexture;
    }

    private ItemStack armor(MinionArmorPieceConfig piece, ItemStack head) {
        XMaterial material = piece.type;
        if (material == null || material == XMaterial.AIR) {
            return null;
        }
        if (material == XMaterial.PLAYER_HEAD && head != null) {
            return head;
        }

        ItemStack item = material.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Material is unavailable: " + material);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(piece.color);
        }
        meta.setEnchantmentGlintOverride(piece.glow);
        item.setItemMeta(meta);
        return item;
    }
}
