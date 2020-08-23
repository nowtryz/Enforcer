package net.nowtryz.enforcer.storage;

import discord4j.core.object.util.Snowflake;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class PlayerInfo {
    protected final UUID uuid;
    protected Snowflake discordId;
    protected String twitchUsername;

    public PlayerInfo(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public abstract void hydrate();

    @NotNull
    public UUID getUniqueId() {
        return this.uuid;
    }

    public Optional<Snowflake> getDiscordId() {
        return Optional.ofNullable(this.discordId);
    }

    public Optional<String> getTwitchUsername() {
        return Optional.ofNullable(this.twitchUsername);
    }

    @NotNull
    public OfflinePlayer getPlayer() {
        return Bukkit.getServer().getOfflinePlayer(this.uuid);
    }

    public abstract List<String> getIps();

    public abstract boolean isRegisteringNewIp();

    public abstract void allowNewIp(boolean doAllow);

    public abstract void allowNewIp();

    public abstract void newIp(InetAddress ip);

    public abstract void setDiscordId(Snowflake id);

    public abstract void setTwitchUsername(String username);
}
