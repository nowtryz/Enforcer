package net.nowtryz.enforcer;

import lombok.Getter;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.listeners.FirewallListener;
import net.nowtryz.enforcer.playermanager.FilePlayersManager;
import net.nowtryz.enforcer.playermanager.PlayersManager;
import net.nowtryz.enforcer.provider.ConfigProvider;
import net.nowtryz.enforcer.twitch.TwitchBot;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public final class Enforcer extends JavaPlugin {
    @Getter private Permission vaultPermission;
    @Getter private PlayersManager playersManager;
    private DiscordBot discordBot = null;
    private TwitchBot twitchBot = null;
    @Getter private final CountDownLatch enableLatch = new CountDownLatch(1);
    @Getter private ConfigProvider provider;


    @Override
    public void onEnable() {
        // Plugin startup logic
        super.saveDefaultConfig();
        this.provider = new ConfigProvider(this);
        this.playersManager = new FilePlayersManager(this);

        // load language file
        this.exportDefaultResource(Translation.DEFAULT_LANG + ".yml");
        Translation.init(this);

        // Bots
        if (this.provider.discord.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            this.getLogger().info("Starting discord bot");
            this.discordBot = new DiscordBot(this);
            this.discordBot.block();
        });
        if (this.provider.twitch.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            this.getLogger().info("Starting twitch bot");
            try {
                this.twitchBot = new TwitchBot(this);
            } catch (Exception e) {
                this.getLogger().log(Level.SEVERE, "Unable to launch twitch bot: %s", e.getMessage());
            }
        });

        // Vault API
         this.vaultPermission = Objects.requireNonNull(
                 Bukkit.getServicesManager().getRegistration(Permission.class),
                 "Cannot find a permission system!"
         ).getProvider();
         this.getLogger().info(Translation.USING_PERM_SYSTEM.get(this.vaultPermission.getName()));

        // Firewall
        if (!this.getServer().getOnlineMode() && this.provider.isFirewallEnabled()) {
            this.getLogger().info("Enabling firewall");
            Bukkit.getPluginManager().registerEvents(new FirewallListener(this), this);
        }

        // enabled
        this.getLogger().info(Translation.LOADED.get(this.provider.getOwner()));
        this.enableLatch.countDown();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Optional.ofNullable(this.twitchBot).ifPresent(TwitchBot::disable);
        Optional.ofNullable(this.discordBot).ifPresent(DiscordBot::disable);
        Optional.ofNullable(this.playersManager).ifPresent(PlayersManager::save);

        this.discordBot = null;
        this.twitchBot = null;
        this.playersManager = null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Translation.HELP.send(sender);
            return true;
        }
        else if (args[0].equals("reload")) {
            if (label.equals("enforcer") && sender.hasPermission("enforcer.reload")) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    this.onDisable();
                    this.reloadConfig();
                    this.onEnable();
                });
                return true;
            }
        } else if (args[0].equals("clear")) {
            Bukkit.getScheduler().runTaskAsynchronously(this, this.playersManager::clear);
            Translation.CLEARED.send(sender);
            return true;
        }
        return false;
    }

    /**
     * Saves the raw contents of any resource embedded with a plugin's .jar file assuming it can be found using
     * {@link JavaPlugin#getResource(String)} if the file doesn't exist. The resource is saved into the plugin's data
     * folder using the same hierarchy as the .jar file (subdirectories are preserved).
     * @param fileName the resource to export
     */
    private void exportDefaultResource(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists() && file.mkdirs()) {
            saveResource(fileName, false);
        }
    }

    /**
     * Get the discord bot
     * @return the discord bot
     */
    public Optional<DiscordBot> getDiscordBot() {
        return Optional.ofNullable(this.discordBot);
    }
}
