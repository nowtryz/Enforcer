package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.discord.command.abstraction.UseArgumentsCommand;

import java.awt.*;

public class MinecraftRegistrationCommand extends AbstractDiscordCommand implements UseArgumentsCommand {
    private final DiscordBot bot;

    public MinecraftRegistrationCommand(DiscordBot bot, Enforcer plugin) {
        super(DiscordBot.REGISTER, plugin);
        this.bot = bot;
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
    public void execute(User bot, MessageCreateEvent event, String[] args) {
        Message message = event.getMessage();
        message.getAuthor().ifPresent(author -> this.registerUser(bot, author, message, args));
    }

    private void registerUser(User bot, User author, Message message, String[] args) {
        if (args.length != 2) {
            message.getChannel()
                    .blockOptional()
                    .ifPresent(channel -> this.sendMissingArgs(bot, this.getUsage(), channel));
            return;
        }

        String username = args[1];
        PlayerInfo playerInfo = this.getPlayersManager().getPlayerInfo(username);

        playerInfo.getDiscordId().flatMap(id -> message.getChannel().blockOptional())
            .ifPresent(channel -> channel.createMessage(this.plugin.translate(
                "discord.already-associated",
                username,
                author.getMention()
            )).block());

        if (!playerInfo.getDiscordId().isPresent()) {
            playerInfo.setDiscordId(author.getId());
            this.bot.grabRole(playerInfo);

            message.getChannel().blockOptional().ifPresent(channel -> channel.createEmbed(embedCreateSpec -> {
                embedCreateSpec.setColor(new Color(this.provider.getEmbedColor()));
                embedCreateSpec.setAuthor(author.getUsername(), "https://mine.ly/" + username, author.getAvatarUrl());
                embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
                embedCreateSpec.setTitle(this.plugin.translate("discord.registered", username));
                this.createFooter(bot, embedCreateSpec);
            }).block());
        }
    }
}
