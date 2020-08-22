package net.nowtryz.enforcer.provider;

import com.google.mu.util.stream.BiStream;
import discord4j.core.object.util.Snowflake;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Getter
public final class DiscordConfigProvider {
    private final ConfigurationSection section;
    private final Snowflake guild;
    private final char prefix;
    private final Color embedColor;
    private final boolean enabled;
    private final String token;
    @Accessors(fluent = true)
    private final boolean doesUpdatePresence;
    private final Map<Snowflake, String> roleToGroup;
    private final Map<String, Snowflake> groupToRole;


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
        this.embedColor = new Color(section.getInt("embed-color", 14528782));
        this.doesUpdatePresence = section.getBoolean("presence", true);

        this.roleToGroup = this.parseDownSync(section.getConfigurationSection("synchronisations.down"));
        this.groupToRole = this.parseUpSync(section.getConfigurationSection("synchronisations.up"));


        //Validations
        if (this.enabled) {
            Validate.notNull(this.token, "Discord token cannot be null");
            Validate.isTrue(prefixField.length() == 1, "Discord prefix must be a character");
        }
    }

    private Map<Snowflake, String> parseDownSync(ConfigurationSection downSection) {
        // TODO catch number exception
        return BiStream.from(downSection.getKeys(false), Snowflake::of, section::getString).toMap();
    }

    private Map<String, Snowflake> parseUpSync(ConfigurationSection downSection) {
        // TODO catch number exception
        return BiStream.from(downSection.getKeys(false), Function.identity(), section::getString)
                .mapValues(Snowflake::of)
                .toMap();
    }

    public Optional<String> getGroupForRole(Snowflake role) {
        return Optional.ofNullable(this.roleToGroup.get(role));
    }
}
