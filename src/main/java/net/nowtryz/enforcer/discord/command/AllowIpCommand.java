package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.discord.command.abstraction.UseFooterCommand;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import reactor.core.publisher.Mono;

import java.awt.*;

public class AllowIpCommand extends AbstractDiscordCommand implements UseFooterCommand {
    public AllowIpCommand(Enforcer plugin) {
        super(DiscordBot.NEW_IP, plugin);
    }

    @Override
    public boolean isEnabled() {
        return this.getConfig().isFirewallEnabled();
    }

    @Override
    public Translation getDescriptionTranslation() {
        return Translation.DISCORD_CMD_DESC_ALLOW_IP;
    }

    @Override
    public Mono<Message> execute(MessageChannel channel, User bot, User author, MessageCreateEvent event, String[] args) {
        return this.getPlayersManager().getPlayerFromDiscord(author.getId())
                .map(info -> this.allowNewIp(info, channel, bot, author))
                .orElseGet(() -> this.sendError(bot, author, channel));
    }

    private Mono<Message> allowNewIp(PlayerInfo playerInfo, MessageChannel channel, User bot, User author) {
        String username = playerInfo.getPlayer().getName();
        playerInfo.allowNewIp();
        return channel.createEmbed(embedCreateSpec -> {
            this.createFooter(bot, embedCreateSpec);
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            embedCreateSpec.setColor(this.provider.getEmbedColor());
            embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
            embedCreateSpec.setTitle(Translation.DISCORD_IP_ALLOWED.get(username));
        });
    }

    private Mono<Message> sendError(User bot, User author, MessageChannel channel) {
        return channel.createEmbed(embedCreateSpec -> {
            this.createFooter(bot, embedCreateSpec);
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            embedCreateSpec.setColor(Color.red);
            embedCreateSpec.setTitle(Translation.DISCORD_NO_PLAYER_TITLE.get());
            embedCreateSpec.setDescription(Translation.DISCORD_NO_PLAYER_DESC.get());
        });
    }
}
