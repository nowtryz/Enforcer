package net.nowtryz.enforcer.discord;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.command.DiscordMessenger;
import org.apache.commons.lang3.Validate;
import org.bukkit.entity.Player;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public class DiscordBot {
    private final Enforcer plugin;
    private final DiscordClient client;
    private final DiscordMessenger messenger;
    private final ConfigProvider provider;

    public DiscordBot(Enforcer plugin) {
        this.plugin = plugin;
        this.provider = new ConfigProvider(plugin);
        this.client = DiscordClientBuilder.create(this.provider.getToken()).build();
        this.messenger = new DiscordMessenger(plugin, provider, this::grabRole);

        client.getEventDispatcher().on(ReadyEvent.class).subscribe(this::onReady);
        client.getEventDispatcher().on(MemberUpdateEvent.class)
                .filter(event -> event.getGuildId().equals(this.provider.getGuild()))
                .subscribe(this::onRoleUpdate);
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().map(id -> id.equals(this.provider.getGuild())).orElse(false))
                .filter(e -> e.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)
                        && e.getMessage().getContent().isPresent())
                .subscribe(this.messenger::onMessage);
    }

    public void block() {
        this.client.login().block();
    }

    public void onRoleUpdate(MemberUpdateEvent event) {
        this.plugin.getPlayersManager().getPlayerFromDiscord(event.getMemberId())
            // Only update online players
            .filter(playerInfo -> playerInfo.getPlayer().map(Player::isOnline).orElse(false))
            .ifPresent(playerInfo ->
                this.grabRole(playerInfo, event.getMemberId())
            );
    }

    public void onReady(ReadyEvent event) {
        User user = event.getSelf();
        String message = this.plugin.translate("discord.ready", user.getUsername(), user.getDiscriminator());
        String logged = this.plugin.translate("discord.logged-to");
        this.messenger.setUser(user);
        this.plugin.getLogger().info(message);

        event.getGuilds().stream().parallel()
                .map(ReadyEvent.Guild::getId)
                .map(client::getGuildById)
                .map(Mono::block)
                .filter(Objects::nonNull)
                .filter(server -> server.getId().equals(this.provider.getGuild()))
                .findFirst()
                .map(Guild::getName)
                .ifPresent(name -> this.plugin.getLogger().log(Level.INFO, String.format(logged, name)));
    }

    public void grabRole(PlayerInfo playerInfo, Snowflake userId) {
        this.client.getMemberById(this.provider.getGuild(), userId)
            .blockOptional()
            .map(Member::getHighestRole)
            .flatMap(Mono::blockOptional)
            .map(Role::getName)
            .map(this::convertGroup)
            .ifPresent(role -> {
                playerInfo.hydrate();
                Optional<String> oldRole = playerInfo.getDiscordRole().map(this::convertGroup);
                playerInfo.setDiscordRole(role);
                playerInfo.getPlayer().ifPresent(player -> {
                    Permission vaultPermission = this.plugin.getVaultPermission();

                    if (
                        vaultPermission.playerInGroup(null, player, role) // no need to update player if already in group
                        && oldRole.map(s -> s.equals(role)).orElse(false) // avoid inheritance
                    ) return;

                    // Remove olf group
                    oldRole.map(s -> vaultPermission.playerRemoveGroup(null, player, s))
                        .ifPresent(b -> {
                            if (!b) this.plugin.getLogger().severe(String.format(
                                "Unable to remove group '%s' from %s",
                                oldRole.get(),
                                playerInfo.getPlayerName()
                            ));
                        });

                    // try to add to new group
                    if (vaultPermission.playerAddGroup(null, player, role)) {
                        this.plugin.getLogger().info(String.format(
                            "%s has been put into %s",
                            player.getName(),
                            role
                        ));
                    } else {
                        // put back in old group if operation failed
                        oldRole.ifPresent(playerInfo::setDiscordRole);
                        // if the operation fail, we try to figure out if it is because there is no group corresponding
                        // to the Discord role
                        List<String> groups = Arrays.asList(vaultPermission.getGroups());
                        if (!groups.contains(role)) {
                            this.plugin.getLogger().severe(String.format("Cannot find %s in server groups", role));
                            this.plugin.getLogger().info("Available groups are: " + String.join(", ", groups));
                        } else this.plugin.getLogger()
                                .severe(String.format("unable to grant %s to %s", role, playerInfo.getPlayerName()));
                    }
                });
            });
    }

    private String convertGroup(String role) {
        Validate.notNull(role, "role");
        return role.toLowerCase().replace(' ', '-');
    }

    public void onDisable() {
        this.client.logout();
    }
}
