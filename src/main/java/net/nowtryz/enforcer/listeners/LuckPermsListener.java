package net.nowtryz.enforcer.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.nowtryz.enforcer.Enforcer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

public class LuckPermsListener implements Listener {
    private final Enforcer plugin;
    private final LuckPerms luckPerms;

    public LuckPermsListener(Enforcer plugin) {
        this.plugin = plugin;
        this.luckPerms = Optional.ofNullable(Bukkit.getServicesManager().getRegistration(LuckPerms.class))
                .map(RegisteredServiceProvider::getProvider)
                .orElse(null);

        if (luckPerms != null) luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onUserUpdate);
    }

    public void onUserUpdate(UserDataRecalculateEvent event) {
        this.plugin.getDiscordBot().ifPresent(bot -> bot.publishGroups(
                this.plugin.getPlayersManager().getPlayerInfo(event.getUser().getUniqueId()),
                Bukkit.getOfflinePlayer(event.getUser().getUniqueId())
        ));
    }
}
