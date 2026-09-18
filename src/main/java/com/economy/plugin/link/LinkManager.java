package com.economy.plugin.link;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class LinkManager {
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long PENDING_TTL_MS = 300000L;

    private final SecureRandom random = new SecureRandom();
    private final File dataFile;
    private final LinkMerger merger;
    private final Map<String, LinkRecord> links = new ConcurrentHashMap<>();
    private final Map<String, String> canonicalByKey = new ConcurrentHashMap<>();
    private final Map<String, String> canonicalByOwner = new ConcurrentHashMap<>();
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();

    public LinkManager(File dataFile, LinkMerger merger) {
        this.dataFile = dataFile;
        this.merger = merger;
        load();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private void pruneExpired() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }

    public String canonicalKey(String key) {
        String canonical = canonicalByKey.get(key);
        return canonical == null ? key : canonical;
    }

    public String canonicalOwner(String owner) {
        String canonical = canonicalByOwner.get(owner);
        return canonical == null ? owner : canonical;
    }

    public boolean isLinked(String identityKey) {
        return canonicalByKey.containsKey(identityKey);
    }

    public String startLink(String identityKey, String ownerName) {
        pruneExpired();
        String startKey = canonicalKey(identityKey);
        String startOwner = canonicalOwner(ownerName);
        String code;
        do {
            code = generateCode();
        } while (pending.containsKey(code));
        pending.put(code, new PendingLink(startKey, startOwner, System.currentTimeMillis() + PENDING_TTL_MS));
        persist();
        return code;
    }

    public Result completeLink(String code, String identityKey, String ownerName) {
        pruneExpired();
        if (code == null) {
            return Result.NO_SUCH_CODE;
        }
        String normalized = code.trim().toUpperCase();
        PendingLink pendingLink = pending.remove(normalized);
        if (pendingLink == null) {
            return Result.NO_SUCH_CODE;
        }
        if (pendingLink.expiresAt < System.currentTimeMillis()) {
            persist();
            return Result.EXPIRED;
        }
        String callerKey = canonicalKey(identityKey);
        String callerOwner = canonicalOwner(ownerName);
        if (callerKey.equals(pendingLink.startKey)) {
            persist();
            return Result.SELF_LINK;
        }
        if (isLinked(callerKey) || isLinked(pendingLink.startKey)) {
            persist();
            return Result.ALREADY_LINKED;
        }

        String primaryKey;
        String secondaryKey;
        String primaryOwner;
        String secondaryOwner;
        boolean callerHasXuid = callerKey.startsWith("xuid:");
        if (callerHasXuid != pendingLink.startKey.startsWith("xuid:")) {
            if (callerHasXuid) {
                primaryKey = callerKey;
                secondaryKey = pendingLink.startKey;
                primaryOwner = callerOwner;
                secondaryOwner = pendingLink.startOwner;
            } else {
                primaryKey = pendingLink.startKey;
                secondaryKey = callerKey;
                primaryOwner = pendingLink.startOwner;
                secondaryOwner = callerOwner;
            }
        } else {
            primaryKey = pendingLink.startKey;
            secondaryKey = callerKey;
            primaryOwner = pendingLink.startOwner;
            secondaryOwner = callerOwner;
        }
        link(primaryKey, secondaryKey, primaryOwner, secondaryOwner);
        merger.merge(primaryKey, secondaryKey, primaryOwner, secondaryOwner);
        persist();
        return Result.LINKED;
    }

    private void link(String primaryKey, String secondaryKey, String primaryOwner, String secondaryOwner) {
        LinkRecord record = new LinkRecord(secondaryKey, primaryOwner);
        record.aliases.add(secondaryOwner);
        links.put(primaryKey, record);
        canonicalByKey.put(primaryKey, primaryKey);
        canonicalByKey.put(secondaryKey, primaryKey);
        canonicalByOwner.put(primaryOwner, primaryOwner);
        canonicalByOwner.put(secondaryOwner, primaryOwner);
    }

    public void persist() {
        if (dataFile == null) {
            return;
        }
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<String, LinkRecord> entry : links.entrySet()) {
            LinkRecord record = entry.getValue();
            out.set("links." + entry.getKey() + ".other", record.otherKey);
            out.set("links." + entry.getKey() + ".owner", record.owner);
            if (!record.aliases.isEmpty()) {
                out.set("links." + entry.getKey() + ".aliases", new ArrayList<>(record.aliases));
            }
        }
        for (Map.Entry<String, PendingLink> entry : pending.entrySet()) {
            PendingLink p = entry.getValue();
            out.set("pending." + entry.getKey() + ".startKey", p.startKey);
            out.set("pending." + entry.getKey() + ".startOwner", p.startOwner);
            out.set("pending." + entry.getKey() + ".expires", p.expiresAt);
        }
        try {
            if (dataFile.getParentFile() != null) {
                dataFile.getParentFile().mkdirs();
            }
            out.save(dataFile);
        } catch (IOException ignored) {
            // best-effort persistence
        }
    }

    private void load() {
        if (dataFile == null || !dataFile.exists()) {
            return;
        }
        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection linksSection = onDisk.getConfigurationSection("links");
        if (linksSection != null) {
            for (String canonical : linksSection.getKeys(false)) {
                String other = linksSection.getString(canonical + ".other");
                String owner = linksSection.getString(canonical + ".owner");
                if (other == null || owner == null) {
                    continue;
                }
                LinkRecord record = new LinkRecord(other, owner);
                record.aliases.addAll(linksSection.getStringList(canonical + ".aliases"));
                links.put(canonical, record);
                canonicalByKey.put(canonical, canonical);
                canonicalByKey.put(other, canonical);
                canonicalByOwner.put(owner, owner);
                for (String alias : record.aliases) {
                    canonicalByOwner.put(alias, owner);
                }
            }
        }
        ConfigurationSection pendingSection = onDisk.getConfigurationSection("pending");
        if (pendingSection != null) {
            for (String code : pendingSection.getKeys(false)) {
                String startKey = pendingSection.getString(code + ".startKey");
                String startOwner = pendingSection.getString(code + ".startOwner");
                long expires = pendingSection.getLong(code + ".expires");
                if (startKey == null) {
                    continue;
                }
                pending.put(code, new PendingLink(startKey, startOwner, expires));
            }
        }
    }

    private static final class PendingLink {
        final String startKey;
        final String startOwner;
        final long expiresAt;

        PendingLink(String startKey, String startOwner, long expiresAt) {
            this.startKey = startKey;
            this.startOwner = startOwner;
            this.expiresAt = expiresAt;
        }
    }

    private static final class LinkRecord {
        final String otherKey;
        final String owner;
        final Set<String> aliases = new HashSet<>();

        LinkRecord(String otherKey, String owner) {
            this.otherKey = otherKey;
            this.owner = owner;
        }
    }

    public enum Result {
        LINKED,
        NO_SUCH_CODE,
        EXPIRED,
        ALREADY_LINKED,
        SELF_LINK
    }
}