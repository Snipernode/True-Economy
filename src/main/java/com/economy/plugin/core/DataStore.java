package com.economy.plugin.core;

import java.util.Map;
import java.util.Optional;

public interface DataStore {
    void saveAccount(Account account);

    Optional<Account> loadAccount(String owner);

    Map<String, Account> loadAllAccounts(double startingBalance);

    void deleteAccount(String owner);

    void shutdown();
}