package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.ConfigProvider;

import java.util.function.Consumer;

public class InfoCommand extends AbstractDiscordCommand {
    private final Consumer<MessageChannel> sendInfo;

    InfoCommand(Enforcer plugin, ConfigProvider provider, Consumer<MessageChannel> sendInfo) {
        super(DiscordMessenger.INFO, plugin, provider);
        this.sendInfo = sendInfo;
    }

    @Override
    public void execute(MessageCreateEvent event, String[] args) {
        event.getMessage().getChannel().blockOptional().ifPresent(this.sendInfo);
    }
}
