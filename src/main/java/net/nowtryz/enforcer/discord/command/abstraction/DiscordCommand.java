package net.nowtryz.enforcer.discord.command.abstraction;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

public interface DiscordCommand {
    Mono<Message> execute(MessageChannel channel, User bot, User author, MessageCreateEvent event, String[] args);
    String getCommand();
    String getDescription();
    String getUsage();

    default boolean isEnabled() {
        return true;
    }
}
