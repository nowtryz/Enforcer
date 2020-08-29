package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.i18n.Translation;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

public class InfoCommand extends AbstractDiscordCommand {
    private final Function<MessageChannel, Mono<Message>> sendInfo;

    public InfoCommand(Enforcer plugin, Function<MessageChannel, Mono<Message>> sendInfo) {
        super(DiscordBot.INFO, plugin);
        this.sendInfo = sendInfo;
    }

    @Override
    public Translation getDescriptionTranslation() {
        return Translation.DISCORD_CMD_DESC_INFO;
    }

    @Override
    public Mono<Message> execute(MessageChannel channel, User bot, User author, MessageCreateEvent event, String[] args) {
        return this.sendInfo.apply(channel);
    }
}
