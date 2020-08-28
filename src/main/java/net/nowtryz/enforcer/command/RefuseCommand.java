package net.nowtryz.enforcer.command;

import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RefuseCommand implements PlayerCommand {
    @Override
    public void run(Enforcer plugin, Player player, String argument) {
        if (argument == null) return;

        try {
            UUID refusedRequest = UUID.fromString(argument);
            if (plugin.getDiscordConfirmationManager().hasRequestPending(refusedRequest)) {
                plugin.getDiscordConfirmationManager().refuse(player, refusedRequest);
            } else Translation.DISCORD_CONFIRMATION_EXPIRED.send(player);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public String getKey() {
        return "refuse";
    }
}
