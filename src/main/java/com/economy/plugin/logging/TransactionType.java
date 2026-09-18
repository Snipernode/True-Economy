package com.economy.plugin.logging;

public enum TransactionType {
    TRANSFER("transfer"),
    TAX("tax"),
    ADMIN_SET("admin-set"),
    ADMIN_GIVE("admin-give"),
    ADMIN_TAKE("admin-take"),
    ADMIN_RESET("admin-reset"),
    DEPOSIT("deposit"),
    WITHDRAW("withdraw"),
    BANK_DEPOSIT("bank-deposit"),
    BANK_WITHDRAW("bank-withdraw"),
    INTEREST("interest"),
    LOAN_ISSUE("loan-issue"),
    LOAN_REPAY("loan-repay"),
    LOAN_DEFAULT("loan-default"),
    AUCTION_LISTING_FEE("auction-listing-fee"),
    AUCTION_BID("auction-bid"),
    AUCTION_REFUND("auction-refund"),
    AUCTION_SALE("auction-sale"),
    AUCTION_FEE("auction-fee"),
    MARKET_BUY("market-buy"),
    MARKET_SELL("market-sell"),
    SHOP_SALE("shop-sale"),
    SHOP_WAGE("shop-wage"),
    GRANT("government-grant"),
    MERGE("identity-merge");

    private final String id;

    TransactionType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}