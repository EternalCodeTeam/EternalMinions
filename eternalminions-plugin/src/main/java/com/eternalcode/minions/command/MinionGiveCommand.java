package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.MinionBehaviorType;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.multification.notice.Notice;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.Locale;
import org.bukkit.entity.Player;

@Command(name = "minions")
public final class MinionGiveCommand {

    private final MinionItemFactory items;
    private final MinionTypeService types;
    private final MessagesConfig messages;

    public MinionGiveCommand(MinionItemFactory items, MinionTypeService types, MessagesConfig messages) {
        this.items = items;
        this.types = types;
        this.messages = messages;
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player) {
        return this.give(player, this.types.defaultType());
    }

    @Execute(name = "give")
    @Permission("eternalminions.command.give")
    public Notice give(@Context Player player, @Arg("type") MinionBehaviorType type) {
        MinionType minionType = this.types.type(type.name().toLowerCase(Locale.ROOT)).orElse(null);
        if (minionType == null) {
            return this.messages.minionTypeUnknown;
        }
        return this.give(player, minionType);
    }

    private Notice give(Player player, MinionType type) {
        player.getInventory().addItem(this.items.create(type));
        return this.messages.minionItemReceived;
    }
}
