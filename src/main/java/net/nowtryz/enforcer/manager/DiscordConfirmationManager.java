package net.nowtryz.enforcer.manager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import discord4j.core.object.entity.User;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.chat.*;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.storage.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;

public class DiscordConfirmationManager implements PluginHolder, BiConsumer<Player, UUID> {
    private final BiMap<UUID, User> confirmationsPending = HashBiMap.create();
    private @Getter final Enforcer plugin;

    public DiscordConfirmationManager(Enforcer plugin) {
        this.plugin = plugin;
    }

    public boolean hasRequestPending(UUID request) {
        return this.confirmationsPending.containsKey(request);
    }

    public boolean hasRequestPending(User requester) {
        return this.confirmationsPending.inverse().containsKey(requester);
    }

    @Override
    public void accept(Player player, UUID request) {
        User discordUser = this.confirmationsPending.get(request);
        if (discordUser == null) return;

        PlayerInfo playerInfo = this.getPlayersManager().getPlayerInfo(player.getUniqueId());
        playerInfo.setDiscordId(discordUser.getId());
        this.getDiscordBot().ifPresent(bot -> bot.grabRole(playerInfo));

        Translation.DISCORD_LINKED.send(player, discordUser.getUsername(), discordUser.getDiscriminator());
    }

    public void refuse(Player player, UUID request) {
        User discordUser = this.confirmationsPending.get(request);
        if (discordUser == null) return;
        this.confirmationsPending.remove(request, discordUser);

        Translation.DISCORD_CONFIRMATION_REFUSED.send(player);
    }

    public void awaitConfirmation(@NonNull Player player, @NonNull User discordUser) {
        UUID requestId = UUID.randomUUID();
        this.confirmationsPending.put(requestId, discordUser);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{
                new TextComponent(Translation.DISCORD_CONFIRMATION_CLICK.get())
        });

        Translation.DISCORD_CONFIRMATION_HEADER.send(player, discordUser.getUsername(), discordUser.getDiscriminator());

        player.spigot().sendMessage(new ComponentBuilder("    ")
                .append(new ComponentBuilder(Translation.DISCORD_CONFIRMATION_ACCEPT.get())
                        .event(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/enforcer accept " + requestId.toString())
                        )
                        .event(hoverEvent)
                        .create())
                .append("     ", ComponentBuilder.FormatRetention.NONE)
                .append(new ComponentBuilder(Translation.DISCORD_CONFIRMATION_REFUSE.get())
                        .event(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/enforcer refuse " + requestId.toString())
                        )
                        .event(hoverEvent)
                        .create())
                .create());

        Translation.DISCORD_CONFIRMATION_FOOTER.send(player);
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                this.plugin,
                () -> this.confirmationsPending.remove(requestId, discordUser),
                this.getDiscordConfig().getConfirmationTimeout()
        );
    }
}
