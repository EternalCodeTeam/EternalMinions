package com.eternalcode.minions.item;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MinionItemFactory {

    private final NamespacedKey minionKey;

    public MinionItemFactory(Plugin plugin) {
        this.minionKey = new NamespacedKey(plugin, "minion");
    }

    public ItemStack create() {
        ItemStack item = XMaterial.PLAYER_HEAD.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Minion górnik", NamedTextColor.GREEN));
        meta.lore(List.of(Component.text("Kliknij blok PPM, aby postawić.", NamedTextColor.DARK_GRAY)));
        meta.getPersistentDataContainer().set(this.minionKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMinion(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Byte value = item.getPersistentDataContainer().get(this.minionKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }
}
