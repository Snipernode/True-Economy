package com.economy.plugin.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public class ShopSection {
    private final String id;
    private final String displayName;
    private final List<String> requiredDiscoveryBlocks;
    private final List<String> itemMaterialNames;

    public ShopSection(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = section.getString("display", id);
        this.requiredDiscoveryBlocks = new ArrayList<>(section.getStringList("required-blocks"));
        this.itemMaterialNames = new ArrayList<>(section.getStringList("items"));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRequiredDiscoveryBlocks() {
        return Collections.unmodifiableList(requiredDiscoveryBlocks);
    }

    public List<String> getItemMaterialNames() {
        return Collections.unmodifiableList(itemMaterialNames);
    }

    public boolean hasRequirement() {
        return !requiredDiscoveryBlocks.isEmpty();
    }

    public boolean isUnlocked(Set<String> discoveredBlocks) {
        if (!hasRequirement()) {
            return true;
        }
        Set<String> lower = new LinkedHashSet<>();
        for (String d : discoveredBlocks) {
            lower.add(d.toLowerCase());
        }
        for (String required : requiredDiscoveryBlocks) {
            if (!lower.contains(required.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}