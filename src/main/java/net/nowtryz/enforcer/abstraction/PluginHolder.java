package net.nowtryz.enforcer.abstraction;

import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.storage.PlayersStorage;
import net.nowtryz.enforcer.provider.ConfigProvider;
import net.nowtryz.enforcer.provider.DiscordConfigProvider;
import net.nowtryz.enforcer.provider.TwitchConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public interface PluginHolder {
    /**
     * Returns the plugin held by this Object
     * @return the plugin
     */
    Enforcer getPlugin();

    /**
     * Returns the configurations of the plugin
     * @return the configuration mapper object
     */
    default @NotNull ConfigProvider getConfig() { return this.getPlugin().getProvider(); }

    /**
     * Returns the configurations of the discord bot
     * @return the configuration mapper object
     */
    default DiscordConfigProvider getDiscordConfig() { return this.getPlugin().getProvider().getDiscordProvider(); }

    /**
     * Returns the configurations of the twitch bot
     * @return the configuration mapper object
     */
    default TwitchConfigProvider getTwitchConfig() { return this.getPlugin().getProvider().getTwitchProvider(); }

    default @NotNull PlayersStorage getPlayersManager() { return this.getPlugin().getPlayersManager(); }
    default @NotNull Logger getLogger() { return this.getPlugin().getLogger(); }
    default @NotNull Permission getVaultPermission() { return this.getPlugin().getVaultPermission(); }
}
