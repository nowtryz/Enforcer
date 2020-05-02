package net.nowtryz.enforcer.discord;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.discord.command.abstraction.DiscordCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public abstract class AbstractDiscordBot implements Listener, PluginHolder {
    protected final Enforcer plugin;
    protected final DiscordClient client;
    protected User user;

    public AbstractDiscordBot(Enforcer plugin) {

        this.plugin = plugin;
        this.client = DiscordClientBuilder.create(this.getDiscordProvider().getToken()).build();

        client.getEventDispatcher().on(ReadyEvent.class).subscribe(this::onReady);
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().map(id -> id.equals(this.getDiscordProvider().getGuild())).orElse(false))
                .filter(e -> e.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)
                        && e.getMessage().getContent().isPresent())
                .subscribe(this::onMessage);
    }

    public final void block() {
        this.client.login().block();
    }

    public final void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public final void onReady(ReadyEvent event) {
        this.user = event.getSelf();
        String message = this.plugin.translate("discord.ready", user.getUsername(), user.getDiscriminator());
        String logged = this.plugin.translate("discord.logged-to");
        this.plugin.getLogger().info(message);

        event.getGuilds().stream().parallel()
                .map(ReadyEvent.Guild::getId)
                .map(client::getGuildById)
                .map(Mono::block)
                .filter(Objects::nonNull)
                .filter(server -> server.getId().equals(this.getDiscordProvider().getGuild()))
                .findFirst()
                .map(Guild::getName)
                .ifPresent(name -> this.plugin.getLogger().log(Level.INFO, String.format(logged, name)));

        Bukkit.getScheduler().runTask(this.plugin, this::register);
    }

    public final void onMessage(MessageCreateEvent event) {
        Message message = event.getMessage();

        if (message.getContent().orElse("").matches(".*8=+D.*")) {
            message.getAuthor().ifPresent(author -> message.getChannel()
                .flatMap(channel -> channel.createMessage(this.plugin.translate(
                    "discord.8=D",
                    author.getMention()
                ))).block()
            );
        }

        if (isBotMentioned(message)) {
            this.onBotMention(message);
            return;
        }

        String[] args = message.getContent().orElse("").split(" ");
        if (args.length == 0) return;
        if (args[0].length() <= 2) return;
        if (args[0].charAt(0) != this.getDiscordProvider().getPrefix()) return;
        String command = args[0].substring(1);

        DiscordCommand discordCommand = this.getCommandMap().get(command);
        if (discordCommand != null) discordCommand.execute(this.user, event, args);
    }

    private void onBotMention(Message message) {
        message.getAuthor().ifPresent(author -> message.getChannel().blockOptional().ifPresent(channel -> {
            channel.createMessage(this.plugin.translate(
                "discord.bot-mentionned",
                author.getMention(),
                this.getDiscordProvider().getPrefix())
            ).block();
            this.sendInfo(channel);
        }));
    }

    private boolean isBotMentioned(Message message) {
        return message.getUserMentionIds().contains(this.user.getId());
    }

    public void sendInfo(MessageChannel channel) {
        channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(new Color(this.getDiscordProvider().getEmbedColor()));
            embedCreateSpec.setTitle(this.plugin.translate("discord.available-commands"));
            Arrays.stream(this.getCommands())
                    .filter(DiscordCommand::isEnabled)
                    .forEach(command -> embedCreateSpec.addField(
                            command.getUsage(),
                            command.getDescription(),
                            false
                    ));
        }).block();
    }

    public final DiscordClient getClient() {
        return client;
    }

    @Override
    public final Enforcer getPlugin() {
        return this.plugin;
    }

    protected abstract DiscordCommand[] getCommands();
    protected abstract Map<String, DiscordCommand> getCommandMap();

    public void disable() {
        this.client.logout().block();
    }
}
