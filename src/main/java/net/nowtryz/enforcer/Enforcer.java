package net.nowtryz.enforcer;

import com.google.common.base.Charsets;
import net.milkbowl.vault.permission.Permission;
import net.nowtryz.enforcer.discord.DiscordBot;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.logging.Level;

public final class Enforcer extends JavaPlugin {
    private FileConfiguration lang;
    private Permission vaultPermission;
    private PlayersManager playersManager;
    private Optional<DiscordBot> discordBot = Optional.empty();

    @Override
    public void onEnable() {
        // Plugin startup logic
        super.saveDefaultConfig();

        // load language file
        InputStream langStream = getResource("fr-FR.yml");
        Validate.notNull(langStream, "Unable to find language file");
        this.lang = YamlConfiguration.loadConfiguration(new InputStreamReader(langStream, Charsets.UTF_8));

        // players manager
        this.playersManager = new PlayersManager(this);

        // Bots
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            this.discordBot = Optional.of(new DiscordBot(this));
            this.discordBot.ifPresent(DiscordBot::block);
        });

        // Vault API
         this.vaultPermission = getServer().getServicesManager().getRegistration(Permission.class).getProvider();
         this.getLogger().info(this.translate("using-perms", this.vaultPermission.getName()));

        // Listener
        this.getServer().getPluginManager().registerEvents(new JoinListener(this, this.playersManager), this);

        // enabled
        this.getLogger().log(Level.INFO, this.translate("loaded", this.getConfig().getString("owner")));
    }

    @Override
    public void onDisable() {
        this.playersManager.save();
        this.playersManager = null;
        this.discordBot.ifPresent(DiscordBot::onDisable);
        this.discordBot = Optional.empty();
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
        return discordBot;
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

    /**
     * Translate the given key using the language file
     * @param key the key to get from the language file
     * @param args variable to use in the string formatter
     * @return the formatted translated string

    public Optional<DiscordBot> getDiscordBot() {
        return discordBot;
    }
     */
    public String translate(String key, Object... args) {
        Validate.notNull(key, "key is missing");
        String translation = this.lang.getString(key);
        Validate.notNull(translation, "no translation for " + key);

        if (args.length > 0) return String.format(translation, args);
        else return translation;
    }
}
