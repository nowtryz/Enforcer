package net.nowtryz.enforcer.storage.flatfile;

import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.storage.PlayerInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FilePlayerInfo extends PlayerInfo {
    ConfigurationSection playerSection, discordSection;
    private final FilePlayersStorage manager;

    FilePlayerInfo(@NotNull UUID uuid, @NotNull ConfigurationSection playerSection, @NotNull FilePlayersStorage manager) {
        super(uuid);
        this.manager = manager;
        this.playerSection = playerSection;
        this.hydrate();
    }

    @Override
    public void hydrate() {
        this.twitchUsername = playerSection.getString("twitch");
    }

    @Override
    public Optional<Snowflake> getDiscordId() {
        return Optional.ofNullable(playerSection.getString("discord"))
                .filter(id -> Long.parseLong(id) != 0)
                .map(Snowflake::of);
    }

    @Override
    public List<String> getIps() {
        return this.playerSection.getStringList("ip");
    }

    @Override
    public boolean isRegisteringNewIp() {
        return this.playerSection.getBoolean("registering-new-ip");
    }

    @Override
    public void allowNewIp(boolean doAllow) {
        this.manager.allowNewIp(this, doAllow);
    }

    @Override
    public void allowNewIp() {
        this.allowNewIp(true);
    }

    @Override
    public void newIp(InetAddress ip) {
        this.manager.registerNewIp(ip, this);
        this.manager.asyncSave();
    }

    @Override
    public void setDiscordId(Snowflake id) {
        this.manager.registerDiscordAccount(id, this);
        this.manager.asyncSave();
    }

    @Override
    public void setTwitchUsername(String username) {
        this.manager.registerTwitchAccount(username, this);
        this.manager.asyncSave();
    }
}
