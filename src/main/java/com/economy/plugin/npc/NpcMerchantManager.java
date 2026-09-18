package com.economy.plugin.npc;

import com.economy.plugin.market.Commodity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NpcMerchantManager {
    private final Map<String, Aimerchant> merchants = new ConcurrentHashMap<>();
    private final double baseMargin;
    private final double sensitivity;

    public NpcMerchantManager(double baseMargin, double sensitivity) {
        this.baseMargin = baseMargin;
        this.sensitivity = sensitivity;
    }

    public void register(Aimerchant merchant) {
        merchants.put(merchant.getName(), merchant);
    }

    public Aimerchant get(String name) {
        return merchants.get(name);
    }

    public List<Aimerchant> all() {
        return new ArrayList<>(merchants.values());
    }

    public double getBuyOffer(Aimerchant merchant, Commodity commodity) {
        return merchant.getBuyPrice(commodity, baseMargin, sensitivity);
    }

    public double getSellPrice(Aimerchant merchant, Commodity commodity) {
        return merchant.getSellPrice(commodity, baseMargin, sensitivity);
    }

    public double getBaseMargin() {
        return baseMargin;
    }

    public double getSensitivity() {
        return sensitivity;
    }
}