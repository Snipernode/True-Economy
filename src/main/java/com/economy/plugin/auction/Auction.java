package com.economy.plugin.auction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class Auction {
    private final UUID id;
    private final String seller;
    private final ItemStack item;
    private final long endTime;
    private final boolean buyItNow;
    private final double buyPrice;
    private final double startingBid;
    private double currentBid;
    private String highestBidder;
    private Status status = Status.ACTIVE;
    private final List<Bid> bidHistory = new ArrayList<>();
    private boolean aiCompeting = false;

    public Auction(UUID id, String seller, ItemStack item, long seconds, boolean buyItNow, double buyPrice, double startingBid) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.endTime = System.currentTimeMillis() + seconds * 1000L;
        this.buyItNow = buyItNow;
        this.buyPrice = buyPrice;
        this.startingBid = startingBid;
        this.currentBid = startingBid;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }

    public synchronized boolean placeBid(String bidder, double amount, double minIncrementPercent) {
        if (status != Status.ACTIVE) {
            return false;
        }
        double minNext = currentBid * (1.0 + minIncrementPercent);
        if (amount < minNext) {
            return false;
        }
        currentBid = amount;
        highestBidder = bidder;
        bidHistory.add(new Bid(bidder, amount, System.currentTimeMillis()));
        return true;
    }

    public synchronized double buyNow(String buyer, double fee) {
        if (status != Status.ACTIVE || !buyItNow) {
            return -1.0;
        }
        currentBid = buyPrice;
        highestBidder = buyer;
        status = Status.SOLD_TO_PLAYER;
        return buyPrice * (1.0 - fee);
    }

    public synchronized void finalizeSale(String winner) {
        status = Status.SOLD;
        highestBidder = winner;
    }

    public synchronized void markClaimed() {
        if (status == Status.SOLD || status == Status.SOLD_TO_PLAYER || status == Status.EXPIRED) {
            status = Status.CLAIMED;
        }
    }

    public void setAiCompeting(boolean aiCompeting) {
        this.aiCompeting = aiCompeting;
    }

    public boolean isAiCompeting() {
        return aiCompeting;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isBuyItNow() {
        return buyItNow;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getStartingBid() {
        return startingBid;
    }

    public synchronized double getCurrentBid() {
        return currentBid;
    }

    public synchronized String getHighestBidder() {
        return highestBidder;
    }

    public List<Bid> getBidHistory() {
        return bidHistory;
    }

    public static class Bid {
        public final String bidder;
        public final double amount;
        public final long time;

        public Bid(String bidder, double amount, long time) {
            this.bidder = bidder;
            this.amount = amount;
            this.time = time;
        }
    }

    public enum Status {
        ACTIVE,
        SOLD,
        SOLD_TO_PLAYER,
        EXPIRED,
        CLAIMED
    }
}