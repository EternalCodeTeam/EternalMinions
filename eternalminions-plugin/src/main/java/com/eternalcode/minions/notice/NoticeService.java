package com.eternalcode.minions.notice;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.multification.adventure.AudienceConverter;
import com.eternalcode.multification.paper.PaperMultification;
import com.eternalcode.multification.translation.TranslationProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class NoticeService extends PaperMultification<MessagesConfig> {

    private final MessagesConfig messages;
    private final MiniMessage miniMessage;

    public NoticeService(MessagesConfig messages, MiniMessage miniMessage) {
        this.messages = messages;
        this.miniMessage = miniMessage;
    }

    @Override
    protected @NotNull TranslationProvider<MessagesConfig> translationProvider() {
        return locale -> this.messages;
    }

    @Override
    protected @NotNull ComponentSerializer<Component, Component, String> serializer() {
        return this.miniMessage;
    }

    @Override
    protected @NotNull AudienceConverter<CommandSender> audienceConverter() {
        return commandSender -> commandSender;
    }
}
