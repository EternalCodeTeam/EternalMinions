package com.eternalcode.minions.command;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.multification.notice.Notice;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;

@Command(name = "minions")
public final class ReloadCommand {

    private final Runnable reload;
    private final MessagesConfig messages;

    public ReloadCommand(Runnable reload, MessagesConfig messages) {
        this.reload = reload;
        this.messages = messages;
    }

    @Execute(name = "reload")
    @Permission("eternalminions.command.reload")
    public Notice reload() {
        this.reload.run();
        return this.messages.reloadCompleted;
    }
}
