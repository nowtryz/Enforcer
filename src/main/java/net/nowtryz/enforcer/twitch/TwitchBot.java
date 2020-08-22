package net.nowtryz.enforcer.twitch;

import net.engio.mbassy.listener.Handler;
import net.nowtryz.enforcer.Enforcer;
import net.nowtryz.enforcer.i18n.Translation;
import net.nowtryz.enforcer.playermanager.PlayerInfo;
import net.nowtryz.enforcer.abstraction.PluginHolder;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ConnectionEvent;
import org.kitteh.irc.client.library.feature.twitch.TwitchSupport;

public class TwitchBot implements PluginHolder {
    public static final String REGISTER = "mc";
    private final Enforcer plugin;
    private final String channel;
    private final Client client;

    public TwitchBot(Enforcer plugin) {
        this.plugin = plugin;
        this.channel = '#' + this.getTwitchConfig().getChannel();
        TwitchDebuger twitchDebuger = new TwitchDebuger(this.getLogger());
        this.client = Client.builder()
                .nick(this.getTwitchConfig().getUser())
                .name("Enforcer")
                .server()
                    .host("irc.chat.twitch.tv")
                    .password(this.getTwitchConfig().getToken())
                    .then()
                    .listeners()
                        .input(twitchDebuger::in)
                        .output(twitchDebuger::out)
                        .exception(this::onException)
                        .then()
                    .build();

        TwitchSupport.addSupport(this.client);
        this.client.connect();
        client.getEventManager().registerEventListener(this);
        client.addChannel(this.channel);
    }

    @Override
    public Enforcer getPlugin() {
        return this.plugin;
    }

    public void onException(Throwable e) {
        this.getLogger().severe("TwitchBot error: " + e.getMessage());
    }

    public void onConnect(ConnectionEvent event) {
        this.getLogger().info(" Twitch bot connected");
    }

    @Handler
    public void onJoin(ChannelJoinEvent event) {
        event.getChannel().sendMessage("Hi " + event.getUser().getNick() + "!");
    }

    @Handler
    public void onMessage(ChannelMessageEvent event) {
        String[] args = event.getMessage().split(" ");
        if (args.length == 0) return;
        if (args[0].length() <= 2) return;
        if (args[0].charAt(0) != this.getTwitchConfig().getPrefix()) return;
        String command = args[0].substring(1);

        if (command.equalsIgnoreCase(REGISTER)) this.registerUser(event.getChannel(), event.getActor().getNick(), args);
    }

    protected void registerUser(Channel channel, String sender, String[] args) {
        if (args.length != 2) {
            this.sendMissingArgs(this.getTwitchConfig().getPrefix() + REGISTER + " <username>", channel);
            return;
        }

        String username = args[1];
        PlayerInfo playerInfo = this.getPlayersManager().getPlayerInfo(username);

        playerInfo.getTwitchUsername().ifPresent(twitch -> this.sendAlreadyAssociated(channel, username, twitch));

        if (!playerInfo.getTwitchUsername().isPresent()) {
            playerInfo.setTwitchUsername(sender);

            // TODO get follow and sub status

            channel.sendMessage(Translation.TWITCH_REGISTERED.get(sender, username));
        }
    }

    private void sendAlreadyAssociated(Channel channel, String username, String twitchUser) {
        channel.sendMessage(Translation.TWITCH_ASSOCIATED.get(username, twitchUser));
    }

    private void sendMissingArgs(String usage, Channel channel) {
        channel.sendMessage(Translation.TWITCH_MISSING_ARGS.get(usage));
    }

    public void disable() {
        this.client.shutdown();
    }
}
