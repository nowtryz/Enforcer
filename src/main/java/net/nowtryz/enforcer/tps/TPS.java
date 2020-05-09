/*
 * The MIT License
 * Copyright (c) 2014-2015 Techcable
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.nowtryz.enforcer.tps;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TPS {
    private TPS() {}

    public static double getTPS() {
        return getAverageTPS(1);
    }

    public static double getAverageTPS(int time) {
        double[] recentTps;
        if (canGetWithPaper()) {
            recentTps = getPaperRecentTps();
        } else {
            recentTps = getNMSRecentTps();
        }
        double raw, tps;
        switch (time) {
            case 1 :
                raw = recentTps[0];
                tps = Math.min(Math.round(raw * 100.0) / 100.0, 20.0);
                return tps;
            case 5 :
                raw = recentTps[1];
                tps = Math.min(Math.round(raw * 100.0) / 100.0, 20.0);
                return tps;
            case 15 :
                raw = recentTps[2];
                tps = Math.min(Math.round(raw * 100.0) / 100.0, 20.0);
                return tps;
            default :
                throw new IllegalArgumentException("Unsupported tps measure time " + time);
        }
    }

    private static final Class<?> spigotServerClass = Reflection.getClass("org.bukkit.Server$Spigot");
    private static final Method getSpigotMethod = Reflection.makeMethod(Bukkit.class, "spigot");
    private static final Method getTPSMethod = spigotServerClass != null ? Reflection.makeMethod(spigotServerClass, "getTPS") : null;
    private static double[] getPaperRecentTps() {
        if (!canGetWithPaper()) throw new UnsupportedOperationException("Can't get TPS from Paper");
        Object server = Reflection.callMethod(getServerMethod, null); // Call static MinecraftServer.getServer()
        return Reflection.getField(recentTpsField, server);
    }

    private static boolean canGetWithPaper() {
        return getSpigotMethod != null && getTPSMethod != null;
    }

    private static final Class<?> minecraftServerClass = Reflection.getNmsClass("MinecraftServer");
    private static final Method getServerMethod = minecraftServerClass != null ? Reflection.makeMethod(minecraftServerClass, "getServer") : null;
    private static final Field recentTpsField = minecraftServerClass != null ? Reflection.makeField(minecraftServerClass, "recentTps") : null;
    private static double[] getNMSRecentTps() {
        if (getServerMethod == null || recentTpsField == null) throw new UnsupportedOperationException("Can't get TPS from NMS");
        Object server = Reflection.callMethod(getServerMethod, null); // Call static MinecraftServer.getServer()
        return Reflection.getField(recentTpsField, server);
    }
}
