package com.eternalcode.minions.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.config.MessagesConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ReloadCommandTest {

    @Test
    void reloadsConfigurationsAndReturnsConfiguredNotice() {
        AtomicBoolean reloaded = new AtomicBoolean();
        MessagesConfig messages = new MessagesConfig();
        ReloadCommand command = new ReloadCommand(() -> reloaded.set(true), messages);

        assertThat(command.reload()).isSameAs(messages.reloadCompleted);
        assertThat(reloaded).isTrue();
    }
}
