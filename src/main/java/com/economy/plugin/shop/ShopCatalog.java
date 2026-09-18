package com.economy.plugin.shop;

import com.economy.plugin.core.EconomyConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ShopCatalog {
    private final Map<String, ShopSection> sections = new LinkedHashMap<>();
    private final Map<String, Integer> startingStock = new LinkedHashMap<>();
    private final List<String> unobtainable = new ArrayList<>();
    private final List<String> bannedNameParts = new ArrayList<>();

    public ShopCatalog(EconomyConfig cfg) {
        ConfigurationSection sectionsSection = cfg.shopSections();
        if (sectionsSection != null) {
            for (String sectionId : sectionsSection.getKeys(false)) {
                ConfigurationSection sec = sectionsSection.getConfigurationSection(sectionId);
                if (sec == null) {
                    continue;
                }
                sections.put(sectionId.toUpperCase(Locale.ROOT), new ShopSection(sectionId, sec));
            }
        }
        for (Map.Entry<String, Integer> entry : cfg.startingStock().entrySet()) {
            startingStock.put(entry.getKey(), Math.max(1, entry.getValue()));
        }
        for (String item : cfg.unobtainableItems()) {
            unobtainable.add(item.toLowerCase(Locale.ROOT));
        }
        for (String part : cfg.bannedNameParts()) {
            bannedNameParts.add(part.toLowerCase(Locale.ROOT));
        }
    }

    public List<ShopSection> getSections() {
        return Collections.unmodifiableList(new ArrayList<>(sections.values()));
    }

    public ShopSection getSection(String id) {
        return sections.get(id.toUpperCase(Locale.ROOT));
    }

    public int getStartingCount(String materialName) {
        Integer count = startingStock.get(materialName);
        return count == null ? 0 : count;
    }

    public int getStartingCount(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }
        return getStartingCount(item.getType().name());
    }

    public ShopSection sectionOf(String materialName) {
        String up = materialName.toUpperCase(Locale.ROOT);
        for (ShopSection section : sections.values()) {
            for (String item : section.getItemMaterialNames()) {
                if (item.equalsIgnoreCase(up)) {
                    return section;
                }
            }
        }
        return null;
    }

    public boolean isObtainable(String materialName) {
        String lower = materialName.toLowerCase(Locale.ROOT);
        if (unobtainable.contains(lower)) {
            return false;
        }
        for (String part : bannedNameParts) {
            if (lower.contains(part)) {
                return false;
            }
        }
        return true;
    }

    public boolean isObtainable(ItemStack item) {
        return item != null && !item.getType().isAir() && isObtainable(item.getType().name());
    }
}