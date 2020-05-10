package net.nowtryz.enforcer.playermanager;

import com.google.common.base.Charsets;
import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;

public class FilePlayersManager extends AbstractPlayersManager {
    private final File playersFile;
    private FileConfiguration config;

    ConfigurationSection players, discord, twitch;


    /**
     * Instantiate a manager to manipulate the players.yml file
     *     File implementation
     * @param plugin the instance of the enforcer plugin
     */
    public FilePlayersManager(Enforcer plugin) {
        super(plugin);
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");

        try (final InputStream defConfigStream = this.plugin.getResource("players.yml")) {
            assert defConfigStream != null;
            if (playersFile.createNewFile()) plugin.getLogger().info("Created a new player file");
            this.config = YamlConfiguration.loadConfiguration(playersFile);
            this.config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
            this.config.options().copyDefaults(true);
            this.players = this.config.getConfigurationSection("players");
            this.discord = this.config.getConfigurationSection("discord");
            this.twitch = this.config.getConfigurationSection("twitch");

            if (!this.config.contains("version", true)) this.migrateV1ToV2();
            else plugin.getLogger().info(String.format("" +
                    "Using players.yml file v%s",
                    this.config.getString("version", "unknown")
            ));

        } catch (IOException e) {
            this.plugin.getLogger().severe("Unable to load players data");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public PlayerInfo getPlayerInfo(String playerName) {
        // We need to get offline player from name to translate it into unique id, hence the deprecation bypass
        return this.getPlayerInfo(Bukkit.getOfflinePlayer(playerName).getUniqueId());
    }

    @Override
    public PlayerInfo getPlayerInfo(UUID uuid) {
        ConfigurationSection section = this.getPlayerSection(uuid);
        return new FilePlayerInfo(uuid, section, this);
    }

    @Override
    public Optional<PlayerInfo> getPlayerFromDiscord(Snowflake userId) {
        ConfigurationSection section = this.discord.getConfigurationSection(userId.asString());

        if (section != null) {
            String playerName = section.getString("mc");

            if (playerName != null) return Optional.of(playerName)
                    .map(UUID::fromString)
                    .map(this::getPlayerInfo);
            else return Optional.empty();
        }
        else return Optional.empty();
    }

    private ConfigurationSection getPlayerSection(UUID uuid) {
        if (!players.contains(uuid.toString())) return this.createPlayerNode(uuid);
        else return players.getConfigurationSection(uuid.toString());
    }

    private ConfigurationSection createPlayerNode(UUID uuid) {
        this.plugin.getLogger().log(Level.INFO, String.format(
            "Creating new node for %s (%s)",
            uuid,
            Bukkit.getOfflinePlayer(uuid).getName()
        ));

        ConfigurationSection section = players.createSection(uuid.toString());
        section.set("ip", new LinkedList<String>());
        section.set("registering-new-ip", true);
        section.set("discord", null);
        section.set("twitch", null);
        this.save();

        return section;
    }

    @Override
    public void registerDiscordAccount(Snowflake userId, PlayerInfo info) {
        // clear old association
        ConfigurationSection discordSection = this.discord.getConfigurationSection(userId.asString());
        if (discordSection != null) {
            Optional.ofNullable(discordSection.getString("mc"))
                    .map(this.players::getConfigurationSection)
                    .ifPresent(oldMc -> oldMc.set("discord", null));
        } else {
            discordSection = this.discord.createSection(userId.asString());
        }

        this.getPlayerSection(info.uuid).set("discord", userId.asLong());
        discordSection.set("mc", info.uuid.toString());
        discordSection.set("role", null);
    }

    @Override
    public void registerTwitchAccount(String twitchUser, PlayerInfo info) {
        // clear old association
        ConfigurationSection twitchSection = this.discord.getConfigurationSection(twitchUser);
        if (twitchSection != null) {
            Optional.ofNullable(twitchSection.getString("mc"))
                    .map(this.players::getConfigurationSection)
                    .ifPresent(oldMc -> oldMc.set("twitch", null));
        } else {
            twitchSection = this.discord.createSection(twitchUser);
        }

        this.getPlayerSection(info.uuid).set("twitch", twitchUser);
        twitchSection.set("mc", info.uuid.toString());
        twitchSection.set("follow", false);
        twitchSection.set("sub", false);
    }

    @Override
    public void allowNewIp(PlayerInfo info, boolean doAllow) {
        this.getPlayerSection(info.uuid).set("registering-new-ip", doAllow);
    }

    @Override
    public void registerNewIp(InetAddress ip, PlayerInfo info) {
        ConfigurationSection section = this.getPlayerSection(info.uuid);
        List<String> ips = info.getIps();
        ips.add(ip.getHostName());
        section.set("ip", ips);
        section.set("registering-new-ip", false);
        this.save();
    }

    @Override
    public void clear() {
        try (final InputStream defConfigStream = this.plugin.getResource("players.yml")) {
            assert defConfigStream != null;
            this.config = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8));
            this.config.options().copyDefaults(true);
            this.players = this.config.getConfigurationSection("players");
            this.discord = this.config.getConfigurationSection("discord");
            this.twitch = this.config.getConfigurationSection("twitch");
            this.asyncSave();
        } catch (IOException e) {
            this.plugin.getLogger().severe("Unable to load players data");
        }
    }

    @Override
    public synchronized void save() {
        try {
            this.config.save(this.playersFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Unable to save players data");
        }
    }

    @SuppressWarnings("deprecation")
    private void migrateV1ToV2() {
        this.plugin.getLogger().warning("Old player file detected, updating file...");

        new HashSet<>(this.players.getKeys(false)).forEach(username -> {
            UUID uuid = Bukkit.getOfflinePlayer(username).getUniqueId();

            this.plugin.getLogger().info(String.format("Renaming %s to %s", username, uuid.toString()));
            this.players.set(uuid.toString(), this.players.get(username));
            this.players.set(username, null);

            PlayerInfo playerInfo = this.getPlayerInfo(uuid);
            playerInfo.getDiscordId().ifPresent(snowflake -> this.registerDiscordAccount(snowflake, playerInfo));
            playerInfo.getTwitchUsername().ifPresent(twitch -> this.registerTwitchAccount(twitch, playerInfo));
        });

        this.asyncSave();
    }
}
