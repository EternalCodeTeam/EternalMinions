package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.notice.NoticeService;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.command.CommandSender;

public final class InvalidUsageHandler implements dev.rollczi.litecommands.invalidusage.InvalidUsageHandler<CommandSender> {

    private final NoticeService noticeService;
    private final MessagesConfig messages;

    public InvalidUsageHandler(NoticeService noticeService, MessagesConfig messages) {
        this.noticeService = noticeService;
        this.messages = messages;
    }

    @Override
    public void handle(
        Invocation<CommandSender> invocation,
        InvalidUsage<CommandSender> invalidUsage,
        ResultHandlerChain<CommandSender> chain
    ) {
        Schematic schematic = invalidUsage.getSchematic();
        if (schematic.isOnlyFirst()) {
            this.send(invocation.sender(), this.messages.correctUsage, schematic.first());
            return;
        }

        this.noticeService.create()
            .viewer(invocation.sender())
            .notice(this.messages.correctUsageHead)
            .send();

        for (String usage : schematic.all()) {
            this.send(invocation.sender(), this.messages.correctUsageEntry, usage);
        }
    }

    private void send(CommandSender sender, com.eternalcode.multification.notice.Notice notice, String usage) {
        this.noticeService.create()
            .viewer(sender)
            .notice(notice)
            .placeholder("{USAGE}", usage)
            .send();
    }
}
