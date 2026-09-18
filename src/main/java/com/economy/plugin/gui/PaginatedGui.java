package com.economy.plugin.gui;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class PaginatedGui {
    protected static final int PAGE_SIZE = 45;
    protected static final int SLOT_PREV = 45;
    protected static final int SLOT_INFO = 49;
    protected static final int SLOT_NEXT = 53;

    private final Player player;
    private final String title;
    private int page;
    private Inventory inventory;

    protected PaginatedGui(Player player, String title) {
        this.player = player;
        this.title = title;
    }

    public Player getPlayer() {
        return player;
    }

    protected abstract List<ItemStack> getItems();

    protected void onItemClick(int slot, ClickType click) {
    }

    protected void onActionSlot(int slot, ClickType click) {
    }

    protected boolean isSelectable(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }

    public void open() {
        render();
        GuiManager.open(this);
        player.openInventory(inventory);
    }

    protected void render() {
        List<ItemStack> items = getItems();
        int maxPage = Math.max(0, (items.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);
        inventory = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', title));
        inventory.clear();

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < items.size(); i++) {
            inventory.setItem(i, items.get(start + i));
        }

        renderBottomRow(maxPage, items.size());
    }

    protected void renderBottomRow(int maxPage, int itemCount) {
        inventory.setItem(SLOT_PREV, navItem(Material.ARROW, page > 0, "&a&lPrevious Page",
                page > 0 ? "Click to go back" : "You are on the first page"));
        inventory.setItem(SLOT_INFO, navItem(Material.PAPER, true, "&e&lPage " + (page + 1) + "/" + (maxPage + 1),
                itemCount + " item(s) available"));
        inventory.setItem(SLOT_NEXT, navItem(Material.ARROW, page < maxPage, "&a&lNext Page",
                page < maxPage ? "Click to go forward" : "You are on the last page"));
    }

    protected void refresh() {
        render();
        if (player.getOpenInventory().getTopInventory() == inventory) {
            player.updateInventory();
        }
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void nextPage() {
        page++;
        refresh();
    }

    protected void previousPage() {
        if (page > 0) {
            page--;
            refresh();
        }
    }

    protected int getPage() {
        return page;
    }

    protected static ItemStack navItem(Material material, boolean enabled, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected static ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void handleSlotClick(int slot, ClickType click) {
        if (slot == SLOT_PREV) {
            previousPage();
        } else if (slot == SLOT_NEXT) {
            nextPage();
        } else if (isSelectable(slot)) {
            onItemClick(slot, click);
        } else {
            onActionSlot(slot, click);
        }
    }

    public UUID getPlayerId() {
        return player.getUniqueId();
    }
}