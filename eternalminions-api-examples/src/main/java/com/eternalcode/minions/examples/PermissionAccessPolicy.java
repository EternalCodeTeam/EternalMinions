package com.eternalcode.minions.examples;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.access.MinionAccessPolicy;
import com.eternalcode.minions.minion.MinionDetails;
import org.bukkit.entity.Player;

public final class PermissionAccessPolicy implements MinionAccessPolicy {

    @Override
    public boolean canAccess(Player player, MinionDetails minion, MinionAccessAction action) {
        return player.hasPermission("eternalminions.examples.manage-any");
    }
}
