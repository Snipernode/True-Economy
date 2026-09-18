package com.economy.plugin.banking;

public class Loan {
    private final String borrower;
    private final double principal;
    private final double interestRate;
    private final long repaymentDeadline;
    private double remaining;
    private Status status = Status.ACTIVE;

    public Loan(String borrower, double principal, double interestRate, long repaymentPeriodSeconds) {
        this.borrower = borrower;
        this.principal = principal;
        this.interestRate = interestRate;
        this.remaining = principal * (1.0 + interestRate);
        this.repaymentDeadline = System.currentTimeMillis() + repaymentPeriodSeconds * 1000L;
    }

    public String getBorrower() {
        return borrower;
    }

    public double getPrincipal() {
        return principal;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public double getRemaining() {
        return remaining;
    }

    public long getRepaymentDeadline() {
        return repaymentDeadline;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized boolean isOverdue() {
        return status == Status.ACTIVE && System.currentTimeMillis() > repaymentDeadline;
    }

    public synchronized boolean repay(double amount) {
        if (status != Status.ACTIVE || amount <= 0.0) {
            return false;
        }
        remaining -= amount;
        if (remaining <= 1.0E-4) {
            remaining = 0.0;
            status = Status.REPAID;
        }
        return true;
    }

    public synchronized void defaultLoan() {
        if (status == Status.ACTIVE) {
            status = Status.DEFAULTED;
        }
    }

    public boolean isFullyRepaid() {
        return status == Status.REPAID;
    }

    public enum Status {
        ACTIVE,
        REPAID,
        DEFAULTED
    }
}