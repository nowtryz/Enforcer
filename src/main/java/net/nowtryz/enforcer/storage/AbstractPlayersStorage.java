package net.nowtryz.enforcer.storage;

import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import org.bukkit.Bukkit;

import java.net.InetAddress;

/**
 * Manipulates the information storage of players
 */
public abstract class AbstractPlayersStorage implements PlayersStorage, PluginHolder {
    protected final Enforcer plugin;

    /**
     * Instantiate a manager to manipulate the player information
     * @param plugin the instance of the enforcer plugin
     */
    protected AbstractPlayersStorage(Enforcer plugin) {
        this.plugin = plugin;
    }

    /**
     * Link a discord account to a player
     * @param userId the id of the discord account to link
     * @param info the player information to update
     */
    public abstract void registerDiscordAccount(Snowflake userId, PlayerInfo info);

    /**
     * Link a twitch account tot a player
     * @param twitchUser the username of the twitch account to link
     * @param info the player information to update
     */
    public abstract void registerTwitchAccount(String twitchUser, PlayerInfo info);

    /**
     * Allow to register a new ip to the firewall for the specified player
     * @param info the player information to update
     * @param doAllow whether to allow or not a new ip through the firewall
     */
    public abstract void allowNewIp(PlayerInfo info, boolean doAllow);

    /**
     * Add a new ip to the firewall for the specified player
     * @param ip the ip to add
     * @param info the player information to update
     */
    public abstract void registerNewIp(InetAddress ip, PlayerInfo info);

    @Override
    public void asyncSave() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, this::save);
    }

    @Override
    public Enforcer getPlugin() {
        return plugin;
    }
}
