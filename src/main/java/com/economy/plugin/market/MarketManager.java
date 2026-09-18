package com.economy.plugin.market;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class MarketManager {
    private final Map<String, Commodity> commodities = new LinkedHashMap<>();
    private final double volatility;
    private final File itemsFile;

    public MarketManager(File itemsFile, double volatility) {
        this.itemsFile = itemsFile;
        this.volatility = volatility;
        this.loadItems();
    }

    private void loadItems() {
        commodities.clear();
        if (itemsFile == null || !itemsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(key);
            if (item == null) {
                continue;
            }
            String name = item.getString("name", key);
            double base = item.getDouble("base-price", 10.0);
            double elasticity = item.getDouble("elasticity", 1.5);
            commodities.put(key, new Commodity(key, name, base, elasticity));
        }
    }

    public void reload() {
        loadItems();
    }

    public boolean hasCommodity(String id) {
        return commodities.containsKey(id);
    }

    public Commodity getCommodity(String id) {
        return commodities.get(id);
    }

    public Collection<Commodity> getAll() {
        return commodities.values();
    }

    public double getVolatility() {
        return volatility;
    }

    public void recalculateAll() {
        for (Commodity c : commodities.values()) {
            c.getCurrentPrice();
            c.buy(0.0, volatility);
        }
    }
}