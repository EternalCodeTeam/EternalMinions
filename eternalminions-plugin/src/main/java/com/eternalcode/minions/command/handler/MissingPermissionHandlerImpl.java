package com.eternalcode.minions.command.handler;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.notice.NoticeService;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.permission.MissingPermissions;
import dev.rollczi.litecommands.permission.MissingPermissionsHandler;
import org.bukkit.command.CommandSender;

public final class MissingPermissionHandlerImpl implements MissingPermissionsHandler<CommandSender> {

    private final NoticeService noticeService;
    private final MessagesConfig messages;

    public MissingPermissionHandlerImpl(NoticeService noticeService, MessagesConfig messages) {
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
