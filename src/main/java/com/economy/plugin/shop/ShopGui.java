package com.economy.plugin.shop;

import com.economy.plugin.gui.PaginatedGui;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopGui extends PaginatedGui {
    private static final Material LOCK_GLASS = Material.getMaterial("GRAY_STAINED_GLASS_PANE");
    private static final Material LOCK_FALLBACK = Material.getMaterial("STAINED_GLASS_PANE");

    private final ShopCatalog catalog;
    private final DiscoveryTracker tracker;
    private final List<ItemStack> stock = new ArrayList<>();

    public ShopGui(Player player, String title, ShopCatalog catalog, DiscoveryTracker tracker) {
        super(player, "TrueEconomy " + (title == null ? "" : title.replace("{player}", player.getName())));
        this.catalog = catalog;
        this.tracker = tracker;
        buildStock();
    }

    private void buildStock() {
        Set<String> unlockedBlocks = tracker == null ? Collections.<String>emptySet() : tracker.discoveredBlocks(getPlayer());
        for (ShopSection section : catalog.getSections()) {
            boolean unlocked = section.isUnlocked(unlockedBlocks);
            for (String itemName : section.getItemMaterialNames()) {
                Material material = Material.matchMaterial(itemName);
                if (material == null || !catalog.isObtainable(itemName)) {
                    continue;
                }
                int count = catalog.getStartingCount(itemName);
                stock.add(stockItem(material, count, section, unlocked));
            }
        }
    }

    private ItemStack stockItem(Material material, int count, ShopSection section, boolean unlocked) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(count, material.getMaxStackSize())));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(section.getDisplayName() + " - " + material.name());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Starting stock: " + count);
            if (unlocked) {
                lore.add(ChatColor.GREEN + "Unlocked: section open for trade");
            } else {
                lore.add(ChatColor.RED + "Locked: section requires exploration");
                for (String req : section.getRequiredDiscoveryBlocks()) {
                    lore.add(ChatColor.DARK_RED + "  * Step on: " + req);
                }
            }
            lore.add(ChatColor.DARK_GRAY + "Stock cannot be removed.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected List<ItemStack> getItems() {
        return stock;
    }
}