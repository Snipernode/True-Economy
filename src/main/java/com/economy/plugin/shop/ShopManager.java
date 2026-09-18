package com.economy.plugin.shop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class ShopManager {
    private final Map<String, List<PlayerShop>> shopsByOwner = new ConcurrentHashMap<>();
    private final int maxShopsPerPlayer;
    private final int maxEmployeesPerShop;
    private final double defaultWage;
    private final Function<String, String> ownerResolver;

    public ShopManager(int maxShopsPerPlayer, int maxEmployeesPerShop, double defaultWage) {
        this(maxShopsPerPlayer, maxEmployeesPerShop, defaultWage, Function.identity());
    }

    public ShopManager(int maxShopsPerPlayer, int maxEmployeesPerShop, double defaultWage, Function<String, String> ownerResolver) {
        this.maxShopsPerPlayer = maxShopsPerPlayer;
        this.maxEmployeesPerShop = maxEmployeesPerShop;
        this.defaultWage = defaultWage;
        this.ownerResolver = ownerResolver == null ? Function.identity() : ownerResolver;
    }

    private String normalize(String owner) {
        return ownerResolver.apply(owner);
    }

    private List<PlayerShop> listFor(String owner) {
        return shopsByOwner.computeIfAbsent(normalize(owner), k -> new CopyOnWriteArrayList<>());
    }

    public PlayerShop createShop(String owner, String name, double startingCapital) {
        String canonical = normalize(owner);
        if (listFor(canonical).size() >= maxShopsPerPlayer) {
            throw new IllegalStateException("Player has reached the shop limit");
        }
        PlayerShop shop = new PlayerShop(canonical, name, startingCapital, defaultWage, ownerResolver);
        listFor(canonical).add(shop);
        return shop;
    }

    public List<PlayerShop> getShops(String owner) {
        return listFor(owner);
    }

    public List<PlayerShop> allShops() {
        return shopsByOwner.values().stream().flatMap(Collection::stream).collect(java.util.stream.Collectors.toList());
    }

    public void deleteShop(PlayerShop shop) {
        List<PlayerShop> list = shopsByOwner.get(normalize(shop.getOwner()));
        if (list != null) {
            list.remove(shop);
        }
    }

    public int getMaxShopsPerPlayer() {
        return maxShopsPerPlayer;
    }

    public int getMaxEmployeesPerShop() {
        return maxEmployeesPerShop;
    }

    public double getDefaultWage() {
        return defaultWage;
    }
}