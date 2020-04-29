package net.nowtryz.enforcer.discord.command;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.MessageChannel;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.discord.ConfigProvider;

import java.awt.*;

abstract class AbstractDiscordCommand implements DiscordCommand {
    protected final ConfigProvider provider;
    protected final Enforcer plugin;
    private final String command;

    AbstractDiscordCommand(String command, Enforcer plugin, ConfigProvider provider) {
        this.command = command;
        this.plugin = plugin;
        this.provider = provider;
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
        return this.plugin.translate("discord.command-desciption." + this.getDescriptionKey());
    }

    public String getDescriptionKey() {
        return this.getCommand();
    }

    protected void sendMissingArgs(String usage, MessageChannel channel) {
        channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(Color.red);
            embedCreateSpec.setTitle(this.plugin.translate("discord.missing-args"));
            embedCreateSpec.setDescription(usage);
        }).block();
    }
}
