package net.nowtryz.enforcer.discord;

import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.command.AllowIpCommand;
import net.nowtryz.enforcer.discord.command.InfoCommand;
import net.nowtryz.enforcer.discord.command.MinecraftRegistrationCommand;
import net.nowtryz.enforcer.discord.command.abstraction.DiscordCommand;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import net.nowtryz.enforcer.util.GroupUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiscordBot extends AbstractDiscordBot {
    public final static String REGISTER = "mc", NEW_IP = "newip", INFO = "info"; // TODO put in enum

    public DiscordBot(Enforcer plugin) {
        super(plugin);

        this.setCommands(new DiscordCommand[]{
                new MinecraftRegistrationCommand(this, plugin),
                new InfoCommand(plugin, super::sendInfo),
                new AllowIpCommand(plugin)
        });

        client.getEventDispatcher().on(MemberUpdateEvent.class)
                .filter(event -> event.getGuildId().equals(this.getDiscordConfig().getGuild()))
                .map(MemberUpdateEvent::getMemberId)
                .map(this.plugin.getPlayersManager()::getPlayerFromDiscord)
                .flatMap(Mono::justOrEmpty)
                .subscribe(this::grabRole);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            this.plugin.getEnableLatch().await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.getLogger().severe(e.getMessage());
            return;
        }

        this.grabRole(this.plugin.getPlayersManager().getPlayerInfo(event.getUniqueId()));
    }

    public void grabRole(PlayerInfo playerInfo) {
        playerInfo.getDiscordId().ifPresent(userId -> this.client
                .getMemberById(this.getDiscordConfig().getGuild(), userId)
                .flatMapMany(Member::getRoles)
                .map(Role::getId)
                .map(this.getDiscordConfig()::getGroupForRole)
                .flatMap(Mono::justOrEmpty)
                .collectList()
                .subscribe(roles -> this.updatePlayerGroups(playerInfo, roles)));
    }

    private void updatePlayerGroups(PlayerInfo playerInfo, List<String> roles) {
        playerInfo.hydrate();
        OfflinePlayer player = playerInfo.getPlayer();
        Permission vaultPermission = this.plugin.getVaultPermission();

        // Remove old groups
        this.getDiscordConfig()
                .getRoleToGroup()
                .values()
                .stream()
                .filter(s -> !roles.contains(s))
                .filter(group -> vaultPermission.playerInGroup(null, player, group))
                .forEach(group -> {
                    if (vaultPermission.playerRemoveGroup(null, player, group)) {
                        this.plugin.getLogger().info(String.format(
                                "%s has been removed from the group %s",
                                player.getName(),
                                group
                        ));
                    } else this.plugin.getLogger().severe(
                            String.format("Unable to remove group '%s' from %s", group, player.getName())
                    );
                });

        roles.stream()
                .filter(group -> !vaultPermission.playerInGroup(null, player, group))
                // try to add to new group
                .filter(group -> vaultPermission.playerAddGroup(null, player, group))
                .forEach(group -> {
                    this.plugin.getLogger().info(String.format(
                            "%s has been put into group %s",
                            player.getName(),
                            group
                    ));

                    // if player is online
                    Optional.ofNullable(player.getPlayer()).ifPresent(p -> Translation.ADDED_TO_GROUP.send(p, group));
                });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        this.publishGroups(
                this.plugin.getPlayersManager().getPlayerInfo(event.getPlayer().getUniqueId()),
                event.getPlayer()
        );
    }

    public void publishGroups(PlayerInfo playerInfo, Player player) {
        Permission vaultPermission = this.getVaultPermission();
        this.getLogger().info("player groups: " + Arrays.toString(vaultPermission.getPlayerGroups(player)));
        List<Snowflake> rolesToAdd = Stream.of(vaultPermission.getPlayerGroups(player))
                .map(GroupUtil::parse)
                .map(this.getDiscordConfig()::getRoleForGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        playerInfo.getDiscordId().ifPresent(discordId -> this.client
                .getMemberById(this.getDiscordConfig().getGuild(), discordId)
                .subscribe(member -> {
                    Set<Snowflake> roleIds = member.getRoleIds();

                    this.getLogger().info("roles to add: " + rolesToAdd);
                    this.getLogger().info(player.getName() + "'s roles: " + roleIds);

                    // Add roles
                    rolesToAdd.stream()
                            .filter(id -> !roleIds.contains(id))
                            .forEach(id -> member.addRole(id, "Added by enforcer").subscribe());

                    // Remove old roles
                    this.getDiscordConfig()
                            .getGroupToRole()
                            .values()
                            .stream()
                            .filter(roleIds::contains)
                            .filter(id -> !rolesToAdd.contains(id))
                            .forEach(id -> member.removeRole(id, "Removed by Enforcer")
                                .subscribe((unused) -> this.getLogger().info(String.format(
                                    "role with id " + id + " as been removed from %s's discord account",
                                    id, player.getName()
                                )))
                            );
                })
        );
    }
}
