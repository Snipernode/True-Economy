package com.economy.plugin.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.inventory.ItemStack;

public class PlayerShop {
    private final String owner;
    private final String name;
    private final double startingCapital;
    private final Set<String> employees = new HashSet<>();
    private final List<ItemStack> inventory = new ArrayList<>();
    private final Function<String, String> ownerResolver;
    private double revenue;
    private double productionLineOutput = 0.0;
    private double wageRate;
    private final int maxEmployees;

    public PlayerShop(String owner, String name, double startingCapital, double defaultWage, Function<String, String> ownerResolver) {
        this.owner = owner;
        this.name = name;
        this.startingCapital = startingCapital;
        this.wageRate = defaultWage;
        this.maxEmployees = 5;
        this.ownerResolver = ownerResolver == null ? Function.identity() : ownerResolver;
    }

    public PlayerShop(String owner, String name, double startingCapital, double defaultWage) {
        this(owner, name, startingCapital, defaultWage, Function.identity());
    }

    private String normalize(String name) {
        return ownerResolver.apply(name);
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getWageRate() {
        return wageRate;
    }

    public int getMaxEmployees() {
        return maxEmployees;
    }

    public Set<String> getEmployees() {
        return employees;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }

    public boolean hire(String employee) {
        String canonical = normalize(employee);
        if (employees.size() >= maxEmployees || employees.contains(canonical)) {
            return false;
        }
        return employees.add(canonical);
    }

    public boolean fire(String employee) {
        return employees.remove(normalize(employee));
    }

    public void setWageRate(double wageRate) {
        this.wageRate = Math.max(0.0, wageRate);
    }

    public synchronized void addRevenue(double amount) {
        revenue += amount;
    }

    public synchronized void expandProductionLine(double amount) {
        productionLineOutput += amount;
    }

    public synchronized double getProductionLineOutput() {
        return productionLineOutput;
    }

    public double wagesOwed() {
        return employees.size() * wageRate;
    }

    public synchronized double netProfitAfterWages() {
        return revenue - wagesOwed();
    }
}