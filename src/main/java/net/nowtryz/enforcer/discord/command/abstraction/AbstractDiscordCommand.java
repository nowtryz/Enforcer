package net.nowtryz.enforcer.discord.command.abstraction;

import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.provider.DiscordConfigProvider;

public abstract class AbstractDiscordCommand implements DiscordCommand, PluginHolder {
    protected final DiscordConfigProvider provider;
    protected final Enforcer plugin;
    private final String command;

    public AbstractDiscordCommand(String command, Enforcer plugin) {
        this.command = command;
        this.plugin = plugin;
        this.provider = plugin.getProvider().getDiscordProvider();
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public String getUsage() {
        return this.provider.getPrefix() + this.getCommand();
    }

    @Override
    public String getDescription() {
        return this.plugin.translate("discord.command-description." + this.getDescriptionKey());
    }

    @Override
    public Enforcer getPlugin() {
        return plugin;
    }

    public String getDescriptionKey() {
        return this.getCommand();
    }
}
