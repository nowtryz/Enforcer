package net.nowtryz.enforcer.util;

import java.util.Optional;

public class GroupUtil {
    public static String parse(String group) {
        return Optional.ofNullable(group)
                .map(String::toLowerCase)
                .map(s -> s.replace('.', '-'))
                .orElse(null);
    }
}
