package net.nowtryz.enforcer.command;

import lombok.NonNull;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnforcerCommand implements TabExecutor {
    private Enforcer plugin;
    private final Map<String, Command> commandMap;

    public EnforcerCommand(Enforcer plugin, @NonNull PluginCommand command) {
        this.plugin = plugin;
        this.commandMap = Stream
                .of(new AcceptCommand(), new ClearCommand(), new RefuseCommand(), new ReloadCommand())
                .collect(Collectors.toMap(Command::getKey, Function.identity()));

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            Translation.HELP.send(sender);
            return true;
        }

        Command children = this.commandMap.get(args[0]);
        if (children == null) return false;

        // Authorize is permission is null
        if (Optional.ofNullable(children.getPermission()).map(sender::hasPermission).orElse(true)) {
            children.run(this.plugin, sender, args.length == 1 ? null : args[1]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        // Grosse flemme
        return args.length == 1 ? new ArrayList<>(this.commandMap.keySet()) : null;
    }
}
