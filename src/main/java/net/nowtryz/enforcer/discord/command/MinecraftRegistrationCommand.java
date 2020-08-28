package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.discord.command.abstraction.UseArgumentsCommand;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import reactor.core.publisher.Mono;

import java.awt.*;

public class MinecraftRegistrationCommand extends AbstractDiscordCommand implements UseArgumentsCommand {
    private final DiscordBot bot;

    public MinecraftRegistrationCommand(DiscordBot bot, Enforcer plugin) {
        super(DiscordBot.REGISTER, plugin);
        this.bot = bot;
    }

    @Override
    public Translation getDescriptionTranslation() {
        return Translation.DISCORD_CMD_DESC_REGISTER;
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <username>";
    }

    @Override
    public void execute(User bot, MessageCreateEvent event, String[] args) {
        Message message = event.getMessage();
        Mono.justOrEmpty(message.getAuthor())
                .zipWhen(u -> message.getChannel(), (author, channel) -> this.registerUser(channel, bot, author, args))
                .subscribe();
    }

    private Mono<Message> registerUser(MessageChannel channel, User bot, User author, String[] args) {
        if (args.length != 2) return this.sendMissingArgs(bot, this.getUsage(), channel);

        String username = args[1];
        PlayerInfo playerInfo = this.getPlayersManager().getPlayerInfo(username);

        if (playerInfo.getDiscordId().isPresent()) {
            Snowflake id = playerInfo.getDiscordId().get();
            return this.sendAlreadyAssociated(channel, username, id);
        } else if (!this.getDiscordConfig().isConfirmationRequired()) {
            playerInfo.setDiscordId(author.getId());
            this.bot.grabRole(playerInfo);
            return this.sendRegistered(channel, username, author, bot);
        } else if (this.plugin.getDiscordConfirmationManager().hasRequestPending(author)) {
            return this.sendAlreadyConfirming(channel, author, bot);
        } else {
            Player player = Bukkit.getPlayer(username);
            if (player == null) {
                // Must be online
                return this.sendNoOnline(channel, author, bot);
            } else {
                // is online, sent confirmation message
                this.plugin.getDiscordConfirmationManager().awaitConfirmation(player, author);
                return this.sendConfirmationSent(channel, username, author, bot);
            }
        }
    }

    private Mono<Message> sendAlreadyAssociated(MessageChannel channel, String username, Snowflake id) {
        return channel.createMessage(Translation.DISCORD_ASSOCIATED.get(username, "<@"+ id.asString() +">"));
    }

    private Mono<Message> sendRegistered(MessageChannel channel, String username, User author, User bot) {
        return channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(this.provider.getEmbedColor());
            embedCreateSpec.setAuthor(author.getUsername(), "https://mine.ly/" + username, author.getAvatarUrl());
            embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
            embedCreateSpec.setTitle(Translation.DISCORD_REGISTERED.get(username));
            this.createFooter(bot, embedCreateSpec);
        });
    }

    private Mono<Message> sendAlreadyConfirming(MessageChannel channel, User author, User bot) {
        return channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(Color.RED);
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            embedCreateSpec.setTitle(Translation.DISCORD_ALREADY_CONFIRMING.get());
            this.createFooter(bot, embedCreateSpec);
        });
    }

    private Mono<Message> sendNoOnline(MessageChannel channel, User author, User bot) {
        return channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(Color.RED);
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            embedCreateSpec.setTitle(Translation.DISCORD_MUST_BE_ONLINE.get());
            this.createFooter(bot, embedCreateSpec);
        });
    }

    private Mono<Message> sendConfirmationSent(MessageChannel channel, String username, User author, User bot) {
        return channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(this.getDiscordConfig().getEmbedColor());
            embedCreateSpec.setAuthor(author.getUsername(), null, author.getAvatarUrl());
            embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
            embedCreateSpec.setTitle(Translation.DISCORD_CONFIRMATION_SENT.get(username));
            this.createFooter(bot, embedCreateSpec);
        });
    }
}
