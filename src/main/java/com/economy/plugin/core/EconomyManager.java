package com.economy.plugin.core;

import com.economy.plugin.logging.TransactionLogger;
import com.economy.plugin.logging.TransactionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final String symbol;
    private final int decimals;
    private final double startingBalance;
    private final double maxBalance;
    private TransactionLogger logger;

    public EconomyManager(String symbol, int decimals, double startingBalance, double maxBalance) {
        this.symbol = symbol;
        this.decimals = decimals;
        this.startingBalance = startingBalance;
        this.maxBalance = maxBalance > 0.0 ? maxBalance : 0.0;
    }

    public void setLogger(TransactionLogger logger) {
        this.logger = logger;
    }

    public Account getAccount(String owner) {
        return accounts.computeIfAbsent(owner, k -> new Account(k, startingBalance));
    }

    public Optional<Account> getExistingAccount(String owner) {
        return Optional.ofNullable(accounts.get(owner));
    }

    public boolean hasAccount(String owner) {
        return accounts.containsKey(owner);
    }

    public Set<String> getOwnerNames() {
        return accounts.keySet();
    }

    public boolean transfer(String from, String to, double amount) {
        if (amount < 0.0) {
            return false;
        }
        Account fromAcc = getAccount(from);
        if (!fromAcc.canAfford(amount)) {
            return false;
        }
        if (!fromAcc.modifyBalance(-amount)) {
            return false;
        }
        Account toAcc = getAccount(to);
        if (maxBalance > 0.0) {
            double deposit = Math.min(amount, Math.max(0.0, maxBalance - toAcc.getBalance()));
            toAcc.modifyBalance(deposit);
        } else {
            toAcc.modifyBalance(amount);
        }
        log(TransactionType.TRANSFER, from, to, amount, null);
        return true;
    }

    public boolean deposit(String owner, double amount) {
        if (amount < 0.0) {
            return false;
        }
        Account acc = getAccount(owner);
        if (maxBalance > 0.0) {
            amount = Math.min(amount, Math.max(0.0, maxBalance - acc.getBalance()));
        }
        if (amount <= 0.0) {
            return false;
        }
        if (!acc.modifyBalance(amount)) {
            return false;
        }
        log(TransactionType.DEPOSIT, null, owner, amount, null);
        return true;
    }

    public boolean withdraw(String owner, double amount) {
        if (amount < 0.0) {
            return false;
        }
        Account acc = getAccount(owner);
        if (!acc.modifyBalance(-amount)) {
            return false;
        }
        log(TransactionType.WITHDRAW, owner, null, amount, null);
        return true;
    }

    public boolean setBalance(String owner, double amount) {
        Account acc = getAccount(owner);
        if (!acc.setBalance(amount)) {
            return false;
        }
        log(TransactionType.ADMIN_SET, null, owner, amount, null);
        return true;
    }

    public boolean removeAccount(String owner) {
        return accounts.remove(owner) != null;
    }

    public List<Account> getTopAccounts(int limit) {
        List<Account> sorted = new ArrayList<>(accounts.values());
        sorted.removeIf(a -> a.getOwner().startsWith("__"));
        sorted.sort(Comparator.comparingDouble(Account::getBalance).reversed());
        if (sorted.size() > limit) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }

    public int getRank(String owner) {
        List<Account> sorted = getTopAccounts(Integer.MAX_VALUE);
        String canonical = owner;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getOwner().equals(canonical)) {
                return i + 1;
            }
        }
        return -1;
    }

    public String format(double amount) {
        String fmt = String.format("%." + decimals + "f", amount);
        return symbol + fmt;
    }

    public double round(double amount) {
        double mult = Math.pow(10.0, decimals);
        return Math.round(amount * mult) / mult;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    private void log(TransactionType type, String from, String to, double amount, String detail) {
        if (logger != null) {
            logger.log(type, from, to, amount, detail);
        }
    }
}