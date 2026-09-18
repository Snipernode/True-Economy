package com.economy.plugin.government;

public class CityWallet {
    private final String region;
    private double balance;
    private double totalCollected;
    private double corruptionLevel = 0.0;

    public CityWallet(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalCollected() {
        return totalCollected;
    }

    public double getCorruptionLevel() {
        return corruptionLevel;
    }

    public synchronized void collectTax(double amount) {
        balance += amount;
        totalCollected += amount;
        corruptionLevel += amount * 1.0E-4;
    }

    public synchronized boolean grant(double amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public synchronized double embezzle(double amount) {
        double taken = Math.min(amount, balance);
        balance -= taken;
        corruptionLevel += taken * 0.05;
        return taken;
    }

    public synchronized double applyEfficiency(double efficiencyFactor) {
        double skimmed = balance * (1.0 - efficiencyFactor);
        balance -= skimmed;
        corruptionLevel *= efficiencyFactor;
        return skimmed;
    }
}