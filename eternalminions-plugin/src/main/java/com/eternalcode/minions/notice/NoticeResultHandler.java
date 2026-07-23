package com.eternalcode.minions.notice;

import com.eternalcode.multification.notice.Notice;
import dev.rollczi.litecommands.handler.result.ResultHandler;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import org.bukkit.command.CommandSender;

public final class NoticeResultHandler implements ResultHandler<CommandSender, Notice> {

    private final NoticeService noticeService;

    public NoticeResultHandler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public void handle(
        Invocation<CommandSender> invocation,
        Notice notice,
        ResultHandlerChain<CommandSender> chain
    ) {
        this.noticeService.create()
            .viewer(invocation.sender())
            .notice(notice)
            .send();
    }
}
