package net.nowtryz.enforcer.i18n;

import com.google.common.base.Charsets;
import net.nowtryz.enforcer.Enforcer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.LF;

/**
 * This enumeration represents the messages that need to be translated
 */
public enum Translation {
    LOADED("loaded", "Hello %s! Enforcer plugin enabled!"),
    USING_PERM_SYSTEM("using-perms", "Using %s for group management"),
    NOT_PLAYER("not.player", "Only players can execute this command"),
    ADDED_TO_GROUP("group-added", "You have been added to the group %s."),
    HELP("help", "Enforcer help: /enforcer <command>\n" +
            "> reload: reload the plugin and restart all bots\n" +
            "> clear: clear all players data"),
    CLEARED("cleared", "Enforcer: all data have been deleted"),
    DISCORD_PRESENCE_LEGACY("discord.presence.legacy", "Minecraft | TPS : %.2f | %d / %d"),
    DISCORD_PRESENCE_DEFAULT("discord.presence.default", "Minecraft"),
    DISCORD_PRESENCE_TPS("discord.presence.tps", "Minecraft | TPS : %.2f"),
    PRESENCE_ONLINE("discord.presence.online", "Minecraft | %d / %d joueurs"),
    DISCORD_READY("discord.ready", ""),
    DISCORD_ALREADY_CONFIRMING("discord.already-confirming", ":x: You already have an in-game confirmation pending"),
    DISCORD_IP_ALLOWED("discord.new-ip-allowed", ""),
    DISCORD_LOGGED("discord.logged-to", "Approved demand for *%s*! Hurry hup, login to the server."),
    DISCORD_MENTIONED("discord.bot-mentioned", "Hey %s! My prefix is \"%c\""),
    DISCORD_ASSOCIATED("discord.already-associated", ":x: *%s* is already associated with %s"),
    DISCORD_MISSING_ARGS("discord.missing-args", ":x: Missing arguments"),
    DISCORD_FOOTER("discord.footer", "Type '%s' to have the list of available commands"),
    DISCORD_AVAILABLE_COMMANDS("discord.available-commands", "Available commands:"),
    DISCORD_NO_PLAYER_TITLE("discord.cannot-get-player.title", ":x: Unable to find a player " +
            "associated with your player"),
    DISCORD_NO_PLAYER_DESC("discord.cannot-get-player.desc", "Have you already linked your account?"),
    DISCORD_REGISTERED("discord.registered", "Your account has been associated with *%s*!"),
    DISCORD_8D("discord.8=D", "Oh! Gross!"),
    DISCORD_CMD_DESC_REGISTER("discord.command-description.register", "Associate your discord with a " +
            "**Minecraft** account to sync roles between the server and discord"),
    DISCORD_LINKED("discord.linked", "Your account has sucessfully been linked to %s#%s!"),
    DISCORD_CONFIRMATION_HEADER("discord.confirmation.header", "Are you the owner of %s#%s?"),
    DISCORD_CONFIRMATION_FOOTER("discord.confirmation.footer", ""),
    DISCORD_CONFIRMATION_REFUSED("discord.confirmation.refused", "Confirmation refused"),
    DISCORD_CONFIRMATION_EXPIRED("discord.confirmation.expired", "This confirmation demand has expired"),
    DISCORD_CONFIRMATION_SENT("discord.confirmation.sent", "A confirmation message has been sent to %s"),
    DISCORD_MUST_BE_ONLINE(
            "discord.must-be-online",
            "You must online on the server to link your discord account"),
    DISCORD_CONFIRMATION_ACCEPT("discord.confirmation.accept", "[YES]"),
    DISCORD_CONFIRMATION_REFUSE("discord.confirmation.refuse", "[NO"),
    DISCORD_CONFIRMATION_CLICK("discord.confirmation.click", "Click to validate"),
    DISCORD_CMD_DESC_ALLOW_IP("discord.command-description.allow-ip", "Add a new ip to the firewall"),
    DISCORD_CMD_DESC_INFO("discord.command-description.info", "Show this help"),
    TWITCH_MISSING_ARGS("twitch.missing-args", "/me: Missing arguments! Use: %s"),
    TWITCH_ASSOCIATED("twitch.already-associated", "/me: Account already associated with %s!"),
    TWITCH_REGISTERED("twitch.registered", "/me: %s, your twitch account has been associated with %s!"),
    LOGIN_DENIED("login-denied", "Login denied! Use %s on the discord server to allow your new ip")
    ;


    public static final String DEFAULT_LANG = "fr-FR"; // may be change in config later

    /**
     * Initialize the translator base on the default language of the plugin
     * @param plugin the Bukkit plugin to get resources from
     */
    public static void init(Enforcer plugin) {
        String fileName = DEFAULT_LANG + ".yml";

        try (InputStream resource = plugin.getResource(fileName)) {
            File file = new File(plugin.getDataFolder(), fileName);
            Validate.notNull(resource, String.format("Unable to find language file for '%s'", DEFAULT_LANG));
            FileConfiguration lang = YamlConfiguration.loadConfiguration(file);
            lang.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(resource, Charsets.UTF_8)));
            Arrays.stream(Translation.values()).forEach(text -> text.init(lang));
        } catch (Exception exception) {
            throw new RuntimeException("Unable to correctly load the language file", exception);
        }

    }

    /**
     * The key to access to the message in the translations file
     */
    private final String key;
    /**
     * The default message to use if no translation is available in the file
     */
    private final String defaultMessage;
    /**
     * The translated message extracted from the translation file
     */
    private String translatedMessage = null;

    /**
     * Create a new text message with information to retrieve the translation from the file
     * @param key the key of the translation in the translations file
     * @param defaultMessage the message to return if the translation is missing, eg. the international version
     */
    Translation(String key, String defaultMessage) {
        this.key = key;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Creates a new text message that doesn't need any translation
     * @param defaultMessage the message to use
     */
    Translation(String defaultMessage) {
        this(null, defaultMessage);
    }

    private void init(FileConfiguration lang) {
        if (this.key == null) this.translatedMessage = ChatColor.translateAlternateColorCodes('&', this.defaultMessage);
        else if (lang.isList(this.key))this.translatedMessage = ChatColor
                .translateAlternateColorCodes('&', String.join(LF, lang.getStringList(this.key)));
        else this.translatedMessage = Optional.ofNullable(lang.getString(this.key))
                    .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                    .orElse(this.defaultMessage);
    }

    /**
     * Translate this message and return the translated version
     * @return the translated message
     */
    public String get() {
        Validate.notNull(this.translatedMessage, "Translations are not initialised");
        return this.translatedMessage;
    }

    public String get(Object... args) {
        return String.format(this.get(), args);
    }

    /**
     * Send the message to a command sender, usually a player
     * @param p the CommandSender that will receive the message
     */
    public void send(CommandSender p) {
        p.sendMessage(this.get());
    }

    public void send(CommandSender p, Object... args) {
        p.sendMessage(this.get(args));
    }

    @Override
    public String toString() {
        return this.get();
    }
}
