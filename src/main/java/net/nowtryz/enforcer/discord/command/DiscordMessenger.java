package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.ConfigProvider;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscordMessenger {
    public final static String REGISTER = "mc", NEW_IP = "newip", INFO = "info";
    private final Map<String, DiscordCommand> commandMap;
    private DiscordCommand[] commands;
    private final ConfigProvider provider;
    private User user;
    private final Enforcer plugin;


    public DiscordMessenger(Enforcer plugin, ConfigProvider provider, BiConsumer<PlayerInfo, Snowflake> roleUpdater) {
        this.plugin = plugin;
        this.provider = provider;

        this.commands = new DiscordCommand[]{
                new MinecraftRegistrationCommand(plugin, provider, roleUpdater),
                new AllowIpCommand(plugin, provider),
                new InfoCommand(plugin, provider, this::sendInfo)
        };

        this.commandMap = Arrays.stream(this.commands)
                .collect(Collectors.toMap(DiscordCommand::getCommand, Function.identity()));
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void onMessage(MessageCreateEvent event) {
        Message message = event.getMessage();

        if (message.getContent().orElse("").matches(".*8=+D.*")) {
            message.getAuthor().ifPresent(author ->
                message.getChannel().blockOptional().ifPresent(channel ->
                        channel.createMessage(this.plugin.translate(
                        "discord.8=D",
                        author.getId().asLong())).block()
                )
            );
        }

        if (isBotMentioned(message)) {
            this.onBotMention(message);
            return;
        }

        String[] args = message.getContent().orElse("").split(" ");
        if (args.length == 0) return;
        if (args[0].length() <= 2) return;
        if (args[0].charAt(0) != this.provider.getPrefix()) return;
        String command = args[0].substring(1);

        DiscordCommand discordCommand = this.commandMap.get(command);
        if (discordCommand != null) discordCommand.execute(event, args);
    }

    private void onBotMention(Message message) {
        if (!message.getAuthor().isPresent()) return;
        User author = message.getAuthor().get();
        message.getChannel().blockOptional().ifPresent(channel -> {
            channel.createMessage(this.plugin.translate(
                    "discord.bot-mentionned",
                    author.getId().asLong(),
                    this.provider.getPrefix())).block();
            this.sendInfo(channel);
        });
    }

    private void sendInfo(MessageChannel channel) {
        channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(new Color(this.provider.getEmbedColor()));
            embedCreateSpec.setTitle(this.plugin.translate("discord.available-commands"));
            Arrays.stream(this.commands)
                    .filter(DiscordCommand::isEnabled)
                    .forEach(command -> embedCreateSpec.addField(
                    command.getUsage(),
                    command.getDescription(),
                    false
            ));
        }).block();
    }

    private boolean isBotMentioned(Message message) {
        Snowflake botId = this.user.getId();
        return message
                .getUserMentions()
                .toStream()
                .map(User::getId)
                .anyMatch(botId::equals);
    }
}
