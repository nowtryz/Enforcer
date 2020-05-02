package net.nowtryz.enforcer.provider;

import discord4j.core.object.util.Snowflake;
import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class DiscordConfigProvider {
    private final ConfigurationSection section;
    private final Snowflake guild;
    private final char prefix;
    private final int embedColor;
    private final boolean enabled;
    private final String token;


    DiscordConfigProvider(@Nullable ConfigurationSection section) {
        Validate.notNull(section, "Discord section is missing in the config.yml");
        this.section = section;

        // queries
        String prefixField = section.getString("prefix", "!");
        assert prefixField != null;

        // affectation
        this.enabled = section.getBoolean("enabled", false);
        this.token = section.getString("token");
        this.guild = Snowflake.of(section.getLong("server"));
        this.prefix = prefixField.charAt(0);
        this.embedColor = section.getInt("embed-color", 14528782);

        //Validations
        if (this.enabled) {
            Validate.notNull(this.token, "Discord token cannot be null");
            Validate.isTrue(prefixField.length() == 1, "Discord prefix must be a character");
        }
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
