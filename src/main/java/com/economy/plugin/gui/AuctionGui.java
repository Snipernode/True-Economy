package com.economy.plugin.gui;

import com.economy.plugin.EconomyPlugin;
import com.economy.plugin.auction.Auction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class AuctionGui extends PaginatedGui {
    private static final int SLOT_BID = 50;
    private static final int SLOT_BUY = 52;

    private final EconomyPlugin plugin;
    private final List<Auction> auctions = new ArrayList<>();
    private UUID selectedId;

    public AuctionGui(EconomyPlugin plugin, Player player) {
        super(player, "&2&lAuction House");
        this.plugin = plugin;
        this.auctions.addAll(plugin.getAuctionHouse().getActive());
    }

    private Auction selected() {
        if (selectedId == null) {
            return null;
        }
        for (Auction a : auctions) {
            if (a.getId().equals(selectedId)) {
                return a;
            }
        }
        return null;
    }

    @Override
    protected List<ItemStack> getItems() {
        List<ItemStack> result = new ArrayList<>();
        for (Auction a : auctions) {
            ItemStack item = a.getItem().clone();
            if (item == null || item.getType() == Material.AIR) {
                item = new ItemStack(Material.STICK);
            }
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "" + a.getItem().getAmount() + " " + a.getItem().getType().name());
                List<String> lore = new ArrayList<>();
                if (a.getId().equals(selectedId)) {
                    lore.add(ChatColor.GREEN + ">> SELECTED <<");
                }
                lore.add(ChatColor.GRAY + "id: " + shortId(a));
                lore.add(ChatColor.GRAY + "Seller: " + a.getSeller());
                lore.add(ChatColor.GRAY + "Current bid: " + ChatColor.WHITE + plugin.getEconomy().format(a.getCurrentBid()));
                if (a.isBuyItNow()) {
                    lore.add(ChatColor.GRAY + "Buy-now: " + ChatColor.WHITE + plugin.getEconomy().format(a.getBuyPrice()));
                }
                long left = Math.max(0L, (a.getEndTime() - System.currentTimeMillis()) / 1000L);
                lore.add(ChatColor.GRAY + "Ends in: " + left + "s");
                lore.add(ChatColor.GREEN + "Click to select, then use the buttons below.");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            result.add(item);
        }
        return result;
    }

    private String shortId(Auction a) {
        return a.getId().toString().substring(0, 8);
    }

    @Override
    protected void renderBottomRow(int maxPage, int itemCount) {
        super.renderBottomRow(maxPage, itemCount);
        Auction sel = selected();
        setItem(SLOT_BID, navItem(Material.GOLD_INGOT, sel != null, "&e&lBid",
                sel == null ? "Select an auction first" : "Bid " + plugin.getEconomy().format(nextBid(sel))));
        setItem(SLOT_BUY, navItem(Material.EMERALD, sel != null && sel.isBuyItNow(), "&a&lBuy Now",
                sel == null ? "Select an auction first" : (sel.isBuyItNow() ? "Buy for " + plugin.getEconomy().format(sel.getBuyPrice()) : "This auction has no buy-now price")));
    }

    private double nextBid(Auction a) {
        double minIncrement = plugin.getAuctionHouse().getMinIncrementPercent();
        return plugin.getEconomy().round(a.getCurrentBid() * (1.0 + minIncrement) + 0.01);
    }

    @Override
    protected void onItemClick(int slot, ClickType click) {
        int index = getPage() * PAGE_SIZE + slot;
        if (index < 0 || index >= auctions.size()) {
            return;
        }
        selectedId = auctions.get(index).getId();
        refresh();
    }

    @Override
    protected void onActionSlot(int slot, ClickType click) {
        if (slot == SLOT_BID) {
            bid();
        } else if (slot == SLOT_BUY) {
            buy();
        }
    }

    private void bid() {
        Auction auction = selected();
        Player player = getPlayer();
        if (auction == null) {
            player.sendMessage(ChatColor.RED + "Select an auction first.");
            return;
        }
        String owner = plugin.linkedOwner(player.getName());
        if (owner.equals(plugin.linkedOwner(auction.getSeller()))) {
            player.sendMessage(ChatColor.RED + "You cannot bid on your own auction.");
            return;
        }
        double amount = nextBid(auction);
        String oldBidder = auction.getHighestBidder();
        double oldBid = auction.getCurrentBid();
        if (plugin.getEconomy().getAccount(owner).canAfford(amount) && plugin.getAuctionHouse().placeBid(auction, owner, amount)) {
            plugin.getEconomy().transfer(owner, escrow(auction), amount);
            plugin.getTransactions().log(com.economy.plugin.logging.TransactionType.AUCTION_BID, owner, "auction:" + shortId(auction), amount, null);
            if (oldBidder != null && !plugin.linkedOwner(oldBidder).equals(owner)) {
                plugin.getEconomy().transfer(escrow(auction), plugin.linkedOwner(oldBidder), oldBid);
                plugin.getTransactions().log(com.economy.plugin.logging.TransactionType.AUCTION_REFUND, "auction:" + shortId(auction), oldBidder, oldBid, null);
            }
            player.sendMessage(ChatColor.GREEN + "Bid " + plugin.getEconomy().format(amount) + " on " + auction.getItem().getType().name() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Bid too low or you cannot afford it.");
        }
        refresh();
    }

    private void buy() {
        Auction auction = selected();
        Player player = getPlayer();
        if (auction == null) {
            player.sendMessage(ChatColor.RED + "Select an auction first.");
            return;
        }
        if (!auction.isBuyItNow()) {
            player.sendMessage(ChatColor.RED + "That auction has no buy-now price.");
            return;
        }
        String owner = plugin.linkedOwner(player.getName());
        if (owner.equals(plugin.linkedOwner(auction.getSeller()))) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own auction.");
            return;
        }
        double buyPrice = auction.getBuyPrice();
        if (!plugin.getEconomy().getAccount(owner).canAfford(buyPrice)) {
            player.sendMessage(ChatColor.RED + "You cannot afford " + plugin.getEconomy().format(buyPrice) + ".");
            return;
        }
        double payout = plugin.getAuctionHouse().buyNow(auction, owner, plugin.getConfigAuctionSalesTax());
        if (payout < 0.0) {
            player.sendMessage(ChatColor.RED + "That auction is no longer available.");
            return;
        }
        plugin.getEconomy().transfer(owner, escrow(auction), buyPrice);
        plugin.getEconomy().transfer(escrow(auction), plugin.linkedOwner(auction.getSeller()), payout);
        plugin.getEconomy().transfer(escrow(auction), "__auction_fees", buyPrice - payout);
        plugin.getTransactions().log(com.economy.plugin.logging.TransactionType.AUCTION_SALE, "auction:" + shortId(auction), owner, buyPrice, null);
        plugin.giveItem(player, auction.getItem().clone());
        auction.markClaimed();
        plugin.getAuctionHouse().remove(auction.getId());
        plugin.getAuctionHouse().resolvedKeep(auction);
        player.sendMessage(ChatColor.GREEN + "Bought " + auction.getItem().getAmount() + " " + auction.getItem().getType().name() + " for " + plugin.getEconomy().format(buyPrice) + ".");
        player.closeInventory();
    }

    private static String escrow(Auction auction) {
        return "__auction_" + auction.getId();
    }
}