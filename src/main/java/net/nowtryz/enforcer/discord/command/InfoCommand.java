package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.i18n.Translation;

import java.util.function.Consumer;

public class InfoCommand extends AbstractDiscordCommand {
    private final Consumer<MessageChannel> sendInfo;

    public InfoCommand(Enforcer plugin, Consumer<MessageChannel> sendInfo) {
        super(DiscordBot.INFO, plugin);
        this.sendInfo = sendInfo;
    }

    @Override
    public Translation getDescriptionTranslation() {
        return Translation.DISCORD_CMD_DESC_INFO;
    }

    @Override
    public void execute(User bot, MessageCreateEvent event, String[] args) {
        event.getMessage().getChannel().blockOptional().ifPresent(this.sendInfo);
    }
}
