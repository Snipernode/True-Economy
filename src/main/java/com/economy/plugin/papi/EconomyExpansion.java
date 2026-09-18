package com.economy.plugin.papi;

import com.economy.plugin.EconomyPlugin;
import com.economy.plugin.core.Account;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class EconomyExpansion extends PlaceholderExpansion {
    private final EconomyPlugin plugin;

    public EconomyExpansion(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "trueeconomy";
    }

    @Override
    public String getAuthor() {
        return "Snipernode";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);

        if (params == null) {
            return "";
        }
        if (params.equalsIgnoreCase("balance")) {
            return plugin.getEconomy().format(acc.getBalance());
        }
        if (params.equalsIgnoreCase("balance_raw")) {
            return String.format("%.2f", acc.getBalance());
        }
        if (params.equalsIgnoreCase("bank")) {
            return plugin.getEconomy().format(acc.getBankBalance());
        }
        if (params.equalsIgnoreCase("bank_raw")) {
            return String.format("%.2f", acc.getBankBalance());
        }
        if (params.equalsIgnoreCase("credit")) {
            return String.valueOf(acc.getCreditScore());
        }
        if (params.equalsIgnoreCase("symbol")) {
            return plugin.getEconomy().getSymbol();
        }
        if (params.equalsIgnoreCase("tax")) {
            return String.valueOf((int) Math.round(plugin.getGovernment().getTransactionTaxPercent() * 100.0));
        }
        if (params.equalsIgnoreCase("rank")) {
            int rank = plugin.getEconomy().getRank(owner);
            return rank < 0 ? "-" : String.valueOf(rank);
        }
        if (params.startsWith("top_")) {
            return topPlaceholder(params);
        }
        return null;
    }

    private String topPlaceholder(String params) {
        String[] parts = params.split("_");
        if (parts.length < 3) {
            return null;
        }
        int rank;
        try {
            rank = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (rank < 1) {
            return null;
        }
        List<Account> top = plugin.getEconomy().getTopAccounts(10);
        if (rank > top.size()) {
            return "-";
        }
        Account acc = top.get(rank - 1);
        String what = parts[2];
        if (what.equalsIgnoreCase("name")) {
            return acc.getOwner();
        }
        if (what.equalsIgnoreCase("balance")) {
            return plugin.getEconomy().format(acc.getBalance());
        }
        return null;
    }
}