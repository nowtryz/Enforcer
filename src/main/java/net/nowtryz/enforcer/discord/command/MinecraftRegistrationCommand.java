package net.nowtryz.enforcer.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.abstraction.AbstractDiscordCommand;
import net.nowtryz.enforcer.discord.command.abstraction.UseArgumentsCommand;

import java.util.Optional;

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

        if (args.length != 2) {
            message.getChannel()
                    .blockOptional()
                    .ifPresent(channel -> this.sendMissingArgs(bot, this.getUsage(), channel));
            return;
        }

        message.getAuthor().ifPresent(author -> this.registerUser(bot, author, message, args));
    }

    private void registerUser(User bot, User author, Message message, String[] args) {
        String username = args[1];
        PlayerInfo playerInfo = this.getPlayersManager().getPlayerInfo(username);

        if (playerInfo.getDiscordId().isPresent()) {
            Snowflake id = playerInfo.getDiscordId().get();
            message.getChannel()
                    .flatMap(channel -> channel.createMessage(Translation.DISCORD_ASSOCIATED.get(username, "<@"+ id.asString() +">")))
                    .subscribe();
        } else if (this.getDiscordConfig().isConfirmationRequired()) {

        } else {
            playerInfo.setDiscordId(author.getId());
            this.bot.grabRole(playerInfo);

            message.getChannel()
                    .map(channel -> channel.createEmbed(embedCreateSpec -> {
                        embedCreateSpec.setColor(this.provider.getEmbedColor());
                        embedCreateSpec.setAuthor(author.getUsername(), "https://mine.ly/" + username, author.getAvatarUrl());
                        embedCreateSpec.setThumbnail(String.format("https://minotar.net/helm/%s/100.png", username));
                        embedCreateSpec.setTitle(Translation.DISCORD_REGISTERED.get(username));
                        this.createFooter(bot, embedCreateSpec);
                    })).subscribe();
        }
    }
}
