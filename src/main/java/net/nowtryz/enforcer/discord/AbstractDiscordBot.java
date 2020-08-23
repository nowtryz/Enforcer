package net.nowtryz.enforcer.discord;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.discord.command.abstraction.DiscordCommand;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.tps.TPS;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDiscordBot implements Listener, PluginHolder {
    private Map<String, DiscordCommand> commandMap = new HashMap<>();
    private DiscordCommand[] commands = {};
    private BukkitTask bukkitTask;

    protected final Enforcer plugin;
    protected final DiscordClient client;
    protected User user;


    public AbstractDiscordBot(Enforcer plugin) {

        this.plugin = plugin;
        this.client = DiscordClientBuilder.create(this.getDiscordConfig().getToken()).build();

        client.getEventDispatcher().on(ReadyEvent.class).subscribe(this::onReady);
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().map(id -> id.equals(this.getDiscordConfig().getGuild())).orElse(false))
                .filter(e -> e.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)
                        && e.getMessage().getContent().isPresent())
                .subscribe(this::onMessage);
    }

    public final void block() {
        this.client.login().block();
    }

    public final void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        if (this.getDiscordConfig().doesUpdatePresence()) {
            this.bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::updatePresence, 0, 1200);
        }
    }

    public final void onReady(ReadyEvent event) {
        this.user = event.getSelf();
        this.plugin.getLogger().info(Translation.DISCORD_READY.get(user.getUsername(), user.getDiscriminator()));

        Flux.fromStream(event.getGuilds().stream())
                .map(ReadyEvent.Guild::getId)
                .flatMap(client::getGuildById)
                .filter(Objects::nonNull)
                .filter(server -> server.getId().equals(this.getDiscordConfig().getGuild()))
                .next()
                .map(Guild::getName)
                .map(Translation.DISCORD_LOGGED::get)
                .doOnNext(this.plugin.getLogger()::info)
                .subscribe(unused -> Bukkit.getScheduler().runTask(this.plugin, this::register));
    }

    public final void onMessage(MessageCreateEvent event) {
        Message message = event.getMessage();

        if (message.getContent().orElse("").matches(".*8=+D.*")) {
            message.getAuthor().ifPresent(author -> message.getChannel()
                .flatMap(channel -> channel.createMessage(Translation.DISCORD_8D.get(author.getMention())))
                .subscribe()
            );
        }

        if (isBotMentioned(message)) {
            this.onBotMention(message);
            return;
        }

        String[] args = message.getContent().orElse("").split(" ");
        if (args.length == 0) return;
        if (args[0].length() <= 2) return;
        if (args[0].charAt(0) != this.getDiscordConfig().getPrefix()) return;
        String command = args[0].substring(1);

        Optional.ofNullable(this.commandMap.get(command))
                .filter(DiscordCommand::isEnabled)
                .ifPresent(c -> c.execute(this.user, event, args));
    }

    private void onBotMention(Message message) {
        this.plugin.getLogger().info("mentioned");
        message.getAuthor().ifPresent(author -> message.getChannel()
                .flatMapMany(channel -> Flux.merge(
                        channel.createMessage(Translation.DISCORD_MENTIONED.get(
                            author.getMention(),
                            this.getDiscordConfig().getPrefix()
                        )),
                        this.sendInfo(channel)
                )).subscribe()
        );
    }

    protected void updatePresence() {
        this.client.updatePresence(Presence.online(Activity.playing(Translation.DISCORD_PRESENCE_LEGACY.get(
                TPS.getTPS(),
                Bukkit.getServer().getOnlinePlayers().size(),
                Bukkit.getServer().getMaxPlayers()
        )))).subscribe();
    }

    private boolean isBotMentioned(Message message) {
        return message.getUserMentionIds().contains(this.user.getId());
    }

    public Mono<Message> sendInfo(MessageChannel channel) {
        return channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(this.getDiscordConfig().getEmbedColor());
            embedCreateSpec.setTitle(Translation.DISCORD_AVAILABLE_COMMANDS.get());
            Arrays.stream(this.commands)
                    .filter(DiscordCommand::isEnabled)
                    .forEach(command -> embedCreateSpec.addField(
                            command.getUsage(),
                            command.getDescription(),
                            false
                    ));
        });
    }

    @Override
    public final Enforcer getPlugin() {
        return this.plugin;
    }

    public final DiscordCommand[] getCommands() {
        return this.commands;
    }

    public final Map<String, DiscordCommand> getCommandMap() {
        return this.commandMap;
    }
    protected final void setCommands(DiscordCommand[] commands) {
        this.commands = commands;
        this.commandMap = Arrays.stream(commands)
                .collect(Collectors.toMap(DiscordCommand::getCommand, Function.identity()));
    }

    public void disable() {
        Optional.ofNullable(this.bukkitTask).ifPresent(BukkitTask::cancel);
        this.client.logout().block();
    }
}
