package com.economy.plugin.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlDataStore implements DataStore {
    private final File dir;
    private final Map<String, YamlConfiguration> cache = new ConcurrentHashMap<>();

    public YamlDataStore(File dir) {
        this.dir = dir;
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private File fileFor(String owner) {
        return new File(dir, owner.hashCode() + ".yml");
    }

    private YamlConfiguration load(String owner) {
        YamlConfiguration config = cache.get(owner);
        if (config == null) {
            File file = fileFor(owner);
            config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
            cache.put(owner, config);
        }
        return config;
    }

    @Override
    public void saveAccount(Account account) {
        YamlConfiguration config = load(account.getOwner());
        config.set("owner", account.getOwner());
        config.set("balance", account.getBalance());
        config.set("bank", account.getBankBalance());
        config.set("credit", account.getCreditScore());
        try {
            config.save(fileFor(account.getOwner()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save account for " + account.getOwner(), e);
        }
    }

    @Override
    public Optional<Account> loadAccount(String owner) {
        YamlConfiguration config = load(owner);
        if (!config.contains("owner")) {
            return Optional.empty();
        }
        return Optional.of(readAccount(config));
    }

    private Account readAccount(YamlConfiguration config) {
        String owner = config.getString("owner");
        Account acc = new Account(owner, 0.0);
        acc.restoreForLoad(config.getDouble("balance", 0.0), config.getDouble("bank", 0.0), config.getInt("credit", 500));
        return acc;
    }

    @Override
    public Map<String, Account> loadAllAccounts(double startingBalance) {
        Map<String, Account> result = new HashMap<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String owner = config.getString("owner");
            if (owner == null) {
                continue;
            }
            result.put(owner, readAccount(config));
        }
        return result;
    }

    @Override
    public void deleteAccount(String owner) {
        cache.remove(owner);
        File file = fileFor(owner);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void shutdown() {
        cache.clear();
    }
}