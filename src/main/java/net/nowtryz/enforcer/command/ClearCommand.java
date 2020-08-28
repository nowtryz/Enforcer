package net.nowtryz.enforcer.command;

import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ClearCommand implements Command {
    @Override
    public String getKey() {
        return "clear";
    }

    @Override
    public String getPermission() {
        return "enforcer.reset";
    }

    @Override
    public void run(Enforcer plugin, CommandSender sender, String argument) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, plugin.getPlayersManager()::clear);
        Translation.CLEARED.send(sender);
    }
}
