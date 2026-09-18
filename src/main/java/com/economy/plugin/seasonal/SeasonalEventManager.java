package com.economy.plugin.seasonal;

import com.economy.plugin.market.Commodity;
import com.economy.plugin.market.MarketManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SeasonalEventManager {
    private static final int HISTORY_LIMIT = 25;
    private final double crisisChance;
    private final double boomChance;
    private final double rareLootChance;
    private final Random random = new Random();
    private final List<MarketEvent> activeEvents = new ArrayList<>();
    private final List<MarketEvent> history = new ArrayList<>();
    private long nextEventTime = System.currentTimeMillis();

    public SeasonalEventManager(double crisisChance, double boomChance, double rareLootChance) {
        this.crisisChance = crisisChance;
        this.boomChance = boomChance;
        this.rareLootChance = rareLootChance;
    }

    public List<MarketEvent> getActiveEvents() {
        activeEvents.removeIf(e -> !e.isActive());
        return activeEvents;
    }

    public List<MarketEvent> getHistory() {
        List<MarketEvent> copy = new ArrayList<>(history);
        Collections.reverse(copy);
        return copy;
    }

    public long getNextEventTime() {
        return nextEventTime;
    }

    public boolean isEventReady() {
        return System.currentTimeMillis() >= nextEventTime;
    }

    public void scheduleNextEvent(long minIntervalMillis, long maxIntervalMillis) {
        long delay = minIntervalMillis + random.nextInt((int) (maxIntervalMillis - minIntervalMillis + 1L));
        nextEventTime = System.currentTimeMillis() + delay;
    }

    public MarketEvent rollEvent(MarketManager market) {
        if (market == null || market.getAll().isEmpty()) {
            return null;
        }
        double roll = random.nextDouble();
        EventType type = EventType.NONE;
        if (roll < crisisChance) {
            type = EventType.CRISIS;
        } else if (roll < crisisChance + boomChance) {
            type = EventType.BOOM;
        }
        if (type == EventType.NONE) {
            return null;
        }
        List<Commodity> all = new ArrayList<>(market.getAll());
        Commodity target = all.get(random.nextInt(all.size()));
        double multiplier = type == EventType.CRISIS ? 0.35 : 2.2;
        long duration = 300000L + random.nextInt(300000);
        target.applyLootShift(multiplier);
        MarketEvent event = new MarketEvent(type, target.getId(), multiplier, duration);
        activeEvents.clear();
        activeEvents.add(event);
        history.add(event);
        if (history.size() > HISTORY_LIMIT) {
            history.remove(0);
        }
        return event;
    }

    public boolean isRareLootAvailable() {
        return random.nextDouble() < rareLootChance;
    }

    public static class MarketEvent {
        public final EventType type;
        public final String commodityId;
        public final double multiplier;
        public final long startedAt;
        public final long durationMillis;

        public MarketEvent(EventType type, String commodityId, double multiplier, long durationMillis) {
            this.type = type;
            this.commodityId = commodityId;
            this.multiplier = multiplier;
            this.startedAt = System.currentTimeMillis();
            this.durationMillis = durationMillis;
        }

        public boolean isActive() {
            return System.currentTimeMillis() - startedAt < durationMillis;
        }
    }

    public enum EventType {
        CRISIS,
        BOOM,
        NONE
    }
}