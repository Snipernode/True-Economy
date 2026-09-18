package com.economy.plugin;

import com.economy.plugin.auction.Auction;
import com.economy.plugin.auction.AuctionHouse;
import com.economy.plugin.banking.BankManager;
import com.economy.plugin.banking.Loan;
import com.economy.plugin.core.Account;
import com.economy.plugin.core.DataStore;
import com.economy.plugin.core.EconomyConfig;
import com.economy.plugin.core.EconomyManager;
import com.economy.plugin.core.SqlDataStore;
import com.economy.plugin.core.YamlDataStore;
import com.economy.plugin.government.GovernmentManager;
import com.economy.plugin.gui.GuiListener;
import com.economy.plugin.gui.GuiManager;
import com.economy.plugin.link.LinkManager;
import com.economy.plugin.logging.TransactionLogger;
import com.economy.plugin.logging.TransactionType;
import com.economy.plugin.market.MarketManager;
import com.economy.plugin.npc.NpcMerchantManager;
import com.economy.plugin.papi.EconomyExpansion;
import com.economy.plugin.seasonal.SeasonalEventManager;
import com.economy.plugin.shop.DiscoveryTracker;
import com.economy.plugin.shop.PlayerIdentityResolver;
import com.economy.plugin.shop.ShopCatalog;
import com.economy.plugin.shop.ShopListener;
import com.economy.plugin.shop.ShopManager;
import com.economy.plugin.vault.VaultEconomy;
import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyPlugin extends JavaPlugin {

    private EconomyConfig cfg;
    private EconomyManager economy;
    private DataStore dataStore;
    private TransactionLogger transactions;
    private MarketManager market;
    private AuctionHouse auctionHouse;
    private BankManager bankManager;
    private NpcMerchantManager npcMerchants;
    private ShopManager shopManager;
    private ShopCatalog shopCatalog;
    private DiscoveryTracker discoveryTracker;
    private PlayerIdentityResolver identityResolver;
    private LinkManager linkManager;
    private GovernmentManager government;
    private SeasonalEventManager seasonal;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = new EconomyConfig(getConfig());

        File dataFolder = new File(getDataFolder(), cfg.dataDir());
        dataStore = createDataStore(dataFolder);

        transactions = new TransactionLogger(dataFolder, cfg.loggingEnabled(), cfg.loggingFile());
        economy = new EconomyManager(cfg.currencySymbol(), cfg.decimals(), cfg.startingBalance(), cfg.maxBalance());
        economy.setLogger(transactions);

        File marketItems = new File(getDataFolder(), "market/items.yml");
        if (!marketItems.exists()) {
            marketItems.getParentFile().mkdirs();
            saveResource("market/items.yml", false);
        }
        market = new MarketManager(marketItems, cfg.marketVolatility());
        identityResolver = new PlayerIdentityResolver();
        linkManager = new LinkManager(new File(getDataFolder(), "links.yml"), (primaryKey, secondaryKey, primaryOwner, secondaryOwner) -> {
            if (discoveryTracker != null) {
                discoveryTracker.mergeIdentities(primaryKey, secondaryKey);
            }
            if (economy.getExistingAccount(secondaryOwner).isPresent()) {
                Account primary = economy.getAccount(primaryOwner);
                primary.mergeFrom(economy.getAccount(secondaryOwner));
                economy.removeAccount(secondaryOwner);
                dataStore.deleteAccount(secondaryOwner);
                dataStore.saveAccount(primary);
                transactions.log(TransactionType.MERGE, secondaryOwner, primaryOwner, primary.getBalance(), "identity merge");
            }
        });
        auctionHouse = new AuctionHouse(cfg.aiBidderChance(), cfg.aiBudgetMultiplier(), cfg.minIncrement(),
                cfg.auctionSalesTax(), cfg.auctionListingFee(), linkManager::canonicalOwner);
        bankManager = new BankManager(cfg.interestRate(), cfg.interestCycle(), cfg.maxBankBalance(),
                cfg.loansEnabled(), cfg.loanInterest(), cfg.maxLoan(), cfg.repaymentPeriod(),
                cfg.minCreditScore(), cfg.defaultPenalty(), cfg.creditRepair(), linkManager::canonicalOwner);
        npcMerchants = new NpcMerchantManager(cfg.npcMargin(), cfg.npcSensitivity());
        shopManager = new ShopManager(cfg.maxShops(), cfg.maxEmployees(), cfg.defaultWage(), linkManager::canonicalOwner);
        if (cfg.shopsEnabled()) {
            shopCatalog = new ShopCatalog(cfg);
            discoveryTracker = new DiscoveryTracker(identityResolver, new File(getDataFolder(), "discoveries.yml"));
            discoveryTracker.setCanonicalKeyResolver(linkManager::canonicalKey);
            ShopListener.setTracker(discoveryTracker);
            registerEvents(new ShopListener());
        }
        government = new GovernmentManager(cfg.taxPercent(), cfg.grantsEnabled(), cfg.grantCooldownDays());
        seasonal = new SeasonalEventManager(cfg.crisisChance(), cfg.boomChance(), cfg.rareLootChance());

        loadPersistentAccounts();

        getCommand("economy").setExecutor(new EconomyCommand(this));
        getCommand("market").setExecutor(new MarketCommand(this));
        getCommand("auction").setExecutor(new AuctionCommand(this));
        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("events").setExecutor(new EventsCommand(this));
        if (cfg.shopsEnabled()) {
            getCommand("shop").setExecutor(new ShopCommand(this));
        }

        registerEvents(new GuiListener());
        hooks();

        startSchedulers();
        getLogger().info("True Economy " + getDescription().getVersion() + " enabled. Storage: " + cfg.dbType());
    }

    private void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    private DataStore createDataStore(File dataFolder) {
        switch (cfg.dbType()) {
            case "sqlite": {
                SqlDataStore store = SqlDataStore.sqlite(new File(dataFolder, cfg.sqliteFile()));
                store.init();
                return store;
            }
            case "mysql": {
                SqlDataStore store = SqlDataStore.mysql(cfg.mysqlHost(), cfg.mysqlPort(), cfg.mysqlDatabase(),
                        cfg.mysqlUsername(), cfg.mysqlPassword());
                store.init();
                return store;
            }
            default:
                return new YamlDataStore(dataFolder);
        }
    }

    private void hooks() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                new VaultEconomy(this, economy).register();
                getLogger().info("Vault hooked - economy available to other plugins.");
            } catch (Throwable t) {
                getLogger().warning("Vault hook failed: " + t.getMessage());
            }
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new EconomyExpansion(this).register();
                getLogger().info("PlaceholderAPI hooked - %trueeconomy_*% placeholders available.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI hook failed: " + t.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        saveAllAccounts();
        if (discoveryTracker != null) {
            discoveryTracker.save();
        }
        if (linkManager != null) {
            linkManager.persist();
        }
        if (dataStore != null) {
            dataStore.shutdown();
        }
        if (transactions != null) {
            transactions.close();
        }
        GuiManager.removeAll();
        getLogger().info("True Economy disabled.");
    }

    private void loadPersistentAccounts() {
        for (Map.Entry<String, Account> entry : dataStore.loadAllAccounts(cfg.startingBalance()).entrySet()) {
            economy.getAccount(entry.getKey()).restoreForLoad(
                    entry.getValue().getBalance(), entry.getValue().getBankBalance(), entry.getValue().getCreditScore());
        }
    }

    public void saveAccount(String owner) {
        Account acc = economy.getExistingAccount(owner).orElse(null);
        if (acc != null) {
            dataStore.saveAccount(acc);
        }
    }

    public void saveAllAccounts() {
        for (String owner : economy.getOwnerNames()) {
            if (owner.startsWith("__")) {
                continue;
            }
            dataStore.saveAccount(economy.getAccount(owner));
        }
    }

    private void startSchedulers() {
        if (cfg.marketEnabled()) {
            long tick = cfg.marketTick() * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> market.recalculateAll(), tick, tick);
        }
        if (cfg.auctionEnabled()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                for (Auction a : auctionHouse.processExpirations()) {
                    settleAuction(a);
                    getLogger().info("Auction resolved: " + a.getId());
                }
            }, 100L, 100L);
        }
        if (cfg.bankingEnabled()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                long touched = bankManager.tickInterest();
                if (touched > 0L) {
                    for (String owner : economy.getOwnerNames()) {
                        if (owner.startsWith("__")) {
                            continue;
                        }
                        Account acc = economy.getAccount(owner);
                        acc.applyBankInterest(cfg.interestRate());
                        dataStore.saveAccount(acc);
                        transactions.log(TransactionType.INTEREST, "bank", owner, acc.getBankBalance() * cfg.interestRate(), "interest");
                    }
                }
                for (Loan loan : bankManager.findDefaulted()) {
                    Account borrower = economy.getAccount(loan.getBorrower());
                    borrower.setCreditScore(borrower.getCreditScore() - bankManager.creditPenaltyForDefault());
                    loan.defaultLoan();
                    dataStore.saveAccount(borrower);
                    transactions.log(TransactionType.LOAN_DEFAULT, loan.getBorrower(), "bank", loan.getRemaining(), "defaulted");
                }
            }, 100L, 100L);
        }
        if (cfg.seasonalEnabled()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (seasonal.isEventReady()) {
                    SeasonalEventManager.MarketEvent event = seasonal.rollEvent(market);
                    if (event != null) {
                        getLogger().info("Seasonal market event: " + event.type + " on " + event.commodityId + " x" + event.multiplier);
                    }
                    seasonal.scheduleNextEvent(cfg.seasonalMin() * 1000L, cfg.seasonalMax() * 1000L);
                }
            }, 100L, 100L);
        }
    }

    private void settleAuction(Auction auction) {
        if (auction.getStatus() != Auction.Status.SOLD || auction.getHighestBidder() == null) {
            return;
        }
        String escrow = "__auction_" + auction.getId();
        double gross = auction.getCurrentBid();
        if (gross <= 0.0) {
            return;
        }
        double tax = gross * cfg.auctionSalesTax();
        String seller = linkedOwner(auction.getSeller());
        economy.transfer(escrow, seller, gross - tax);
        if (tax > 0.0) {
            economy.transfer(escrow, "__auction_fees", tax);
        }
        transactions.log(TransactionType.AUCTION_SALE, "auction:" + auction.getId(), seller, gross - tax, "settled");
        transactions.log(TransactionType.AUCTION_FEE, "auction:" + auction.getId(), "__auction_fees", tax, "sales tax");
        saveAccount(seller);
    }

    public void giveItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        player.updateInventory();
    }

    public TransactionLogger getTransactions() {
        return transactions;
    }

    public long getConfigDefaultAuctionDuration() {
        return cfg.auctionDefaultDuration();
    }

    public long getConfigMaxAuctionDuration() {
        return cfg.auctionMaxDuration();
    }

    public double getConfigAuctionListingFee() {
        return cfg.auctionListingFee();
    }

    public double getConfigAuctionSalesTax() {
        return cfg.auctionSalesTax();
    }

    public double getShopStartingCapital() {
        return cfg.shopStartingCapital();
    }

    public EconomyManager getEconomy() {
        return economy;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public MarketManager getMarket() {
        return market;
    }

    public AuctionHouse getAuctionHouse() {
        return auctionHouse;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public NpcMerchantManager getNpcMerchants() {
        return npcMerchants;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public ShopCatalog getShopCatalog() {
        return shopCatalog;
    }

    public DiscoveryTracker getDiscoveryTracker() {
        return discoveryTracker;
    }

    public PlayerIdentityResolver getIdentityResolver() {
        return identityResolver;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public String linkedOwner(String name) {
        return linkManager == null ? name : linkManager.canonicalOwner(name);
    }

    public String getShopMenuTitle() {
        return cfg.shopMenuTitle();
    }

    public GovernmentManager getGovernment() {
        return government;
    }

    public SeasonalEventManager getSeasonal() {
        return seasonal;
    }
}