package com.economy.plugin.market;

public class Commodity {
    private final String id;
    private final String displayName;
    private final double basePrice;
    private final double elasticity;
    private double currentPrice;
    private double supply;
    private double demand;
    private long lastTradeTime;

    public Commodity(String id, String displayName, double basePrice, double elasticity) {
        this.id = id;
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.elasticity = elasticity;
        this.currentPrice = basePrice;
        this.supply = 100.0;
        this.demand = 50.0;
        this.lastTradeTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getElasticity() {
        return elasticity;
    }

    public double getSupply() {
        return supply;
    }

    public double getDemand() {
        return demand;
    }

    public synchronized double getCurrentPrice() {
        long elapsed = System.currentTimeMillis() - lastTradeTime;
        double decayMinutes = elapsed / 60000.0;
        double decayFactor = Math.max(0.0, 1.0 - decayMinutes * 0.01);
        double reverted = basePrice + (currentPrice - basePrice) * decayFactor;
        return capPrice(reverted);
    }

    public synchronized double buy(double units, double volatility) {
        demand += units;
        supply -= units * 0.05;
        if (supply < 1.0) {
            supply = 1.0;
        }
        lastTradeTime = System.currentTimeMillis();
        return recompute(volatility);
    }

    public synchronized double sell(double units, double volatility) {
        supply += units * 0.5;
        demand -= units * 0.2;
        if (demand < 1.0) {
            demand = 1.0;
        }
        lastTradeTime = System.currentTimeMillis();
        return recompute(volatility);
    }

    private synchronized double recompute(double volatility) {
        double ratio = demand / supply;
        double target = basePrice * Math.pow(ratio, elasticity);
        double move = (target - currentPrice) * volatility;
        currentPrice = capPrice(currentPrice + move);
        return currentPrice;
    }

    private static double capPrice(double price) {
        return Math.max(0.01, price);
    }

    public synchronized void applyLootShift(double factor) {
        currentPrice = capPrice(currentPrice * factor);
        lastTradeTime = System.currentTimeMillis();
    }
}