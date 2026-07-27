package com.eternalcode.minions.minion.limit;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.MinionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class PlayerMinionLimitService {

    private static final String LIMIT_PERMISSION_PREFIX = "eternalminions.limit.";
    private static final String UNLIMITED_PERMISSION = "eternalminions.limit.*";

    private final MinionRegistry registry;
    private final MinionsConfig.LimitsConfig config;

    public PlayerMinionLimitService(MinionRegistry registry, MinionsConfig.LimitsConfig config) {
        this.registry = registry;
        this.config = config;
    }

    public MinionLimitStatus statusFor(Player player) {
        int current = this.registry.countByOwner(player.getUniqueId());
        if (player.hasPermission(UNLIMITED_PERMISSION)) {
            return new MinionLimitStatus(current, Integer.MAX_VALUE, true);
        }
        return new MinionLimitStatus(current, this.maxAllowed(player), false);
    }

    private int maxAllowed(Player player) {
        int highest = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }

            String permission = info.getPermission();
            if (!permission.startsWith(LIMIT_PERMISSION_PREFIX) || permission.equals(UNLIMITED_PERMISSION)) {
                continue;
            }

            int value = parseNonNegativeInt(permission.substring(LIMIT_PERMISSION_PREFIX.length()));
            if (value > highest) {
                highest = value;
            }
        }
        return highest < 0 ? Math.max(0, this.config.defaultLimit) : highest;
    }

    private static int parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? -1 : parsed;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
