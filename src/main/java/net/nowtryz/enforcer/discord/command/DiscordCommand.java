package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;

interface DiscordCommand {
    void execute(MessageCreateEvent event, String[] args);
    String getCommand();
    String getDescription();
    String getUsage();

    default boolean isEnabled() {
        return true;
    }
}
