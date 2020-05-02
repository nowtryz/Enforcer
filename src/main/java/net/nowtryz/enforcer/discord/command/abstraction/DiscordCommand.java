package net.nowtryz.enforcer.discord.command.abstraction;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;

public interface DiscordCommand {
    void execute(User bot, MessageCreateEvent event, String[] args);
    String getCommand();
    String getDescription();
    String getUsage();

    default boolean isEnabled() {
        return true;
    }
}
