package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.multification.notice.Notice;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;

@Command(name = "minions")
public final class MinionGiveCommand {

    private final MinionItemFactory items;
    private final MinionBehaviorRegistry behaviors;
    private final MessagesConfig messages;

    public MinionGiveCommand(
        MinionItemFactory items,
        MinionBehaviorRegistry behaviors,
        MessagesConfig messages
    ) {
        this.items = items;
        this.behaviors = behaviors;
        this.messages = messages;
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player) {
        return this.give(player, this.behaviors.defaultBehavior());
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player, @Arg("type") String type) {
        MinionBehavior behavior = this.behaviors.find(type).orElse(null);
        if (behavior == null) {
            return this.messages.minionTypeUnknown;
        }
        return this.give(player, behavior);
    }

    private Notice give(Player player, MinionBehavior behavior) {
        player.getInventory().addItem(this.items.create(behavior));
        return this.messages.minionItemReceived;
    }
}
