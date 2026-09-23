package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.behavior.MinionBehaviorRegistry;
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
        return this.give(player, this.behaviors.defaultBehavior(), 1);
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player, @Arg("type") String type) {
        return this.give(player, type, 1);
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player, @Arg("type") String type, @Arg("amount") int amount) {
        MinionBehavior behavior = this.behaviors.find(type).orElse(null);
        if (behavior == null) {
            return this.messages.minionTypeUnknown;
        }
        return this.give(player, behavior, amount);
    }

    private Notice give(Player player, MinionBehavior behavior, int amount) {
        player.getInventory().addItem(this.items.create(behavior, amount));
        return this.messages.minionItemReceived;
    }
}
