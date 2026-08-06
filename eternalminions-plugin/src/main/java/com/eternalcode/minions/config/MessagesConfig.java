package com.eternalcode.minions.config;

import com.eternalcode.multification.notice.Notice;
import java.nio.file.Path;

public final class MessagesConfig extends ConfigurationFile {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("messages.yml");
    }

    public Notice noPermission = Notice.chat(prefix() + "<red>You do not have permission to use this command.");
    public Notice playerNotFound = Notice.chat(prefix() + "<red>Player not found.");
    public Notice playerOnly = Notice.chat(prefix() + "<red>Only players can use this command.");
    public Notice correctUsage = Notice.chat(prefix() + "<white>Correct usage: <green>{USAGE}");
    public Notice correctUsageHead = Notice.chat(prefix() + "<white>Correct usage:");
    public Notice correctUsageEntry = Notice.chat("<dark_gray>➤</dark_gray> <green>{USAGE}");
    public Notice reloadCompleted = Notice.chat(prefix() + "<green>Configuration reloaded successfully.");

    public Notice minionToolUpdated = Notice.chat(prefix() + "<green>Minion tool updated.");
    public Notice minionStorageCollected = Notice.chat(prefix() + "<green>Items collected from the minion's storage.");
    public Notice minionPickedUp = Notice.chat(
        prefix() + "<green>Minion picked up. <gray>({MINION_LIMIT_CURRENT}/{MINION_LIMIT_MAX})"
    );
    public Notice minionOwnerRequired = Notice.chat(prefix() + "<red>Only the owner can manage this minion.");
    public Notice minionNotFound = Notice.chat(prefix() + "<red>This minion no longer exists.");
    public Notice minionPlaced = Notice.chat(
        prefix() + "<green>Minion placed. <gray>({MINION_LIMIT_CURRENT}/{MINION_LIMIT_MAX})"
    );
    public Notice minionItemReceived = Notice.chat(prefix() + "<green>Minion item received.");
    public Notice minionPlacementBlocked = Notice.chat(prefix() + "<red>You cannot place a minion here.");
    public Notice minionTypeUnknown = Notice.chat(prefix() + "<red>Unknown minion type.");
    public Notice minionLimitReached = Notice.chat(
        prefix() + "<red>You have reached your minion limit ({MINION_LIMIT_CURRENT}/{MINION_LIMIT_MAX})."
    );
    public Notice upgradePurchased = Notice.chat(prefix() + "<green>Upgrade purchased.");
    public Notice upgradeMaxed = Notice.chat(prefix() + "<red>This upgrade is already at its maximum tier.");
    public Notice upgradeRequiresLevel = Notice.chat(prefix() + "<red>This minion's level is too low for that upgrade.");
    public Notice upgradeCannotAfford = Notice.chat(prefix() + "<red>You cannot afford this upgrade, or the economy is unavailable.");

    public Notice chestLinkStart = Notice.chat(prefix() + "<white>Right-click a chest to link it to this minion.");
    public Notice chestLinked = Notice.chat(prefix() + "<green>Chest linked to the minion.");
    public Notice chestUnlinked = Notice.chat(prefix() + "<green>Chest unlinked from the minion.");
    public Notice chestLinkTooFar = Notice.chat(prefix() + "<red>That chest is too far away from the minion.");
    public Notice chestLinkExpired = Notice.chat(prefix() + "<red>Chest selection timed out.");

    public Notice minionRotated = Notice.chat(prefix() + "<green>Minion rotated.");

    private static String prefix() {
        return "<b><gradient:#FACC15:#FFE15F:#FACC15>ᴍɪɴɪᴏɴꜱ</gradient></b> <dark_gray>➤</dark_gray> ";
    }
}
