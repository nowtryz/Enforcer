package net.nowtryz.enforcer;

import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.discord.command.DiscordMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import net.nowtryz.enforcer.PlayersManager.PlayerInfo;

import java.util.logging.Level;

public class JoinListener implements Listener {
    private Enforcer plugin;
    private PlayersManager players;

    public JoinListener(Enforcer enforcer, PlayersManager players) {
        this.plugin = enforcer;
        this.players = players;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        PlayerInfo playerInfo = this.players.getPlayerInfo(player.getName());

        // ip checking
        if (!playerInfo.getIps().contains(event.getAddress().getHostName())) {
            plugin.getLogger().log(Level.INFO, String.format("player using wrong ip (%s)", player.getName()));
            if (playerInfo.isRegisteringNewIp()) {
                plugin.getLogger().log(Level.INFO, String.format("registering new ip for %s", player.getName()));
                playerInfo.newIp(event.getAddress());
            }
            else event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.translate(
                    "login-denied",
                    this.plugin.getConfig().getString("discord.prefix") + DiscordMessenger.NEW_IP
            ));
        }

        playerInfo.getDiscordId().ifPresent(discordId -> {
            this.plugin.getDiscordBot().ifPresent(bot -> Bukkit.getScheduler().runTaskAsynchronously(this.plugin,
                    () -> bot.grabRole(playerInfo, discordId)
            ));
        });
    }
}
