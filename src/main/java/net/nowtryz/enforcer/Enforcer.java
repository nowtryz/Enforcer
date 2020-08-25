package net.nowtryz.enforcer;

import lombok.Getter;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.listeners.FirewallListener;
import net.nowtryz.enforcer.listeners.LuckPermsListener;
import net.nowtryz.enforcer.manager.DiscordConfirmationManager;
import net.nowtryz.enforcer.storage.flatfile.FilePlayersStorage;
import net.nowtryz.enforcer.storage.PlayersStorage;
import net.nowtryz.enforcer.provider.ConfigProvider;
import net.nowtryz.enforcer.twitch.TwitchBot;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

@Getter
public final class Enforcer extends JavaPlugin {
    private Permission vaultPermission;
    private PlayersStorage playersManager;
    private DiscordBot discordBot = null;
    private TwitchBot twitchBot = null;
    private final CountDownLatch enableLatch = new CountDownLatch(1);
    private ConfigProvider provider;
    private DiscordConfirmationManager discordConfirmationManager = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        super.saveDefaultConfig();
        this.provider = new ConfigProvider(this);
        this.playersManager = new FilePlayersStorage(this);

        // load language file
        this.exportDefaultResource(Translation.DEFAULT_LANG + ".yml");
        Translation.init(this);

        // Bots
        if (this.provider.discord.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (this.provider.discord.getToken() == null) {
                this.getLogger().warning("Skipping discord bot: token is not set");
            } else if (this.provider.discord.getGuild().asLong() == 0) {
                this.getLogger().warning("Skipping discord bot: guild is not set");
            } else {
                this.getLogger().info("Starting discord bot");
                this.discordConfirmationManager = new DiscordConfirmationManager(this);
                this.discordBot = new DiscordBot(this);
                this.discordBot.block();
            }
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

        // Group change listener
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            Bukkit.getPluginManager().registerEvents(new LuckPermsListener(this), this);
            this.getLogger().info("LuckPerms listener hooked");
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
        Optional.ofNullable(this.playersManager).ifPresent(PlayersStorage::save);

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

        switch (args[0].toLowerCase()) {
            case "accept":
                if (!(sender instanceof Player)) return true;
                if (args.length != 2) return true;
                UUID request = UUID.fromString(args[1]);

                if (this.discordConfirmationManager.hasRequestPending(request)) {
                    this.discordConfirmationManager.accept((Player) sender, request);
                } else Translation.DISCORD_CONFIRMATION_EXPIRED.send(sender);

                return true;
            case "refuse":
                if (!(sender instanceof Player)) return true;
                if (args.length != 2) return true;
                UUID refusedRequest = UUID.fromString(args[1]);
                if (this.discordConfirmationManager.hasRequestPending(refusedRequest)) {
                    this.discordConfirmationManager.refuse((Player) sender, refusedRequest);
                } else Translation.DISCORD_CONFIRMATION_EXPIRED.send(sender);
                return true;
            case "reload":
                if (label.equals("enforcer") && sender.hasPermission("enforcer.reload")) {
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        this.onDisable();
                        this.reloadConfig();
                        this.onEnable();
                    });
                    return true;
                }
                break;
            case "clear":
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
        if (!file.exists()) {
            if (file.getParentFile().mkdirs()) this.getLogger().info("created folder for locals");
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
