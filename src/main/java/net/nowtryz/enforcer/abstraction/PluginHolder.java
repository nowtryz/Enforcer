package net.nowtryz.enforcer.abstraction;

import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.playermanager.PlayersManager;
import net.nowtryz.enforcer.provider.ConfigProvider;
import net.nowtryz.enforcer.provider.DiscordConfigProvider;
import net.nowtryz.enforcer.provider.TwitchConfigProvider;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public interface PluginHolder {
    Enforcer getPlugin();

    default @NotNull ConfigProvider getProvider() { return this.getPlugin().getProvider(); }
    default DiscordConfigProvider getDiscordProvider() { return this.getPlugin().getProvider().getDiscordProvider(); }
    default TwitchConfigProvider getTwitchProvider() { return this.getPlugin().getProvider().getTwitchProvider(); }
    default @NotNull PlayersManager getPlayersManager() { return this.getPlugin().getPlayersManager(); }
    default @NotNull Logger getLogger() { return this.getPlugin().getLogger(); }
    default @NotNull Permission getVaultPermission() { return this.getPlugin().getVaultPermission(); }
    default String translate(String key, Object... args) { return this.getPlugin().translate(key, args); }
}
