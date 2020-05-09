package net.nowtryz.enforcer.provider;

import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;

public class TwitchConfigProvider {
    private final ConfigurationSection section;
    private final boolean enabled;
    private final String channel;
    private final String user;
    private final String token;
    private final char prefix;
    private final boolean subSync;
    private final String subGroup;
    private final boolean subWhiteList;
    private final boolean followerSync;
    private final String followerGroup;
    private final boolean followerWhiteList;

    public TwitchConfigProvider(ConfigurationSection section) {
        Validate.notNull(section, "Twitch section is missing in the config.yml");
        this.section = section;

        // queries
        String prefixField = section.getString("prefix", "!");
        assert prefixField != null;

        //affectations
        this.enabled = section.getBoolean("enabled", false);
        this.channel = toLower(section.getString("channel"));
        this.user = section.getString("user");
        this.token = section.getString("token");
        this.prefix = prefixField.charAt(0);
        this.subSync = section.getBoolean("sub.sync", false);
        this.subGroup = section.getString("sub.group", "sub");
        this.subWhiteList = section.getBoolean("sub.white-list", false);
        this.followerSync = section.getBoolean("follower.sync", false);
        this.followerGroup = section.getString("follower.group", "follower");
        this.followerWhiteList = section.getBoolean("follower.white-list", false);

        //Validations
        if (this.enabled) {
            Validate.notNull(this.channel, "Twitch channel cannot be null");
            Validate.notNull(this.token, "Twitch token cannot be null");
            Validate.isTrue(prefixField.length() == 1, "Twitch prefix must be a character");
        }
    }

    private String toLower(String str) {
        if (str == null) return  null;
        return str.toLowerCase();
    }

    public ConfigurationSection getSection() {
        return section;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getChannel() {
        return channel;
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public char getPrefix() {
        return prefix;
    }

    public boolean isSubSync() {
        return subSync;
    }

    public String getSubGroup() {
        return subGroup;
    }

    public boolean isSubWhiteList() {
        return subWhiteList;
    }

    public boolean isFollowerSync() {
        return followerSync;
    }

    public String getFollowerGroup() {
        return followerGroup;
    }

    public boolean isFollowerWhiteList() {
        return followerWhiteList;
    }
}
