package net.nowtryz.enforcer.discord.command.abstraction;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.discord.DiscordBot;

public interface UseFooterCommand extends PluginHolder {
    default void createFooter(User bot, EmbedCreateSpec embedCreateSpec) {
        embedCreateSpec.setFooter(
                this.getPlugin().translate("discord.footer", this.getDiscordProvider().getPrefix() + DiscordBot.INFO),
                bot.getAvatarUrl()
        );
    }
}
