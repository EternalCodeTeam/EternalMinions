package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.notice.NoticeService;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.permission.MissingPermissions;
import org.bukkit.command.CommandSender;

public final class MissingPermissionHandler implements dev.rollczi.litecommands.permission.MissingPermissionsHandler<CommandSender> {

    private final NoticeService noticeService;
    private final MessagesConfig messages;

    public MissingPermissionHandler(NoticeService noticeService, MessagesConfig messages) {
        this.noticeService = noticeService;
        this.messages = messages;
    }

    @Override
    public void handle(
        Invocation<CommandSender> invocation,
        MissingPermissions missingPermissions,
        ResultHandlerChain<CommandSender> chain
    ) {
        this.noticeService.create()
            .viewer(invocation.sender())
            .notice(this.messages.noPermission)
            .placeholder("{PERMISSION}", missingPermissions.asJoinedText())
            .send();
    }
}
