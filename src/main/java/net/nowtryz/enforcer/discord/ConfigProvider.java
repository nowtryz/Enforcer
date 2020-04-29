package net.nowtryz.enforcer.discord;

import discord4j.core.object.util.Snowflake;
import net.nowtryz.enforcer.Enforcer;
import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public class ConfigProvider {
    private final ConfigurationSection section;
    private final Snowflake guild;
    private final char prefix;
    private final int embedColor;
    private final boolean enabled;
    private final String token;


    ConfigProvider(Enforcer plugin) {
        this.section = plugin.getConfig().getConfigurationSection("discord");
        assert this.section != null;

        // queries
        String prefixField = section.getString("prefix", "!");
        assert prefixField != null;

        // affectation
        this.enabled = section.getBoolean("enable", false);
        this.token = section.getString("token");
        this.guild = Snowflake.of(section.getLong("server"));
        this.prefix = prefixField.charAt(0);
        this.embedColor = section.getInt("embed-color", 14528782);

        //Validations
        Validate.notNull(token, "Discord token cannot be null");
        Validate.isTrue(prefixField.length() == 1, "Discord prefix must be a character");
    }

    public ConfigurationSection getSection() {
        return section;
    }

    public Snowflake getGuild() {
        return guild;
    }

    public char getPrefix() {
        return prefix;
    }

    public int getEmbedColor() {
        return embedColor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getToken() {
        return token;
    }
}
