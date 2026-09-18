package com.economy.plugin.auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuctionHouse {
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<UUID, Auction> resolved = new ConcurrentHashMap<>();
    private final double aiBidderChance;
    private final double aiBudgetMultiplier;
    private final double minIncrementPercent;
    private final double salesTax;
    private final double listingFee;
    private final Function<String, String> ownerResolver;

    public AuctionHouse(double aiBidderChance, double aiBudgetMultiplier, double minIncrementPercent, double salesTax, double listingFee) {
        this(aiBidderChance, aiBudgetMultiplier, minIncrementPercent, salesTax, listingFee, Function.identity());
    }

    public AuctionHouse(double aiBidderChance, double aiBudgetMultiplier, double minIncrementPercent, double salesTax, double listingFee, Function<String, String> ownerResolver) {
        this.aiBidderChance = aiBidderChance;
        this.aiBudgetMultiplier = aiBudgetMultiplier;
        this.minIncrementPercent = minIncrementPercent;
        this.salesTax = salesTax;
        this.listingFee = listingFee;
        this.ownerResolver = ownerResolver == null ? Function.identity() : ownerResolver;
    }

    private String normalize(String name) {
        return ownerResolver.apply(name);
    }

    public void add(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public Auction get(UUID id) {
        return auctions.get(id);
    }

    public Auction getResolved(UUID id) {
        return resolved.get(id);
    }

    public List<Auction> getAllResolved() {
        return new CopyOnWriteArrayList<>(resolved.values());
    }

    public void remove(UUID id) {
        auctions.remove(id);
    }

    public void resolvedKeep(Auction auction) {
        resolved.put(auction.getId(), auction);
    }

    public List<Auction> getAll() {
        return new CopyOnWriteArrayList<>(auctions.values());
    }

    public List<Auction> getActive() {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == Auction.Status.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Auction> getBySeller(String seller) {
        String canonical = normalize(seller);
        return auctions.values().stream()
                .filter(a -> Objects.equals(normalize(a.getSeller()), canonical))
                .collect(Collectors.toList());
    }

    public List<Auction> getByWinner(String winner) {
        String canonical = normalize(winner);
        return auctions.values().stream()
                .filter(a -> a.getHighestBidder() != null && Objects.equals(normalize(a.getHighestBidder()), canonical))
                .collect(Collectors.toList());
    }

    public boolean placeBid(Auction auction, String bidder, double amount) {
        if (auction == null) {
            return false;
        }
        return auction.placeBid(normalize(bidder), amount, minIncrementPercent);
    }

    public double buyNow(Auction auction, String buyer, double fee) {
        if (auction == null) {
            return -1.0;
        }
        return auction.buyNow(normalize(buyer), fee);
    }

    public double getAiBidderChance() {
        return aiBidderChance;
    }

    public double getAiBudgetMultiplier() {
        return aiBudgetMultiplier;
    }

    public double getMinIncrementPercent() {
        return minIncrementPercent;
    }

    public double getSalesTax() {
        return salesTax;
    }

    public double getListingFee() {
        return listingFee;
    }

    public List<Auction> processExpirations() {
        List<Auction> resolvedList = new ArrayList<>();
        for (Auction auction : getAll()) {
            if (auction.getStatus() != Auction.Status.ACTIVE || !auction.isExpired()) {
                continue;
            }
            if (auction.getHighestBidder() != null) {
                auction.finalizeSale(auction.getHighestBidder());
            } else {
                auction.setStatus(Auction.Status.EXPIRED);
            }
            resolvedList.add(auction);
            auctions.remove(auction.getId());
            resolved.put(auction.getId(), auction);
        }
        return resolvedList;
    }
}