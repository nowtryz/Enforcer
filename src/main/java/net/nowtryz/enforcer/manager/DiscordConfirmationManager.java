package net.nowtryz.enforcer.manager;

import discord4j.core.object.util.Snowflake;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public class DiscordConfirmationManager implements Consumer<Player> {
    private Map<Player, Snowflake> confirmationsPending;

    @Override
    public void accept(Player player) {

    }
}
