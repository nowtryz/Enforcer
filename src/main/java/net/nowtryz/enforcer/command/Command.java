package net.nowtryz.enforcer.command;

import net.nowtryz.enforcer.Enforcer;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface Command {
    String getKey();

    void run(Enforcer plugin, CommandSender sender, String argument);

    default List<String> tabComplete(Enforcer plugin, CommandSender sender, String argument) {
        return null;
    }

    default String getPermission() {
        return null;
    }
}
