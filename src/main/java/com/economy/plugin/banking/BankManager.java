package com.economy.plugin.banking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BankManager {
    private final double interestRatePerCycle;
    private final long interestCycleMillis;
    private final double maxAccountBalance;
    private final boolean loansEnabled;
    private final double loanInterestPercent;
    private final double maxLoanAmount;
    private final long repaymentPeriodSeconds;
    private final int defaultMinCreditScore;
    private final int defaultPenalty;
    private final int creditRepairPerRepayment;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Loan>> loans = new ConcurrentHashMap<>();
    private long lastInterestTick;
    private final Function<String, String> ownerResolver;

    public BankManager(double interestRatePerCycle, long interestCycleSeconds, double maxAccountBalance, boolean loansEnabled, double loanInterestPercent, double maxLoanAmount, long repaymentPeriodSeconds, int defaultMinCreditScore, int defaultPenalty, int creditRepairPerRepayment) {
        this(interestRatePerCycle, interestCycleSeconds, maxAccountBalance, loansEnabled, loanInterestPercent, maxLoanAmount, repaymentPeriodSeconds, defaultMinCreditScore, defaultPenalty, creditRepairPerRepayment, Function.identity());
    }

    public BankManager(double interestRatePerCycle, long interestCycleSeconds, double maxAccountBalance, boolean loansEnabled, double loanInterestPercent, double maxLoanAmount, long repaymentPeriodSeconds, int defaultMinCreditScore, int defaultPenalty, int creditRepairPerRepayment, Function<String, String> ownerResolver) {
        this.interestRatePerCycle = interestRatePerCycle;
        this.interestCycleMillis = interestCycleSeconds * 1000L;
        this.maxAccountBalance = maxAccountBalance;
        this.loansEnabled = loansEnabled;
        this.loanInterestPercent = loanInterestPercent;
        this.maxLoanAmount = maxLoanAmount;
        this.repaymentPeriodSeconds = repaymentPeriodSeconds;
        this.defaultMinCreditScore = defaultMinCreditScore;
        this.defaultPenalty = defaultPenalty;
        this.creditRepairPerRepayment = creditRepairPerRepayment;
        this.ownerResolver = ownerResolver == null ? Function.identity() : ownerResolver;
        this.lastInterestTick = System.currentTimeMillis();
    }

    private String normalize(String name) {
        return ownerResolver.apply(name);
    }

    public boolean isLoanAllowed(int creditScore) {
        return loansEnabled && creditScore >= defaultMinCreditScore;
    }

    public double getMaxLoanAmount() {
        return maxLoanAmount;
    }

    public double getLoanInterestPercent() {
        return loanInterestPercent;
    }

    public int getDefaultMinCreditScore() {
        return defaultMinCreditScore;
    }

    public double getMaxAccountBalance() {
        return maxAccountBalance;
    }

    public List<Loan> getLoans(String owner) {
        return loans.getOrDefault(normalize(owner), new CopyOnWriteArrayList<>());
    }

    public Collection<CopyOnWriteArrayList<Loan>> allLoans() {
        return loans.values();
    }

    public Loan createLoan(String borrower, double principal, int creditScore) {
        if (!isLoanAllowed(creditScore)) {
            throw new IllegalArgumentException("Credit score too low for a loan");
        }
        if (principal > maxLoanAmount) {
            throw new IllegalArgumentException("Loan exceeds maximum amount");
        }
        String canonical = normalize(borrower);
        Loan loan = new Loan(canonical, principal, loanInterestPercent, repaymentPeriodSeconds);
        loans.computeIfAbsent(canonical, k -> new CopyOnWriteArrayList<>()).add(loan);
        return loan;
    }

    public boolean repayLoan(Loan loan, double amount) {
        boolean paid = loan.repay(amount);
        return paid && loan.isFullyRepaid();
    }

    public int creditChangeForRepayment() {
        return creditRepairPerRepayment;
    }

    public int creditPenaltyForDefault() {
        return defaultPenalty;
    }

    public int tickInterest() {
        long now = System.currentTimeMillis();
        if (now - lastInterestTick < interestCycleMillis) {
            return 0;
        }
        lastInterestTick = now;
        return interestRatePerCycle > 0.0 ? 1 : 0;
    }

    public long getInterestCycleMillis() {
        return interestCycleMillis;
    }

    public List<Loan> findDefaulted() {
        return loans.values().stream()
                .flatMap(Collection::stream)
                .filter(Loan::isOverdue)
                .collect(Collectors.toList());
    }
}