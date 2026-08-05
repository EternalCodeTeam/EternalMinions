package com.eternalcode.minions.examples;

import com.eternalcode.minions.minion.MinionDetails;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.minion.MinionSnapshot;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class MinionQueryExamples {

    private final MinionService minions;

    MinionQueryExamples(MinionService minions) {
        this.minions = minions;
    }

    boolean showOwned(CommandSender sender) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Collection<MinionDetails> owned = this.minions.findByOwner(player.getUniqueId());
        ExampleMessages.info(sender, "Loaded minions owned by you: " + owned.size());
        for (MinionDetails minion : owned) {
            ExampleMessages.info(
                sender,
                "#" + minion.id().value() + " " + minion.behaviorId() + " level " + minion.level()
            );
        }
        return true;
    }

    boolean showSnapshot(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            ExampleMessages.error(sender, "Usage: /minionapi snapshot <id>");
            return true;
        }

        MinionId minionId = ExampleMessages.parseMinionId(sender, arguments[1]).orElse(null);
        if (minionId == null) {
            return true;
        }

        MinionSnapshot snapshot = this.minions.findSnapshotById(minionId).orElse(null);
        if (snapshot == null) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
            return true;
        }

        ExampleMessages.info(sender, "Behavior: " + snapshot.details().behaviorId());
        ExampleMessages.info(sender, "Owner: " + snapshot.details().ownerId());
        ExampleMessages.info(sender, "Level/progress: " + snapshot.details().level() + "/" + snapshot.progress());
        ExampleMessages.info(sender, "Direction/status: " + snapshot.direction() + "/" + snapshot.statusKey());
        ExampleMessages.info(sender, "Storage capacity: " + snapshot.storageCapacity());
        ExampleMessages.info(sender, "Upgrades: " + snapshot.upgradeTiers());
        return true;
    }

    boolean showAt(CommandSender sender) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Location location = player.getLocation();
        MinionPosition position = new MinionPosition(
            location.getWorld().getKey().asString(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        MinionDetails minion = this.minions.findAt(position).orElse(null);
        if (minion == null) {
            ExampleMessages.info(sender, "No minion occupies your current block.");
            return true;
        }

        ExampleMessages.success(sender, "Found minion #" + minion.id().value());
        return true;
    }
}
