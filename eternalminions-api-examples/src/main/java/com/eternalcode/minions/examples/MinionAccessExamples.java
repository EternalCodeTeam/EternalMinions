package com.eternalcode.minions.examples;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.minion.MinionDetails;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionService;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class MinionAccessExamples {

    private final MinionAccessService access;
    private final MinionService minions;

    MinionAccessExamples(MinionAccessService access, MinionService minions) {
        this.access = access;
        this.minions = minions;
    }

    boolean check(CommandSender sender, String[] arguments) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (arguments.length < 3) {
            ExampleMessages.error(sender, "Usage: /minionapi access <id> <OPEN_PANEL|MANAGE|PICK_UP>");
            return true;
        }

        MinionId minionId = ExampleMessages.parseMinionId(sender, arguments[1]).orElse(null);
        if (minionId == null) {
            return true;
        }
        MinionDetails minion = this.minions.findById(minionId).orElse(null);
        if (minion == null) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
            return true;
        }

        try {
            MinionAccessAction action = MinionAccessAction.valueOf(arguments[2].toUpperCase(Locale.ROOT));
            boolean allowed = this.access.canAccess(player, minion, action);
            ExampleMessages.info(sender, "Access for " + action + ": " + allowed);
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, "Unknown access action: " + arguments[2]);
        }
        return true;
    }
}
