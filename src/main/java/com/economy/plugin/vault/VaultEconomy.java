package com.economy.plugin.vault;

import com.economy.plugin.EconomyPlugin;
import com.economy.plugin.core.EconomyManager;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

public class VaultEconomy implements Economy {
    private final EconomyPlugin plugin;
    private final EconomyManager economy;

    public VaultEconomy(EconomyPlugin plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void register() {
        Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.Normal);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "TrueEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return economy.getDecimals();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return economy.getSymbol();
    }

    @Override
    public String currencyNameSingular() {
        return economy.getSymbol();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return economy.hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return economy.hasAccount(player.getName());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return economy.hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return economy.hasAccount(player.getName());
    }

    @Override
    public double getBalance(String playerName) {
        return economy.getAccount(playerName).getBalance();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economy.getAccount(player.getName()).getBalance();
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return economy.getAccount(playerName).canAfford(amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.getAccount(player.getName()).canAfford(amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdraw(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdraw(player.getName(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdraw(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdraw(player.getName(), amount);
    }

    private EconomyResponse withdraw(String playerName, double amount) {
        if (amount < 0.0) {
            return new EconomyResponse(0.0, amount, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amounts");
        }
        double balance = economy.getAccount(playerName).getBalance();
        if (economy.withdraw(playerName, amount)) {
            return new EconomyResponse(balance, economy.getAccount(playerName).getBalance() - balance,
                    EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(balance, amount, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return deposit(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return deposit(player.getName(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return deposit(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return deposit(player.getName(), amount);
    }

    private EconomyResponse deposit(String playerName, double amount) {
        if (amount < 0.0) {
            return new EconomyResponse(0.0, amount, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amounts");
        }
        double balance = economy.getAccount(playerName).getBalance();
        if (economy.deposit(playerName, amount)) {
            return new EconomyResponse(balance, economy.getAccount(playerName).getBalance() - balance,
                    EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(balance, amount, EconomyResponse.ResponseType.FAILURE, "Account cannot hold that much");
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking is handled by /bank");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        economy.getAccount(playerName);
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        economy.getAccount(player.getName());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}