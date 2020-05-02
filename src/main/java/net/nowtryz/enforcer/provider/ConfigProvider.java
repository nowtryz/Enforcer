package net.nowtryz.enforcer.provider;

import net.nowtryz.enforcer.Enforcer;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigProvider {
    public final DiscordConfigProvider discord;
    public final TwitchConfigProvider twitch;
    private final String owner;
    private final boolean firewallEnabled;

    public ConfigProvider(Enforcer plugin) {
        FileConfiguration config = plugin.getConfig();
         this.owner = config.getString("owner");
         this.firewallEnabled = config.getBoolean("firewall.enabled", false);
         this.discord = new DiscordConfigProvider(config.getConfigurationSection("discord"));
         this.twitch = new TwitchConfigProvider(config.getConfigurationSection("twitch"));
    }

    public String getOwner() { return owner; }
    public boolean isFirewallEnabled() { return firewallEnabled; }
    public TwitchConfigProvider getTwitchProvider() { return twitch; }
    public DiscordConfigProvider getDiscordProvider() { return discord; }
}
