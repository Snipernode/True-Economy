package com.economy.plugin.shop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;

public class ShopListener implements Listener {
    private static volatile DiscoveryTracker tracker;

    private static boolean isShopMenu(Inventory inventory) {
        if (inventory == null || inventory.getViewers().isEmpty() || inventory.getType() != InventoryType.CHEST) {
            return false;
        }
        String title = null;
        try {
            title = ((HumanEntity) inventory.getViewers().get(0)).getOpenInventory().getTitle();
        } catch (Throwable ignore) {
            title = null;
        }
        if (title == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(title);
        return stripped != null && stripped.contains("TrueEconomy");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!isShopMenu(event.getWhoClicked().getOpenInventory().getTopInventory())) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        ((Player) event.getWhoClicked()).updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!isShopMenu(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        ((Player) event.getWhoClicked()).updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOnGround()) {
            return;
        }
        DiscoveryTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }
        Material stoodOn = to.clone().subtract(0.0, 1.0, 0.0).getBlock().getType();
        tracker.recordStep(player, stoodOn.name());
    }

    public static void setTracker(DiscoveryTracker t) {
        tracker = t;
    }

    public static DiscoveryTracker getTracker() {
        return tracker;
    }
}