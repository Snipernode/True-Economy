package com.economy.plugin.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class GuiManager {
    private static final Map<UUID, PaginatedGui> open = new ConcurrentHashMap<>();

    private GuiManager() {
    }

    public static void open(PaginatedGui gui) {
        open.put(gui.getPlayerId(), gui);
    }

    public static PaginatedGui get(Player player) {
        return open.get(player.getUniqueId());
    }

    public static void remove(Player player) {
        open.remove(player.getUniqueId());
    }

    public static void removeAll() {
        open.clear();
    }
}