package net.nowtryz.enforcer.discord;

import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.playermanager.PlayerInfo;
import net.nowtryz.enforcer.discord.command.AllowIpCommand;
import net.nowtryz.enforcer.discord.command.InfoCommand;
import net.nowtryz.enforcer.discord.command.MinecraftRegistrationCommand;
import net.nowtryz.enforcer.discord.command.abstraction.DiscordCommand;
import org.apache.commons.lang3.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
                .subscribe(this::onRoleUpdate);
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

    public void onRoleUpdate(MemberUpdateEvent event) {
        this.plugin.getPlayersManager().getPlayerFromDiscord(event.getMemberId())
            // Only update online players
            .filter(playerInfo -> playerInfo.getPlayer().isOnline())
            .ifPresent(this::grabRole);
    }

    public void grabRole(PlayerInfo playerInfo) {
        playerInfo.getDiscordId()
            .map(userId -> this.client.getMemberById(this.getDiscordConfig().getGuild(), userId))
            .flatMap(Mono::blockOptional)
            .map(Member::getHighestRole)
            .flatMap(Mono::blockOptional)
            .map(Role::getName)
            .map(this::convertGroup)
            .ifPresent(role -> this.updatePlayerGroup(playerInfo, role));
    }

    private void updatePlayerGroup(PlayerInfo playerInfo, String role) {
        playerInfo.hydrate();
        Optional<String> oldRole = playerInfo.getDiscordRole().map(this::convertGroup);
        playerInfo.setDiscordRole(role);
        OfflinePlayer player = playerInfo.getPlayer();
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
                    player.getName()
                ));
            });

        // try to add to new group
        if (vaultPermission.playerAddGroup(null, player, role)) {
            this.plugin.getLogger().info(String.format(
                    "%s has been put into group %s",
                    player.getName(),
                    role
            ));

            // if player is online
            Optional.ofNullable(player.getPlayer())
                    .ifPresent(p -> Translation.ADDED_TO_GROUP.send(p, role));

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
                    .severe(String.format("unable to grant %s to %s", role, player.getName()));
        }
    }

    private String convertGroup(String role) {
        Validate.notNull(role, "role");
        return role.toLowerCase().replace(' ', '-');
    }
}
