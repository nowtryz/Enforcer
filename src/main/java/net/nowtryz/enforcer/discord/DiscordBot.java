package net.nowtryz.enforcer.discord;

import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;
import net.nowtryz.enforcer.discord.command.AllowIpCommand;
import net.nowtryz.enforcer.discord.command.InfoCommand;
import net.nowtryz.enforcer.discord.command.MinecraftRegistrationCommand;
import net.nowtryz.enforcer.discord.command.abstraction.DiscordCommand;
import org.apache.commons.lang3.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscordBot extends AbstractDiscordBot {
    public final static String REGISTER = "mc", NEW_IP = "newip", INFO = "info"; // TODO put in enum
    private final Map<String, DiscordCommand> commandMap;
    private final DiscordCommand[] commands;

    public DiscordBot(Enforcer plugin) {
        super(plugin);

        this.commands = new DiscordCommand[]{
                new MinecraftRegistrationCommand(this, plugin),
                new InfoCommand(plugin, super::sendInfo),
                new AllowIpCommand(plugin)
        };

        this.commandMap = Arrays.stream(commands)
                .collect(Collectors.toMap(DiscordCommand::getCommand, Function.identity()));

        client.getEventDispatcher().on(MemberUpdateEvent.class)
                .filter(event -> event.getGuildId().equals(this.getDiscordProvider().getGuild()))
                .subscribe(this::onRoleUpdate);
    }

    @Override
    protected DiscordCommand[] getCommands() {
        return this.commands;
    }

    @Override
    protected Map<String, DiscordCommand> getCommandMap() {
        return this.commandMap;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            this.plugin.getEnableLatch().await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.getLogger().severe(e.getMessage());
            return;
        }

        // fixme event not triggered
        this.plugin.getPlayersManager()
                .getPlayerInfo(event.getUniqueId())
                .map(p -> {
                    System.out.println("got player info");
                    return p;
                })
                .ifPresent(this::grabRole);
    }

    public void onRoleUpdate(MemberUpdateEvent event) {
        this.plugin.getPlayersManager().getPlayerFromDiscord(event.getMemberId())
            // Only update online players
            .filter(playerInfo -> playerInfo.getPlayer().map(Player::isOnline).orElse(false))
            .ifPresent(this::grabRole);
    }

    public void grabRole(PlayerInfo playerInfo) {
        playerInfo.getDiscordId()
            .map(userId -> this.client.getMemberById(this.getDiscordProvider().getGuild(), userId))
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
    }

    private String convertGroup(String role) {
        Validate.notNull(role, "role");
        return role.toLowerCase().replace(' ', '-');
    }
}
