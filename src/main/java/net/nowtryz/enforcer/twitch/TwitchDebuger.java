package net.nowtryz.enforcer.twitch;

import java.util.logging.Logger;

public class TwitchDebuger {
    private Logger logger;

    public TwitchDebuger(Logger logger) {
        this.logger = logger;
    }

    public void in(String in) {
        this.logger.info("[I]" + in);
    }

    public void out(String out) {
        this.logger.info("[O]" + out);
    }
}
