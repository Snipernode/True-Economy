package com.economy.plugin.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class EconomyConfig {
    private final FileConfiguration config;

    public EconomyConfig(FileConfiguration config) {
        this.config = config;
    }

    public String currencySymbol() {
        return config.getString("currency.symbol", "$");
    }

    public int decimals() {
        return config.getInt("currency.decimals", 2);
    }

    public double startingBalance() {
        return config.getDouble("currency.starting-balance", 100.0);
    }

    public double maxBalance() {
        return config.getDouble("currency.max-balance", 1000000000.0);
    }

    public String dataDir() {
        return config.getString("database.dir", "data");
    }

    public String dbType() {
        return config.getString("database.type", "yaml").toLowerCase();
    }

    public String sqliteFile() {
        return config.getString("database.sqlite.file", "economy.db");
    }

    public String mysqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int mysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String mysqlDatabase() {
        return config.getString("database.mysql.database", "economy");
    }

    public String mysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String mysqlPassword() {
        return config.getString("database.mysql.password", "");
    }

    public boolean loggingEnabled() {
        return config.getBoolean("logging.enabled", true);
    }

    public String loggingFile() {
        return config.getString("logging.file", "transactions.log");
    }

    public boolean marketEnabled() {
        return config.getBoolean("market.enabled", true);
    }

    public double marketVolatility() {
        return config.getDouble("market.volatility", 0.03);
    }

    public long marketTick() {
        return config.getLong("market.tick-interval", 300L);
    }

    public boolean auctionEnabled() {
        return config.getBoolean("auction.enabled", true);
    }

    public double aiBidderChance() {
        return config.getDouble("auction.ai-bidder-chance", 0.15);
    }

    public double aiBudgetMultiplier() {
        return config.getDouble("auction.ai-bidder-max-budget-multiplier", 1.5);
    }

    public double minIncrement() {
        return config.getDouble("auction.min-increment-percent", 0.05);
    }

    public double auctionSalesTax() {
        return config.getDouble("auction.sales-tax-percent", 0.02);
    }

    public double auctionListingFee() {
        return config.getDouble("auction.listing-fee", 5.0);
    }

    public long auctionDefaultDuration() {
        return config.getLong("auction.default-duration-seconds", 300L);
    }

    public long auctionMaxDuration() {
        return config.getLong("auction.max-duration-seconds", 86400L);
    }

    public boolean auctionBuyItNowEnabled() {
        return config.getBoolean("auction.buy-it-now-enabled", true);
    }

    public boolean bankingEnabled() {
        return config.getBoolean("banking.enabled", true);
    }

    public double interestRate() {
        return config.getDouble("banking.interest-rate-per-cycle", 0.001);
    }

    public long interestCycle() {
        return config.getLong("banking.interest-cycle-seconds", 600L);
    }

    public double maxBankBalance() {
        return config.getDouble("banking.max-account-balance", 1000000.0);
    }

    public boolean loansEnabled() {
        return config.getBoolean("banking.loans.enabled", true);
    }

    public double loanInterest() {
        return config.getDouble("banking.loans.default-interest-percent", 0.05);
    }

    public double maxLoan() {
        return config.getDouble("banking.loans.max-loan-amount", 10000.0);
    }

    public long repaymentPeriod() {
        return config.getLong("banking.loans.repayment-period-seconds", 604800L);
    }

    public int minCreditScore() {
        return config.getInt("banking.loans.default-min-credit-score", 300);
    }

    public int defaultPenalty() {
        return config.getInt("banking.loans.default-penalty", 100);
    }

    public int creditRepair() {
        return config.getInt("banking.loans.credit-repair-per-repayment", 5);
    }

    public boolean npcEnabled() {
        return config.getBoolean("npc.enabled", true);
    }

    public double npcMargin() {
        return config.getDouble("npc.base-margin", 0.3);
    }

    public double npcSensitivity() {
        return config.getDouble("npc.margin-sensitivity", 0.5);
    }

    public boolean shopsEnabled() {
        return config.getBoolean("shops.enabled", true);
    }

    public int maxShops() {
        return config.getInt("shops.max-shops-per-player", 10);
    }

    public int maxEmployees() {
        return config.getInt("shops.max-employees-per-shop", 5);
    }

    public double defaultWage() {
        return config.getDouble("shops.default-wage", 50.0);
    }

    public double shopStartingCapital() {
        return config.getDouble("shops.starting-capital", 0.0);
    }

    public String shopMenuTitle() {
        return config.getString("shops.menu-title", "&2&lTrue Economy Shop");
    }

    public List<String> unobtainableItems() {
        return config.getStringList("shops.unobtainable");
    }

    public List<String> bannedNameParts() {
        return config.getStringList("shops.banned-name-parts");
    }

    public Map<String, Integer> startingStock() {
        Map<String, Integer> map = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("shops.starting-stock");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.getInt(key, 8));
            }
        }
        return map;
    }

    public ConfigurationSection shopSections() {
        return config.getConfigurationSection("shops.sections");
    }

    public boolean governmentEnabled() {
        return config.getBoolean("government.enabled", true);
    }

    public double taxPercent() {
        return config.getDouble("government.transaction-tax-percent", 0.05);
    }

    public boolean grantsEnabled() {
        return config.getBoolean("government.grants.enabled", true);
    }

    public int grantCooldownDays() {
        return config.getInt("government.grants.cooldown-days", 7);
    }

    public boolean seasonalEnabled() {
        return config.getBoolean("seasonal.enabled", true);
    }

    public long seasonalMin() {
        return config.getLong("seasonal.event-interval-min-seconds", 1800L);
    }

    public long seasonalMax() {
        return config.getLong("seasonal.event-interval-max-seconds", 7200L);
    }

    public double crisisChance() {
        return config.getDouble("seasonal.crisis-chance", 0.3);
    }

    public double boomChance() {
        return config.getDouble("seasonal.boom-chance", 0.3);
    }

    public double rareLootChance() {
        return config.getDouble("seasonal.rare-loot-chance", 0.15);
    }
}