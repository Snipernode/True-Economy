package com.economy.plugin.shop;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PlayerIdentityResolver {
    private static final String FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateAPI";
    private static final String FLOODGATE_PLAYER = "org.geysermc.floodgate.api.player.FloodgatePlayer";
    private static volatile Boolean floodgateAvailable;

    private static boolean floodgatePresent() {
        Boolean available = floodgateAvailable;
        if (available == null) {
            try {
                Class.forName(FLOODGATE_API);
                Class.forName(FLOODGATE_PLAYER);
                available = Boolean.TRUE;
            } catch (Throwable t) {
                available = Boolean.FALSE;
            }
            floodgateAvailable = available;
        }
        return available;
    }

    private static String resolveXuid(Player player) {
        if (!floodgatePresent()) {
            return null;
        }
        try {
            Class<?> api = Class.forName(FLOODGATE_API);
            Method byUuid;
            Object floodgatePlayer;
            try {
                byUuid = api.getMethod("getPlayer", UUID.class);
                floodgatePlayer = byUuid.invoke(null, player.getUniqueId());
            } catch (NoSuchMethodException ignored) {
                Method byPlayer = api.getMethod("getPlayer", Player.class);
                floodgatePlayer = byPlayer.invoke(null, player);
            }
            if (floodgatePlayer == null) {
                return null;
            }
            Method getXuid = floodgatePlayer.getClass().getMethod("getXuid");
            Object xuid = getXuid.invoke(floodgatePlayer);
            return xuid == null ? null : xuid.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public PlayerIdentity resolve(Player player) {
        return new PlayerIdentity(player.getUniqueId(), resolveXuid(player));
    }
}