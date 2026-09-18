package com.economy.plugin.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import com.economy.plugin.gui.PaginatedGui;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }
        PaginatedGui gui = GuiManager.get((Player) event.getWhoClicked());
        if (gui == null || event.getView().getTopInventory() != event.getClickedInventory()) {
            return;
        }
        event.setCancelled(true);
        gui.handleSlotClick(event.getSlot(), event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        PaginatedGui gui = GuiManager.get((Player) event.getWhoClicked());
        if (gui == null || event.getView().getTopInventory() == null) {
            return;
        }
        for (Integer slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}