package net.nowtryz.enforcer;

import com.google.common.base.Charsets;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.discord.DiscordBot;
import net.nowtryz.enforcer.listeners.FirewallListener;
import net.nowtryz.enforcer.provider.ConfigProvider;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public final class Enforcer extends JavaPlugin {
    private FileConfiguration lang;
    private Permission vaultPermission;
    private PlayersManager playersManager;
    private DiscordBot discordBot = null;
    private final CountDownLatch enableLatch = new CountDownLatch(1);
    private ConfigProvider provider;

    @Override
    public void onEnable() {
        // Plugin startup logic
        super.saveDefaultConfig();
        this.provider = new ConfigProvider(this);

        // load language file
        InputStream langStream = getResource("fr-FR.yml");
        Validate.notNull(langStream, "Unable to find language file");
        this.lang = YamlConfiguration.loadConfiguration(new InputStreamReader(langStream, Charsets.UTF_8));

        // players manager
        this.playersManager = new PlayersManager(this);

        // Bots
        if (this.provider.discord.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            this.getLogger().info("Starting discord bot");
            this.discordBot = new DiscordBot(this);
            this.discordBot.block();
        });

        // Vault API
         this.vaultPermission = Objects.requireNonNull(
                 Bukkit.getServicesManager().getRegistration(Permission.class),
                 "Cannot find a permission system"
         ).getProvider();
         this.getLogger().info(this.translate("using-perms", this.vaultPermission.getName()));

        // Firewall
        if (!this.getServer().getOnlineMode() && this.provider.isFirewallEnabled()) {
            this.getLogger().info("Enabling firewall");
            Bukkit.getPluginManager().registerEvents(new FirewallListener(this), this);
        }

        // enabled
        this.getLogger().log(Level.INFO, this.translate("loaded", this.provider.getOwner()));
        this.enableLatch.countDown();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Optional.ofNullable(this.discordBot).ifPresent(DiscordBot::disable);
        this.discordBot = null;

        this.playersManager.save();
        this.playersManager = null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equals("enforcer") && sender.hasPermission("enforcer.reload")) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                this.onDisable();
                this.reloadConfig();
                this.onEnable();
            });
        }
        return true;
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

    /**
     * Getter for the lang object
     * @return the lang file configuration
     */
    public FileConfiguration getLang() {
        return this.lang;
    }

    public Permission getVaultPermission() {
        return vaultPermission;
    }

    public PlayersManager getPlayersManager() {
        return playersManager;
    }

    public CountDownLatch getEnableLatch() {
        return enableLatch;
    }

    public ConfigProvider getProvider() {
        return provider;
    }

    /**
     * Translate the given key using the language file
     * @param key the key to get from the language file
     * @param args variable to use in the string formatter
     * @return the formatted translated string
     */
    public String translate(String key, Object... args) {
        Validate.notNull(key, "key is missing");
        String translation = this.lang.getString(key);
        Validate.notNull(translation, "no translation for " + key);

        if (args.length > 0) return String.format(translation, args);
        else return translation;
    }
}
