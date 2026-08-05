package com.eternalcode.minions.examples;

import com.eternalcode.minions.item.MinionItemService;
import com.eternalcode.minions.item.MinionItemSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class MinionItemExamples {

    private final MinionItemService items;

    MinionItemExamples(MinionItemService items) {
        this.items = items;
    }

    boolean give(CommandSender sender, String[] arguments) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (arguments.length < 2) {
            ExampleMessages.error(sender, "Usage: /minionapi give <behavior>");
            return true;
        }

        try {
            this.giveOrDrop(player, this.items.create(arguments[1]));
            ExampleMessages.success(sender, "Created a " + arguments[1] + " minion item.");
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    boolean inspect(CommandSender sender) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        MinionItemSnapshot snapshot = this.items.inspect(item).orElse(null);
        if (snapshot == null) {
            ExampleMessages.error(sender, "The item in your main hand is not a minion item.");
            return true;
        }

        ExampleMessages.info(sender, "Behavior: " + snapshot.behaviorId());
        ExampleMessages.info(sender, "Level/progress: " + snapshot.level() + "/" + snapshot.progress());
        ExampleMessages.info(sender, "Stored slots: " + snapshot.storageContents().size());
        ExampleMessages.info(sender, "Upgrades: " + snapshot.upgradeTiers());
        return true;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        for (ItemStack overflow : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }
}
