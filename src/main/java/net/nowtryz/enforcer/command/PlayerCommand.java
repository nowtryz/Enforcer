package net.nowtryz.enforcer.command;

import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface PlayerCommand extends Command {
    @Override
    default void run(Enforcer plugin, CommandSender sender, String argument) {
        if (sender instanceof Player) this.run(plugin, (Player) sender, argument);
        else Translation.NOT_PLAYER.send(sender);
    }

    void run(Enforcer plugin, Player player, String argument);
}
