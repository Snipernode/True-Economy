package com.economy.plugin.npc;

import com.economy.plugin.market.Commodity;

public class Aimerchant {
    private final String name;
    private double cashReserve;
    private double inventoryLevel;

    public Aimerchant(String name, double startingCash, double startingInventory) {
        this.name = name;
        this.cashReserve = startingCash;
        this.inventoryLevel = startingInventory;
    }

    public String getName() {
        return name;
    }

    public double getCashReserve() {
        return cashReserve;
    }

    public double getInventoryLevel() {
        return inventoryLevel;
    }

    public synchronized void earn(double amount) {
        cashReserve += amount;
    }

    public synchronized void spend(double amount) {
        if (cashReserve < amount) {
            amount = cashReserve;
        }
        cashReserve -= amount;
    }

    public double getBuyPrice(Commodity commodity, double baseMargin, double sensitivity) {
        double marketPrice = commodity.getCurrentPrice();
        double scarcityFactor = Math.max(0.2, 100.0 / commodity.getSupply());
        double margin = baseMargin * sensitivity + (1.0 - sensitivity) * 0.15;
        double offer = marketPrice * 0.7 * scarcityFactor * (1.0 - margin);
        return Math.max(0.01, offer);
    }

    public double getSellPrice(Commodity commodity, double baseMargin, double sensitivity) {
        double marketPrice = commodity.getCurrentPrice();
        double margin = baseMargin * sensitivity + (1.0 - sensitivity) * 0.5;
        return marketPrice * (1.0 + margin);
    }
}