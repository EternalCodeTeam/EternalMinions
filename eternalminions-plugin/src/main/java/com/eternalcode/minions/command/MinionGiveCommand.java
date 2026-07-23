package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.multification.notice.Notice;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;

@Command(name = "minions")
public final class MinionGiveCommand {

    private final MinionItemFactory items;
    private final MessagesConfig messages;

    public MinionGiveCommand(MinionItemFactory items, MessagesConfig messages) {
        this.items = items;
        this.messages = messages;
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player) {
        player.getInventory().addItem(this.items.create());
        return this.messages.minionItemReceived;
    }
}
