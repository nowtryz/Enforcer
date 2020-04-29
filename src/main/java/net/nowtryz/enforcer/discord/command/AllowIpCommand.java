package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.ConfigProvider;

import java.awt.*;
import java.util.Optional;

public class AllowIpCommand extends AbstractDiscordCommand {
    AllowIpCommand(Enforcer plugin, ConfigProvider provider) {
        super(DiscordMessenger.NEW_IP, plugin, provider);
    }

    @Override
    public String getDescriptionKey() {
        return "allow-ip";
    }

    @Override
    public void execute(MessageCreateEvent event, String[] args) {
        Message message = event.getMessage();
        message.getAuthor().ifPresent(author -> allowNewIp(message, author));
    }

    private void allowNewIp(Message message, User author) {
        Optional<PlayerInfo> playerInfo = this.plugin.getPlayersManager().getPlayerFromDiscord(author.getId());
        playerInfo.ifPresent(PlayerInfo::allowNewIp);
        message.getChannel().blockOptional().ifPresent(channel -> sendConfirmation(author, playerInfo, channel));
    }

    private void sendConfirmation(User author, Optional<PlayerInfo> playerInfo, MessageChannel channel) {
        channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            if (playerInfo.isPresent()) {
                String username = playerInfo.get().getPlayerName();
                embedCreateSpec.setColor(new Color(this.provider.getEmbedColor()));
                embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
                embedCreateSpec.setTitle(this.plugin.translate(
                        "discord.new-ip-allowed",
                        username)
                );
            } else {
                embedCreateSpec.setColor(Color.red);
                embedCreateSpec.setTitle(this.plugin.translate("discord.cannot-get-player.title"));
                embedCreateSpec.setDescription(this.plugin.translate("discord.cannot-get-player.desc"));
            }
        }).block();
    }
}
