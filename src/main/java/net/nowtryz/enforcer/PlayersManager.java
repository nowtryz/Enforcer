package net.nowtryz.enforcer;

import com.google.common.base.Charsets;
import discord4j.core.object.util.Snowflake;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class PlayersManager {
    private final File playersFile;
    private FileConfiguration config;
    private ConfigurationSection players, discord;
    private Enforcer plugin;


    public PlayersManager(Enforcer plugin) {
        this.plugin = plugin;
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");

        try (final InputStream defConfigStream = this.plugin.getResource("players.yml")) {
            assert defConfigStream != null;
            if (playersFile.createNewFile()) plugin.getLogger().info("Created a new player file");
            this.config = YamlConfiguration.loadConfiguration(playersFile);
            this.config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
            this.players = this.config.getConfigurationSection("players");
            this.discord = this.config.getConfigurationSection("discord");
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Unable to load players data");
        }
    }

    private ConfigurationSection getPlayerSection(String playerName) {
        if (!players.contains(playerName)) return this.createPlayerNode(playerName);
        else return players.getConfigurationSection(playerName);
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        ConfigurationSection section = this.getPlayerSection(playerName);
        return new PlayerInfo(playerName, section, this);
    }

    public Optional<PlayerInfo> getPlayerInfo(UUID uuid) {
        return Optional.ofNullable(Bukkit.getServer().getPlayer(uuid))
                .map(Player::getName)
                .map(playerName -> {
            ConfigurationSection section = this.getPlayerSection(playerName);
            return new PlayerInfo(playerName, section, this);
        });
    }

    public Optional<PlayerInfo> getPlayerFromDiscord(Snowflake userId) {
        ConfigurationSection section = this.discord.getConfigurationSection(userId.asString());

        if (section != null) {
            String playerName = section.getString("mc");

            if (playerName != null) return Optional.of(this.getPlayerInfo(playerName));
            else return Optional.empty();
        }
        else return Optional.empty();
    }

    private void registerDiscordAccount(Snowflake userId, String playerName, ConfigurationSection playerSection) {
        // clear old association
        ConfigurationSection discordSection = this.discord.getConfigurationSection(userId.asString());
        if (discordSection != null) {
            String mc = discordSection.getString("mc");
            assert mc != null;
            ConfigurationSection oldMc = this.players.getConfigurationSection(mc);
            assert oldMc != null;
            oldMc.set("discord", null);
        } else {
            discordSection = this.discord.createSection(userId.asString());
        }

        playerSection.set("discord", userId.asLong());
        discordSection.set("mc", playerName);
        discordSection.set("role", null);
    }

    public ConfigurationSection getPlayerDiscordSection(String playerName) {
        String discordId =  this.getPlayerSection(playerName).getString("discord");
        assert discordId != null;
        return this.discord.getConfigurationSection(discordId);
    }

    private ConfigurationSection createPlayerNode(String playerName) {
        this.plugin.getLogger().log(Level.INFO, "Creating new node for " + playerName);
        ConfigurationSection section = players.createSection(playerName);
        section.set("ip", new LinkedList<String>());
        section.set("registering-new-ip", true);
        section.set("discord", null);
        this.save();

        return section;
    }

    private void allowNewIp(ConfigurationSection playerSection) {
        playerSection.set("registering-new-ip", true);
    }

    private void registerNewIp(InetAddress ip, ConfigurationSection section) {
        List<String> ips = section.getStringList("ip");
        ips.add(ip.getHostName());
        section.set("ip", ips);
        section.set("registering-new-ip", false);
        this.save();
    }

    public synchronized void save() {
        try {
            this.config.save(this.playersFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Unable to save players data");
        }
    }

    public static class PlayerInfo {
        ConfigurationSection playerSection, discordSection;
        private final PlayersManager manager;
        private final String playerName;
        private Snowflake discordId;

        PlayerInfo(@NotNull String playerName, @NotNull ConfigurationSection playerSection, @NotNull PlayersManager manager) {
            this.manager = manager;
            this.playerName = playerName;
            this.playerSection = playerSection;
            this.hydrate();
        }

        public void hydrate() {
            String discordIdStr = playerSection.getString("discord");

            if (discordIdStr != null && Long.parseLong(discordIdStr) != 0) {
                this.discordId = Snowflake.of(discordIdStr);
                this.discordSection = this.manager.discord.getConfigurationSection(discordIdStr);
            }
        }

        public String getPlayerName() {
            return playerName;
        }

        public List<String> getIps() {
            return this.playerSection.getStringList("ip");
        }

        public boolean isRegisteringNewIp() {
            return this.playerSection.getBoolean("registering-new-ip");
        }

        public Optional<Snowflake> getDiscordId() {
            return Optional.ofNullable(this.discordId);
        }

        public Optional<String> getDiscordRole() {
            if (this.discordSection != null) return Optional.ofNullable(this.discordSection.getString("role"));
            else return Optional.empty();
        }

        public Optional<Player> getPlayer() {
            return Optional.ofNullable(Bukkit.getServer().getPlayer(playerName));
        }

        public void allowNewIp() {
            this.manager.allowNewIp(this.playerSection);
        }

        public void newIp(InetAddress ip) {
            this.manager.registerNewIp(ip, this.playerSection);
            Bukkit.getScheduler().runTaskAsynchronously(this.manager.plugin, this.manager::save);
        }

        public void setDiscordId(Snowflake id) {
            this.manager.registerDiscordAccount(id, this.playerName, this.playerSection);
            Bukkit.getScheduler().runTaskAsynchronously(this.manager.plugin, this.manager::save);
        }

        public void setDiscordRole(String role) {
            Optional.ofNullable(this.discordSection).ifPresent(section -> section.set("role", role));
            Bukkit.getScheduler().runTaskAsynchronously(this.manager.plugin, this.manager::save);
        }
    }
}
