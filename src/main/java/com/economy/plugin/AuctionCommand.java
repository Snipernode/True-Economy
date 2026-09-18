package com.economy.plugin;

import com.economy.plugin.auction.Auction;
import com.economy.plugin.gui.AuctionGui;
import com.economy.plugin.logging.TransactionType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class AuctionCommand implements CommandExecutor {
    private final EconomyPlugin plugin;

    public AuctionCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    private static String escrow(Auction auction) {
        return "__auction_" + auction.getId();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getAuctionHouse() == null) {
            sender.sendMessage(ChatColor.RED + "Auction house is disabled.");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player) {
                new AuctionGui(plugin, (Player) sender).open();
                return true;
            }
            return list(sender);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                return create(sender, args);
            case "bid":
                return bid(sender, args);
            case "buy":
                return buy(sender, args);
            case "list":
                return list(sender);
            case "mylist":
                return myList(sender);
            case "cancel":
                return cancel(sender, args);
            case "claim":
                return claim(sender, args);
            case "gui":
            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the auction GUI.");
                    return true;
                }
                new AuctionGui(plugin, (Player) sender).open();
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /auction <create|bid|buy|list|mylist|cancel|claim|gui>");
                return true;
        }
    }

    private boolean create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /auction create <itemMaterial> [amount] [buyNowPrice] [seconds]");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Material material = Material.matchMaterial(args[1]);
        if (material == null || material.isAir()) {
            player.sendMessage(ChatColor.RED + "Unknown item material '" + args[1] + "'.");
            return true;
        }
        if (plugin.getShopCatalog() != null && !plugin.getShopCatalog().isObtainable(material.name())) {
            player.sendMessage(ChatColor.RED + "That item is unobtainable and cannot be auctioned.");
            return true;
        }
        int amount = args.length > 2 ? parseInt(args[2], 1) : 1;
        if (amount < 1) {
            player.sendMessage(ChatColor.RED + "Amount must be at least 1.");
            return true;
        }
        double buyNowPrice = args.length > 3 ? parsePrice(args[3]) : 0.0;
        long seconds = args.length > 4 ? parseTime(args[4]) : plugin.getConfigDefaultAuctionDuration();
        if (buyNowPrice < 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid buy-now price.");
            return true;
        }
        if (seconds < 10L || seconds > plugin.getConfigMaxAuctionDuration()) {
            player.sendMessage(ChatColor.RED + "Duration must be between 10s and " + plugin.getConfigMaxAuctionDuration() + "s.");
            return true;
        }
        double fee = plugin.getConfigAuctionListingFee();
        if (fee > 0.0 && !plugin.getEconomy().transfer(owner, "__auction_fees", fee)) {
            player.sendMessage(ChatColor.RED + "You cannot afford the " + plugin.getEconomy().format(fee) + " listing fee.");
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        InventoryOps op = removeItems(inventory, material, amount);
        if (!op.ok) {
            if (fee > 0.0) {
                plugin.getEconomy().transfer("__auction_fees", owner, fee);
            }
            player.sendMessage(ChatColor.RED + "You don't have "
                    + (op.have > 0 ? Math.min(op.have, amount) + " of " : "any ") + material.name()
                    + " in your inventory.");
            return true;
        }
        Auction auction = new Auction(UUID.randomUUID(), owner, new ItemStack(material, amount), seconds,
                buyNowPrice > 0.0, buyNowPrice, 0.0);
        plugin.getAuctionHouse().add(auction);
        plugin.getTransactions().log(TransactionType.AUCTION_LISTING_FEE, owner, "__auction_fees", fee, "listing " + shortId(auction));
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Auction listed: " + ChatColor.WHITE + amount + " " + material.name()
                + ChatColor.GOLD + " (id " + shortId(auction) + ")"
                + (buyNowPrice > 0.0 ? ChatColor.GRAY + "  buy-now " + plugin.getEconomy().format(buyNowPrice) : ""));
        return true;
    }

    private boolean bid(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /auction bid <id> <amount>");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Auction auction = findActive(args[1]);
        if (auction == null) {
            player.sendMessage(ChatColor.RED + "No active auction with that id.");
            return true;
        }
        if (owner.equals(plugin.linkedOwner(auction.getSeller()))) {
            player.sendMessage(ChatColor.RED + "You cannot bid on your own auction.");
            return true;
        }
        double amount = parsePrice(args[2]);
        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid bid amount.");
            return true;
        }
        String oldBidder = auction.getHighestBidder();
        double oldBid = auction.getCurrentBid();
        if (plugin.getEconomy().getAccount(owner).canAfford(amount) && plugin.getAuctionHouse().placeBid(auction, owner, amount)) {
            plugin.getEconomy().transfer(owner, escrow(auction), amount);
            plugin.getTransactions().log(TransactionType.AUCTION_BID, owner, "auction:" + shortId(auction), amount, null);
            if (oldBidder != null && !plugin.linkedOwner(oldBidder).equals(owner)) {
                plugin.getEconomy().transfer(escrow(auction), plugin.linkedOwner(oldBidder), oldBid);
                plugin.getTransactions().log(TransactionType.AUCTION_REFUND, "auction:" + shortId(auction), oldBidder, oldBid, null);
            }
            player.sendMessage(ChatColor.GREEN + "Bid " + plugin.getEconomy().format(amount) + " on "
                    + ChatColor.WHITE + auction.getItem().getType().name() + ChatColor.GRAY
                    + " (id " + shortId(auction) + ").");
        } else {
            player.sendMessage(ChatColor.RED + "Bid too low or you cannot afford it.");
        }
        return true;
    }

    private boolean buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /auction buy <id>");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Auction auction = findActive(args[1]);
        if (auction == null || !auction.isBuyItNow()) {
            player.sendMessage(ChatColor.RED + "No buy-it-now auction with that id.");
            return true;
        }
        if (owner.equals(plugin.linkedOwner(auction.getSeller()))) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own auction.");
            return true;
        }
        double buyPrice = auction.getBuyPrice();
        if (!plugin.getEconomy().getAccount(owner).canAfford(buyPrice)) {
            player.sendMessage(ChatColor.RED + "You cannot afford " + plugin.getEconomy().format(buyPrice) + ".");
            return true;
        }
        double payout = plugin.getAuctionHouse().buyNow(auction, owner, plugin.getConfigAuctionSalesTax());
        if (payout < 0.0) {
            player.sendMessage(ChatColor.RED + "That auction is no longer available.");
            return true;
        }
        plugin.getEconomy().transfer(owner, escrow(auction), buyPrice);
        plugin.getEconomy().transfer(escrow(auction), plugin.linkedOwner(auction.getSeller()), payout);
        plugin.getEconomy().transfer(escrow(auction), "__auction_fees", buyPrice - payout);
        plugin.getTransactions().log(TransactionType.AUCTION_SALE, "auction:" + shortId(auction), owner, buyPrice, null);
        plugin.giveItem(player, auction.getItem().clone());
        auction.markClaimed();
        plugin.getAuctionHouse().remove(auction.getId());
        plugin.getAuctionHouse().resolvedKeep(auction);
        player.sendMessage(ChatColor.GREEN + "Bought " + ChatColor.WHITE + auction.getItem().getAmount() + " "
                + auction.getItem().getType().name() + ChatColor.GREEN + " for " + plugin.getEconomy().format(buyPrice) + ".");
        return true;
    }

    private boolean list(CommandSender sender) {
        List<Auction> active = plugin.getAuctionHouse().getActive();
        if (active.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No active auctions.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Active Auctions:");
        for (Auction a : active) {
            long left = Math.max(0L, (a.getEndTime() - System.currentTimeMillis()) / 1000L);
            sender.sendMessage(ChatColor.AQUA + "[" + shortId(a) + "] " + ChatColor.WHITE
                    + a.getItem().getAmount() + " " + a.getItem().getType().name() + ChatColor.GRAY
                    + "  bid " + plugin.getEconomy().format(a.getCurrentBid())
                    + (a.isBuyItNow() ? "  / buy-now " + plugin.getEconomy().format(a.getBuyPrice()) : "")
                    + "  ends in " + left + "s");
        }
        return true;
    }

    private boolean myList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have listings.");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        List<Auction> mine = plugin.getAuctionHouse().getBySeller(owner);
        if (mine.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You have no active listings.");
            return true;
        }
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Your Listings:");
        for (Auction a : mine) {
            long left = Math.max(0L, (a.getEndTime() - System.currentTimeMillis()) / 1000L);
            player.sendMessage(ChatColor.AQUA + "[" + shortId(a) + "] " + ChatColor.WHITE
                    + a.getItem().getAmount() + " " + a.getItem().getType().name() + ChatColor.GRAY
                    + "  current " + plugin.getEconomy().format(a.getCurrentBid()) + "  ends in " + left + "s");
        }
        return true;
    }

    private boolean cancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /auction cancel <id>");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Auction auction = findActive(args[1]);
        if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
            player.sendMessage(ChatColor.RED + "No active auction with that id.");
            return true;
        }
        if (!owner.equals(plugin.linkedOwner(auction.getSeller()))) {
            player.sendMessage(ChatColor.RED + "Only the seller can cancel this auction.");
            return true;
        }
        String bidder = auction.getHighestBidder();
        if (bidder != null) {
            plugin.getEconomy().transfer(escrow(auction), plugin.linkedOwner(bidder), auction.getCurrentBid());
            plugin.getTransactions().log(TransactionType.AUCTION_REFUND, "auction:" + shortId(auction), bidder, auction.getCurrentBid(), "cancel");
            player.sendMessage(ChatColor.GRAY + "Refunded the highest bidder.");
        }
        plugin.getAuctionHouse().remove(auction.getId());
        plugin.giveItem(player, auction.getItem().clone());
        auction.markClaimed();
        player.sendMessage(ChatColor.GREEN + "Auction cancelled, item returned.");
        return true;
    }

    private boolean claim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /auction claim <id>");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Auction auction = plugin.getAuctionHouse().getResolved(parseUuid(args[1]));
        if (auction == null) {
            auction = findResolved(args[1]);
        }
        if (auction == null) {
            player.sendMessage(ChatColor.RED + "No resolved auction with that id (did it expire?).");
            return true;
        }
        if (auction.getStatus() == Auction.Status.CLAIMED) {
            player.sendMessage(ChatColor.RED + "That auction was already claimed.");
            return true;
        }
        if (auction.getStatus() == Auction.Status.EXPIRED) {
            if (!owner.equals(plugin.linkedOwner(auction.getSeller()))) {
                player.sendMessage(ChatColor.RED + "Only the seller may reclaim an unsold auction.");
                return true;
            }
            plugin.giveItem(player, auction.getItem().clone());
            auction.markClaimed();
            player.sendMessage(ChatColor.GREEN + "Auction reclaimed, item returned.");
            return true;
        }
        if (auction.getHighestBidder() != null && !owner.equals(plugin.linkedOwner(auction.getHighestBidder()))) {
            player.sendMessage(ChatColor.RED + "Only the winning bidder may claim this item.");
            return true;
        }
        plugin.giveItem(player, auction.getItem().clone());
        auction.markClaimed();
        player.sendMessage(ChatColor.GREEN + "Item claimed: " + auction.getItem().getAmount() + " " + auction.getItem().getType().name());
        return true;
    }

    private static String shortId(Auction auction) {
        return auction.getId().toString().substring(0, 8);
    }

    private Auction findActive(String input) {
        if (input.length() > 10) {
            Auction direct = plugin.getAuctionHouse().get(parseUuid(input));
            if (direct != null) {
                return direct;
            }
        }
        for (Auction a : plugin.getAuctionHouse().getActive()) {
            if (shortId(a).equalsIgnoreCase(input)) {
                return a;
            }
        }
        return null;
    }

    private Auction findResolved(String input) {
        if (input.length() > 10) {
            Auction direct = plugin.getAuctionHouse().getResolved(parseUuid(input));
            if (direct != null) {
                return direct;
            }
        }
        for (Auction a : plugin.getAuctionHouse().getAllResolved()) {
            if (shortId(a).equalsIgnoreCase(input)) {
                return a;
            }
        }
        return null;
    }

    private static UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parsePrice(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private static long parseTime(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return 300L;
        }
    }

    private static InventoryOps removeItems(PlayerInventory inventory, Material material, int amount) {
        int have = 0;
        int size = Math.min(inventory.getSize(), 36);
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != material) {
                continue;
            }
            have += item.getAmount();
        }
        if (have < amount) {
            return new InventoryOps(false, have);
        }
        int remaining = amount;
        for (int slot = 0; slot < size && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != material) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            if (take >= item.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
                inventory.setItem(slot, item);
            }
            remaining -= take;
        }
        return new InventoryOps(true, have);
    }

    private static final class InventoryOps {
        final boolean ok;
        final int have;

        InventoryOps(boolean ok, int have) {
            this.ok = ok;
            this.have = have;
        }
    }
}