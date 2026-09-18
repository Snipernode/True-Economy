package com.economy.plugin.government;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GovernmentManager {
    private final Map<String, CityWallet> wallets = new ConcurrentHashMap<>();
    private final double transactionTaxPercent;
    private final boolean grantsEnabled;
    private final int grantCooldownDays;
    private final Map<String, Long> lastGrant = new ConcurrentHashMap<>();

    public GovernmentManager(double transactionTaxPercent, boolean grantsEnabled, int grantCooldownDays) {
        this.transactionTaxPercent = transactionTaxPercent;
        this.grantsEnabled = grantsEnabled;
        this.grantCooldownDays = grantCooldownDays;
    }

    public CityWallet getWallet(String region) {
        return wallets.computeIfAbsent(region, CityWallet::new);
    }

    public List<CityWallet> allWallets() {
        return new ArrayList<>(wallets.values());
    }

    public double levyTransactionTax(String region, double transactionAmount) {
        if (transactionTaxPercent <= 0.0) {
            return 0.0;
        }
        double tax = transactionAmount * transactionTaxPercent;
        getWallet(region).collectTax(tax);
        return tax;
    }

    public double getTransactionTaxPercent() {
        return transactionTaxPercent;
    }

    public boolean isGrantEligible(String owner, String region) {
        if (!grantsEnabled) {
            return false;
        }
        Long last = lastGrant.get(owner + ":" + region);
        if (last == null) {
            return true;
        }
        long cooldownMillis = grantCooldownDays * 24L * 3600L * 1000L;
        return System.currentTimeMillis() - last >= cooldownMillis;
    }

    public boolean approveGrant(String owner, String region, double amount) {
        if (!isGrantEligible(owner, region)) {
            return false;
        }
        getWallet(region).grant(amount);
        lastGrant.put(owner + ":" + region, System.currentTimeMillis());
        return true;
    }
}