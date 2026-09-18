package com.economy.plugin.link;

public interface LinkMerger {
    void merge(String primaryKey, String secondaryKey, String primaryOwner, String secondaryOwner);
}