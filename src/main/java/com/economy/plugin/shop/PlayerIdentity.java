package com.economy.plugin.shop;

import java.util.Locale;
import java.util.UUID;

public final class PlayerIdentity {
    private final UUID uuid;
    private final String xuid;

    public PlayerIdentity(UUID uuid, String xuid) {
        this.uuid = uuid;
        this.xuid = xuid == null ? null : xuid.toLowerCase(Locale.ROOT);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getXuid() {
        return xuid;
    }

    public String getKey() {
        if (xuid != null && !xuid.isEmpty()) {
            return "xuid:" + xuid;
        }
        return "uuid:" + (uuid == null ? "" : uuid.toString().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerIdentity)) {
            return false;
        }
        PlayerIdentity other = (PlayerIdentity) o;
        return getKey().equals(other.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        return getKey();
    }
}