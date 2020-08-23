package net.nowtryz.enforcer.listeners;

import lombok.Getter;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import net.nowtryz.enforcer.discord.DiscordBot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.logging.Level;

public class FirewallListener implements Listener, PluginHolder {
    @Getter(onMethod_={@Override}) private final Enforcer plugin;

    public FirewallListener(Enforcer enforcer) {
        this.plugin = enforcer;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        PlayerInfo playerInfo = this.plugin.getPlayersManager().getPlayerInfo(player.getName());

        // ip checking
        if (!playerInfo.getIps().contains(event.getAddress().getHostName())) {
            plugin.getLogger().log(Level.INFO, String.format("player using wrong ip (%s)", player.getName()));
            if (playerInfo.isRegisteringNewIp()) {
                plugin.getLogger().log(Level.INFO, String.format("registering new ip for %s", player.getName()));
                playerInfo.newIp(event.getAddress());
            }
            else event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                Translation.LOGIN_DENIED.get(this.getDiscordConfig().getPrefix() + DiscordBot.NEW_IP)
            );
        }



        playerInfo.allowNewIp(false);
    }
}
