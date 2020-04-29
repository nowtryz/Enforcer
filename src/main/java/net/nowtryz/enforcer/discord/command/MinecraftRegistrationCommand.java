package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.ConfigProvider;

import java.awt.*;
import java.util.function.BiConsumer;

class MinecraftRegistrationCommand extends AbstractDiscordCommand {
    private final BiConsumer<PlayerInfo, Snowflake> roleUpdater;

    public MinecraftRegistrationCommand(Enforcer plugin, ConfigProvider provider, BiConsumer<PlayerInfo, Snowflake> roleUpdater) {
        super(DiscordMessenger.REGISTER, plugin, provider);
        this.roleUpdater = roleUpdater;
    }

    @Override
    public String getDescriptionKey() {
        return "register";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <username>";
    }

    @Override
    public void execute(MessageCreateEvent event, String[] args) {
        Message message = event.getMessage();
        message.getAuthor().ifPresent(author -> this.registerUser(author, message, args));
    }

    private void registerUser(User author, Message message, String[] args) {
        if (args.length != 2) {
            message.getChannel()
                    .blockOptional()
                    .ifPresent(channel -> this.sendMissingArgs(this.getUsage(), channel));
            return;
        }

        String username = args[1];
        PlayerInfo playerInfo = this.plugin.getPlayersManager().getPlayerInfo(username);

        playerInfo.getDiscordId()
                .ifPresent(id -> message.
                        getChannel()
                        .blockOptional()
                        .ifPresent(channel -> channel.createMessage(this.plugin.translate(
                                "discord.already-associated",
                                username,
                                id.asLong()
                        )).block())
                );

        if (!playerInfo.getDiscordId().isPresent()) {
            playerInfo.setDiscordId(author.getId());
            this.roleUpdater.accept(playerInfo, author.getId());

            message.getChannel().blockOptional().ifPresent(channel -> channel.createEmbed(embedCreateSpec -> {
                embedCreateSpec.setColor(new Color(this.provider.getEmbedColor()));
                embedCreateSpec.setAuthor(author.getUsername(), "https://mine.ly/" + username, author.getAvatarUrl());
                embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
                embedCreateSpec.setTitle(this.plugin.translate("discord.registered", username));
            }).block());
        }
    }
}
