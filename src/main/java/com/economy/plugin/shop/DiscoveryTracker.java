package com.economy.plugin.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class DiscoveryTracker {
    private static final long SAVE_THROTTLE_MS = 5000L;

    private final Map<String, Record> records = new ConcurrentHashMap<>();
    private final PlayerIdentityResolver identityResolver;
    private final File dataFile;
    private final YamlConfiguration persisted = new YamlConfiguration();
    private long dirtySince;
    private final Object writeLock = new Object();
    private Function<String, String> canonicalKeyResolver = Function.identity();

    public DiscoveryTracker(PlayerIdentityResolver resolver, File dataFile) {
        this.identityResolver = resolver;
        this.dataFile = dataFile;
        load();
    }

    private void load() {
        if (dataFile == null || !dataFile.exists()) {
            return;
        }
        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = onDisk.getConfigurationSection("discoveries");
        if (root == null) {
            return;
        }
        for (String identityKey : root.getKeys(false)) {
            String uuidStr = root.getString(identityKey + ".uuid");
            String xuid = root.getString(identityKey + ".xuid");
            List<String> blockNames = root.getStringList(identityKey + ".blocks");
            UUID uuid = null;
            if (uuidStr != null && !uuidStr.isEmpty()) {
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                    uuid = null;
                }
            }
            Record record = new Record(uuid, xuid);
            record.blocks.addAll(blockNames);
            records.put(identityKey, record);
        }
    }

    private void persist() {
        if (dataFile == null) {
            return;
        }
        synchronized (writeLock) {
            persisted.set("discoveries", null);
            for (Map.Entry<String, Record> e : records.entrySet()) {
                Record record = e.getValue();
                String base = "discoveries." + e.getKey();
                persisted.set(base + ".uuid", record.uuid == null ? null : record.uuid.toString());
                persisted.set(base + ".xuid", record.xuid);
                persisted.set(base + ".blocks", new ArrayList<>(record.blocks));
            }
            try {
                if (dataFile.getParentFile() != null) {
                    dataFile.getParentFile().mkdirs();
                }
                persisted.save(dataFile);
            } catch (IOException ignored) {
                // best-effort persistence
            }
        }
    }

    private String keyOf(Player player) {
        return canonicalKeyResolver.apply(identityResolver.resolve(player).getKey());
    }

    public void setCanonicalKeyResolver(Function<String, String> resolver) {
        this.canonicalKeyResolver = resolver == null ? Function.identity() : resolver;
    }

    public void mergeIdentities(String primaryKey, String secondaryKey) {
        String canonical = canonicalKeyResolver.apply(primaryKey);
        String secondary = canonicalKeyResolver.apply(secondaryKey);
        if (canonical.equals(secondary)) {
            return;
        }
        Record sec = records.get(secondary);
        Record prim = records.get(canonical);
        if (prim == null) {
            prim = new Record(uuidOfKey(canonical), xuidOfKey(canonical));
            records.put(canonical, prim);
        }
        if (sec != null) {
            prim.blocks.addAll(sec.blocks);
            records.remove(secondary);
        }
        persist();
    }

    private static UUID uuidOfKey(String key) {
        if (key == null || !key.startsWith("uuid:")) {
            return null;
        }
        try {
            return UUID.fromString(key.substring(5));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String xuidOfKey(String key) {
        return key != null && key.startsWith("xuid:") ? key.substring(5) : null;
    }

    public void recordStep(Player player, String materialName) {
        recordSteps(keyOf(player), materialName);
    }

    public void recordSteps(String identityKey, String... materialNames) {
        Record record = records.get(identityKey);
        if (record == null) {
            record = new Record(null, identityKey.startsWith("xuid:") ? identityKey.substring(5) : null);
            records.put(identityKey, record);
        }
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (String materialName : materialNames) {
            if (materialName == null || materialName.isEmpty() || !record.blocks.add(materialName.toLowerCase())) {
                continue;
            }
            changed = true;
        }
        if (changed) {
            boolean save = dirtySince == 0L || now - dirtySince >= SAVE_THROTTLE_MS;
            if (save) {
                dirtySince = now;
                persist();
            }
        }
    }

    public Set<String> discoveredBlocks(Player player) {
        return discoveredBlocks(keyOf(player));
    }

    public Set<String> discoveredBlocks(String identityKey) {
        Record record = records.get(identityKey);
        return record == null ? Collections.emptySet() : record.getBlocks();
    }

    public boolean hasDiscovered(Player player, String materialName) {
        return discoveredBlocks(player).contains(materialName.toLowerCase());
    }

    public void save() {
        persist();
    }

    public static final class Record {
        public final UUID uuid;
        public final String xuid;
        public final Set<String> blocks = new LinkedHashSet<>();

        Record(UUID uuid, String xuid) {
            this.uuid = uuid;
            this.xuid = xuid;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getXuid() {
            return xuid;
        }

        public Set<String> getBlocks() {
            return Collections.unmodifiableSet(blocks);
        }
    }
}