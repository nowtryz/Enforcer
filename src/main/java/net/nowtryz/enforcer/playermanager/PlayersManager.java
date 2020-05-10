package net.nowtryz.enforcer.playermanager;

import discord4j.core.object.util.Snowflake;

import java.util.Optional;
import java.util.UUID;

public interface PlayersManager {
    /**
     * Retrieve the {@link PlayerInfo} associated to a player from its name
     * @param playerName the username of the player
     * @return the player information
     */
    PlayerInfo getPlayerInfo(String playerName);

    /**
     * retrieve the {@link PlayerInfo} associated to a player from its unique id
     * @param uuid the unique id of the player
     * @return the player information
     */
    PlayerInfo getPlayerInfo(UUID uuid);

    /**
     * retrieve the {@link PlayerInfo} associated to a player if they linked their minecraft account with the given
     * discord id
     * @param userId the unique id of the discord account
     * @return the player information
     */
    Optional<PlayerInfo> getPlayerFromDiscord(Snowflake userId);

    /**
     * Clear all stored player information and links
     */
    void clear();

    /**
     * Save information from memory to permanent storage
     */
    void save();

    /**
     * Perform a {@link PlayersManager#save()} asynchronously.
     */
    void asyncSave();
}
