package com.eternalcode.minions.examples;

import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.status.MinionStatusService;
import org.bukkit.command.CommandSender;

final class MinionStatusExamples {

    private final MinionStatusService statuses;

    MinionStatusExamples(MinionStatusService statuses) {
        this.statuses = statuses;
    }

    boolean update(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            ExampleMessages.error(sender, "Usage: /minionapi status <id> [KEY|clear]");
            return true;
        }
        MinionId minionId = ExampleMessages.parseMinionId(sender, arguments[1]).orElse(null);
        if (minionId == null) {
            return true;
        }

        if (arguments.length == 2) {
            String status = this.statuses.findStatus(minionId).orElse(null);
            if (status == null) {
                ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
                return true;
            }
            ExampleMessages.info(sender, "Current status: " + status);
            return true;
        }

        try {
            boolean changed = arguments[2].equalsIgnoreCase("clear")
                ? this.statuses.clearStatus(minionId)
                : this.statuses.setStatus(minionId, arguments[2].toUpperCase(java.util.Locale.ROOT));
            ExampleMessages.info(sender, changed ? "Status changed." : "Status was unchanged or minion was absent.");
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }
}
