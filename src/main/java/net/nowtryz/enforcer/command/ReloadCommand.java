package net.nowtryz.enforcer.command;

import net.nowtryz.enforcer.Enforcer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements Command {
    @Override
    public String getKey() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "enforcer.reload";
    }

    @Override
    public void run(Enforcer plugin, CommandSender player, String argument) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.onDisable();
            plugin.reloadConfig();
            plugin.onEnable();
        });
    }
}
