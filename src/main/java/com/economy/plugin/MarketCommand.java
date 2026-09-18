package com.economy.plugin;

import com.economy.plugin.core.Account;
import com.economy.plugin.gui.MarketGui;
import com.economy.plugin.logging.TransactionType;
import com.economy.plugin.market.Commodity;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarketCommand implements CommandExecutor {
    private static final String MARKET_ACCOUNT = "__market__";
    private final EconomyPlugin plugin;

    public MarketCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                MarketGui gui = new MarketGui(plugin, (Player) sender);
                gui.open();
            }
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list":
                return list(sender);
            case "view":
                return view(sender, args);
            case "trade":
                return trade(sender, args);
            case "gui":
            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the market GUI.");
                    return true;
                }
                new MarketGui(plugin, (Player) sender).open();
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /market <list|view <id>|trade <id> <amount> [sell]|gui>");
                return true;
        }
    }

    private boolean marketCheck(CommandSender sender) {
        if (plugin.getMarket() == null || plugin.getMarket().getAll().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "The market is empty or disabled.");
            return false;
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!marketCheck(sender)) {
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Dynamic Market:");
        for (Commodity c : plugin.getMarket().getAll()) {
            sender.sendMessage(ChatColor.AQUA + c.getDisplayName() + ChatColor.WHITE + " -> "
                    + plugin.getEconomy().format(c.getCurrentPrice()) + ChatColor.GRAY + "  [id: " + c.getId()
                    + "]  supply " + Math.round(c.getSupply()) + ", demand " + Math.round(c.getDemand()));
        }
        return true;
    }

    private boolean view(CommandSender sender, String[] args) {
        if (!marketCheck(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /market view <id>");
            return true;
        }
        Commodity c = plugin.getMarket().getCommodity(args[1].toLowerCase(Locale.ROOT));
        if (c == null) {
            sender.sendMessage(ChatColor.RED + "Unknown commodity '" + args[1] + "'. Use /market list.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + c.getDisplayName());
        sender.sendMessage(ChatColor.GRAY + "Current price: " + plugin.getEconomy().format(c.getCurrentPrice()));
        sender.sendMessage(ChatColor.GRAY + "Base price:    " + plugin.getEconomy().format(c.getBasePrice()));
        sender.sendMessage(ChatColor.GRAY + "Supply: " + Math.round(c.getSupply()) + "  Demand: " + Math.round(c.getDemand())
                + "  Elasticity: " + c.getElasticity());
        return true;
    }

    private boolean trade(CommandSender sender, String[] args) {
        if (!marketCheck(sender)) {
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can trade on the market.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /market trade <id> <amount> [sell]");
            return true;
        }
        Commodity c = plugin.getMarket().getCommodity(args[1].toLowerCase(Locale.ROOT));
        if (c == null) {
            sender.sendMessage(ChatColor.RED + "Unknown commodity '" + args[1] + "'. Use /market list.");
            return true;
        }
        double amount;
        try {
            amount = Math.abs(Double.parseDouble(args[2]));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        if (amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }
        boolean selling = args.length > 3 && args[3].equalsIgnoreCase("sell");
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        double price = c.getCurrentPrice() * amount;
        double tax = 0.0;
        if (selling) {
            Account marketAcc = plugin.getEconomy().getExistingAccount(MARKET_ACCOUNT).orElse(null);
            if (marketAcc == null || !marketAcc.canAfford(price)) {
                player.sendMessage(ChatColor.RED + "The market cannot afford to buy that right now.");
                return true;
            }
            c.sell(amount, plugin.getMarket().getVolatility());
            plugin.getEconomy().transfer(MARKET_ACCOUNT, owner, price);
            plugin.getTransactions().log(TransactionType.MARKET_SELL, owner, MARKET_ACCOUNT, price, c.getId());
            player.sendMessage(ChatColor.GREEN + "Sold " + Math.round(amount) + " " + c.getDisplayName() + " for "
                    + plugin.getEconomy().format(price) + ".");
        } else {
            tax = plugin.getGovernment().levyTransactionTax("default", price);
            plugin.getTransactions().log(TransactionType.TAX, owner, "government", tax, "market tax");
            double total = price + tax;
            if (!plugin.getEconomy().transfer(owner, MARKET_ACCOUNT, total)) {
                player.sendMessage(ChatColor.RED + "You cannot afford " + plugin.getEconomy().format(total) + ".");
                return true;
            }
            c.buy(amount, plugin.getMarket().getVolatility());
            plugin.getTransactions().log(TransactionType.MARKET_BUY, MARKET_ACCOUNT, owner, price, c.getId());
            player.sendMessage(ChatColor.GREEN + "Bought " + Math.round(amount) + " " + c.getDisplayName() + " for "
                    + plugin.getEconomy().format(price)
                    + (tax > 0.0 ? ChatColor.GRAY + " (tax " + plugin.getEconomy().format(tax) + ")" : "") + ".");
        }
        return true;
    }
}