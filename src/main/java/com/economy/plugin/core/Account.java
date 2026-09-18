package com.economy.plugin.core;

public class Account {
    private final String owner;
    private double balance;
    private double bankBalance;
    private int creditScore = 500;

    public Account(String owner, double startingBalance) {
        this.owner = owner;
        this.balance = startingBalance;
    }

    public String getOwner() {
        return owner;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized double getBankBalance() {
        return bankBalance;
    }

    public synchronized int getCreditScore() {
        return creditScore;
    }

    public synchronized void setCreditScore(int creditScore) {
        this.creditScore = Math.max(0, Math.min(1000, creditScore));
    }

    public synchronized boolean setBalance(double newBalance) {
        if (newBalance < 0.0) {
            return false;
        }
        this.balance = newBalance;
        return true;
    }

    public synchronized boolean modifyBalance(double delta) {
        if (balance + delta < 0.0) {
            return false;
        }
        balance += delta;
        return true;
    }

    public synchronized boolean depositBank(double amount) {
        if (amount < 0.0 || balance < amount) {
            return false;
        }
        balance -= amount;
        bankBalance += amount;
        return true;
    }

    public synchronized boolean withdrawBank(double amount) {
        if (amount < 0.0 || bankBalance < amount) {
            return false;
        }
        bankBalance -= amount;
        balance += amount;
        return true;
    }

    public synchronized void applyBankInterest(double rate) {
        bankBalance += bankBalance * rate;
    }

    public synchronized boolean canAfford(double amount) {
        return balance >= amount;
    }

    public synchronized boolean payFromBank(double amount) {
        if (bankBalance < amount) {
            return false;
        }
        bankBalance -= amount;
        return true;
    }

    public synchronized void restoreForLoad(double wallet, double bank, int credit) {
        this.balance = wallet;
        this.bankBalance = bank;
        this.setCreditScore(credit);
    }

    public synchronized void mergeFrom(Account other) {
        if (other == null || other == this) {
            return;
        }
        synchronized (other) {
            this.balance += other.balance;
            this.bankBalance += other.bankBalance;
            this.creditScore = Math.max(this.creditScore, other.creditScore);
            other.balance = 0.0;
            other.bankBalance = 0.0;
        }
    }
}