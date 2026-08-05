package com.eternalcode.minions.examples;

import com.eternalcode.minions.minion.MinionId;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class ExampleMessages {

    private ExampleMessages() {
    }

    static void success(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    static void info(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.AQUA));
    }

    static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    static void usage(CommandSender sender) {
        info(sender, "/minionapi behaviors | owned | snapshot <id> | at");
        info(sender, "/minionapi give <behavior> | inspect | create <behavior>");
        info(sender, "/minionapi remove <id> | rotate <id> | chest <id> <here|clear>");
        info(sender, "/minionapi tool <id> | storage <id> <slot>");
        info(sender, "/minionapi upgrade <id> <kind> <tier> | progress <id> <level> <value>");
        info(sender, "/minionapi status <id> [KEY|clear]");
        info(sender, "/minionapi access <id> <action> | shop <on|off|status>");
    }

    static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        error(sender, "This example requires a player.");
        return null;
    }

    static Optional<MinionId> parseMinionId(CommandSender sender, String rawId) {
        try {
            return Optional.of(new MinionId(Long.parseLong(rawId)));
        }
        catch (IllegalArgumentException exception) {
            error(sender, "Invalid minion id: " + rawId);
            return Optional.empty();
        }
    }
}
