package com.eternalcode.minions.examples;

import com.eternalcode.minions.minion.MinionCreateRequest;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionManagementService;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionSnapshot;
import com.eternalcode.minions.minion.MinionService;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class MinionManagementExamples {

    private final MinionManagementService management;
    private final MinionService minions;

    MinionManagementExamples(MinionManagementService management, MinionService minions) {
        this.management = management;
        this.minions = minions;
    }

    boolean create(CommandSender sender, String[] arguments) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (arguments.length < 2) {
            ExampleMessages.error(sender, "Usage: /minionapi create <behavior>");
            return true;
        }

        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            ExampleMessages.error(sender, "Look at a block within six blocks.");
            return true;
        }
        Block placement = target.getRelative(BlockFace.UP);
        if (!placement.isEmpty() || !placement.getRelative(BlockFace.UP).isEmpty()) {
            ExampleMessages.error(sender, "The two blocks above the target must be empty.");
            return true;
        }

        MinionCreateRequest request = new MinionCreateRequest(
            player.getUniqueId(),
            arguments[1],
            this.position(placement.getLocation()),
            MinionDirection.fromYaw(player.getYaw())
        );

        try {
            MinionSnapshot created = this.management.create(request);
            ExampleMessages.success(sender, "Created minion #" + created.details().id().value());
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    boolean remove(CommandSender sender, String[] arguments) {
        MinionId minionId = this.parseId(sender, arguments, "/minionapi remove <id>");
        if (minionId == null) {
            return true;
        }

        if (!this.management.remove(minionId)) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
            return true;
        }
        ExampleMessages.success(sender, "Removed minion #" + minionId.value());
        return true;
    }

    boolean rotate(CommandSender sender, String[] arguments) {
        MinionId minionId = this.parseId(sender, arguments, "/minionapi rotate <id>");
        if (minionId == null) {
            return true;
        }

        MinionSnapshot current = this.requireSnapshot(sender, minionId);
        if (current == null) {
            return true;
        }
        Optional<MinionSnapshot> updated = this.management.setDirection(
            minionId,
            current.direction().rotated()
        );
        ExampleMessages.success(sender, "Direction: " + updated.orElseThrow().direction());
        return true;
    }

    boolean setChest(CommandSender sender, String[] arguments) {
        if (arguments.length < 3) {
            ExampleMessages.error(sender, "Usage: /minionapi chest <id> <here|clear>");
            return true;
        }
        MinionId minionId = this.parseId(sender, arguments, "");
        if (minionId == null) {
            return true;
        }

        MinionPosition position = null;
        if (arguments[2].equalsIgnoreCase("here")) {
            Player player = ExampleMessages.requirePlayer(sender);
            if (player == null) {
                return true;
            }
            position = this.position(player.getLocation());
        }
        else if (!arguments[2].equalsIgnoreCase("clear")) {
            ExampleMessages.error(sender, "Use 'here' or 'clear'.");
            return true;
        }

        if (this.management.setChestPosition(minionId, position).isEmpty()) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
            return true;
        }
        ExampleMessages.success(sender, position == null ? "Chest link cleared." : "Chest position set.");
        return true;
    }

    boolean setTool(CommandSender sender, String[] arguments) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        MinionId minionId = this.parseId(sender, arguments, "/minionapi tool <id>");
        if (minionId == null) {
            return true;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemStack replacement = tool.getType().isAir() ? null : tool;
        if (this.management.setTool(minionId, replacement).isEmpty()) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
            return true;
        }
        ExampleMessages.success(sender, replacement == null ? "Tool cleared." : "Tool copied from main hand.");
        return true;
    }

    boolean setStorage(CommandSender sender, String[] arguments) {
        Player player = ExampleMessages.requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (arguments.length < 3) {
            ExampleMessages.error(sender, "Usage: /minionapi storage <id> <slot>");
            return true;
        }
        MinionId minionId = this.parseId(sender, arguments, "");
        if (minionId == null) {
            return true;
        }

        try {
            int slot = Integer.parseInt(arguments[2]);
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            ItemStack replacement = heldItem.getType().isAir() ? null : heldItem;
            if (this.management.setStorageItem(minionId, slot, replacement).isEmpty()) {
                ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
                return true;
            }
            ExampleMessages.success(sender, "Storage slot " + slot + " updated.");
        }
        catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    boolean setUpgrade(CommandSender sender, String[] arguments) {
        if (arguments.length < 4) {
            ExampleMessages.error(sender, "Usage: /minionapi upgrade <id> <kind> <tier>");
            return true;
        }
        MinionId minionId = this.parseId(sender, arguments, "");
        if (minionId == null) {
            return true;
        }

        try {
            int tier = Integer.parseInt(arguments[3]);
            if (this.management.setUpgradeTier(minionId, arguments[2], tier).isEmpty()) {
                ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
                return true;
            }
            ExampleMessages.success(sender, "Upgrade " + arguments[2] + " set to tier " + tier);
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    boolean setProgress(CommandSender sender, String[] arguments) {
        if (arguments.length < 4) {
            ExampleMessages.error(sender, "Usage: /minionapi progress <id> <level> <value>");
            return true;
        }
        MinionId minionId = this.parseId(sender, arguments, "");
        if (minionId == null) {
            return true;
        }

        try {
            int level = Integer.parseInt(arguments[2]);
            long progress = Long.parseLong(arguments[3]);
            if (this.management.setProgress(minionId, level, progress).isEmpty()) {
                ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
                return true;
            }
            ExampleMessages.success(sender, "Progress updated.");
        }
        catch (IllegalArgumentException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    private MinionId parseId(CommandSender sender, String[] arguments, String usage) {
        if (arguments.length < 2) {
            ExampleMessages.error(sender, usage);
            return null;
        }
        return ExampleMessages.parseMinionId(sender, arguments[1]).orElse(null);
    }

    private MinionSnapshot requireSnapshot(CommandSender sender, MinionId minionId) {
        MinionSnapshot snapshot = this.minions.findSnapshotById(minionId).orElse(null);
        if (snapshot == null) {
            ExampleMessages.error(sender, "No loaded minion with id " + minionId.value());
        }
        return snapshot;
    }

    private MinionPosition position(Location location) {
        return new MinionPosition(
            location.getWorld().getKey().asString(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}
