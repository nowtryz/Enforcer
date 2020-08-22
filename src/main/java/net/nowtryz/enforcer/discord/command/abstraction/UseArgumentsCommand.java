package net.nowtryz.enforcer.discord.command.abstraction;

import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.i18n.Translation;

import java.awt.*;

public interface UseArgumentsCommand extends PluginHolder, UseFooterCommand {
    default void sendMissingArgs(User bot, String usage, MessageChannel channel) {
        channel.createEmbed(embedCreateSpec -> {
            embedCreateSpec.setColor(Color.red);
            embedCreateSpec.setTitle(Translation.DISCORD_MISSING_ARGS.get());
            embedCreateSpec.setDescription(usage);
            this.createFooter(bot, embedCreateSpec);
        }).block();
    }
}
