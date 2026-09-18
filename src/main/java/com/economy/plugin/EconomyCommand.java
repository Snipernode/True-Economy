package com.economy.plugin;

import com.economy.plugin.core.Account;
import com.economy.plugin.link.LinkManager;
import com.economy.plugin.logging.TransactionType;
import com.economy.plugin.shop.PlayerIdentity;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand implements CommandExecutor {
    private final EconomyPlugin plugin;

    public EconomyCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Economy help: /economy <balance|pay|top|set|give|take|reset|market|auction|bank|loan|shop|events|link|tax>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "balance":
            case "bal":
                return balance(sender, args);
            case "pay":
                return pay(sender, args);
            case "top":
                return top(sender, args);
            case "set":
                return set(sender, args);
            case "give":
                return give(sender, args);
            case "take":
                return take(sender, args);
            case "reset":
                return reset(sender, args);
            case "market":
                return new MarketCommand(plugin).onCommand(sender, command, label, shift(args));
            case "auction":
            case "ah":
                return new AuctionCommand(plugin).onCommand(sender, command, label, shift(args));
            case "bank":
            case "loan":
                return new BankCommand(plugin).onCommand(sender, command, label, shift(args));
            case "shop":
                return new ShopCommand(plugin).onCommand(sender, command, label, shift(args));
            case "events":
                return new EventsCommand(plugin).onCommand(sender, command, label, shift(args));
            case "tax":
                sender.sendMessage(ChatColor.GOLD + "Current transaction tax: " + ChatColor.WHITE
                        + (int) (plugin.getGovernment().getTransactionTaxPercent() * 100.0) + "%");
                return true;
            case "link":
                return link(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /economy balance|pay|top|market|auction|bank|shop|events|link|tax");
                return true;
        }
    }

    private static String[] shift(String[] args) {
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }

    // ---------- identity link ----------

    private boolean link(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can link identities.");
            return true;
        }
        if (plugin.getLinkManager() == null) {
            sender.sendMessage(ChatColor.RED + "Identity linking is unavailable.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "/economy link start - create a confirmation code");
            sender.sendMessage(ChatColor.YELLOW + "/economy link <code> - complete a link from your other account");
            return true;
        }
        Player player = (Player) sender;
        PlayerIdentity identity = plugin.getIdentityResolver().resolve(player);
        LinkManager manager = plugin.getLinkManager();
        if (args[1].equalsIgnoreCase("start")) {
            String code = manager.startLink(identity.getKey(), plugin.linkedOwner(player.getName()));
            player.sendMessage(ChatColor.GREEN + "Link code: " + ChatColor.WHITE + code);
            player.sendMessage(ChatColor.GRAY + "Sign in on your other account and run " + ChatColor.YELLOW + "/economy link " + code);
            player.sendMessage(ChatColor.GRAY + "Code expires in 5 minutes. Unlocks, currency and bank balances will be merged.");
            return true;
        }
        LinkManager.Result result = manager.completeLink(args[1], identity.getKey(), plugin.linkedOwner(player.getName()));
        switch (result) {
            case LINKED:
                player.sendMessage(ChatColor.GREEN + "Identities linked! Unlocks, currency and bank balances merged.");
                break;
            case NO_SUCH_CODE:
                player.sendMessage(ChatColor.RED + "No pending link with that code.");
                break;
            case EXPIRED:
                player.sendMessage(ChatColor.RED + "That link code has expired. Run /economy link start again.");
                break;
            case ALREADY_LINKED:
                player.sendMessage(ChatColor.RED + "One of those identities is already linked. Ask an admin if this is wrong.");
                break;
            case SELF_LINK:
                player.sendMessage(ChatColor.RED + "You cannot link an identity to itself.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Link failed.");
        }
        return true;
    }

    // ---------- balance ----------

    private boolean balance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Console cannot view a balance here.");
            return true;
        }
        Player p = (Player) sender;
        Account acc = plugin.getEconomy().getAccount(plugin.linkedOwner(p.getName()));
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Balance: " + ChatColor.WHITE
                + plugin.getEconomy().format(acc.getBalance())
                + ChatColor.GRAY + " (Bank: " + plugin.getEconomy().format(acc.getBankBalance()) + ")");
        p.sendMessage(ChatColor.GRAY + "Credit score: " + acc.getCreditScore());
        return true;
    }

    // ---------- pay ----------

    private boolean pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can pay.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy pay <player> <amount>");
            return true;
        }
        Player from = (Player) sender;
        String to = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            from.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        if (amount <= 0.0) {
            from.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }
        if (plugin.getEconomy().transfer(plugin.linkedOwner(from.getName()), plugin.linkedOwner(to), amount)) {
            plugin.getTransactions().log(TransactionType.TRANSFER, from.getName(), to, amount, "pay");
            double tax = plugin.getGovernment().levyTransactionTax("default", amount);
            plugin.getTransactions().log(TransactionType.TAX, from.getName(), "government", tax, "transaction tax");
            plugin.saveAccount(plugin.linkedOwner(to));
            from.sendMessage(ChatColor.GREEN + "Paid " + plugin.getEconomy().format(amount) + " to " + to
                    + ChatColor.GRAY + " (tax: " + plugin.getEconomy().format(tax) + ")");
        } else {
            from.sendMessage(ChatColor.RED + "Insufficient funds.");
        }
        return true;
    }

    // ---------- leaderboard ----------

    private boolean top(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Math.min(100, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        List<Account> top = plugin.getEconomy().getTopAccounts(limit);
        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No accounts yet.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Richest players:");
        int i = 1;
        for (Account acc : top) {
            String medal = i == 1 ? ChatColor.GOLD + "#1 " : ChatColor.GRAY + "#" + i + " ";
            sender.sendMessage(medal + ChatColor.WHITE + acc.getOwner() + ChatColor.GRAY + "  "
                    + plugin.getEconomy().format(acc.getBalance()));
            i++;
        }
        return true;
    }

    // ---------- admin: set / give / take / reset ----------

    private boolean set(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy set <player> <amount>");
            return true;
        }
        String owner = plugin.linkedOwner(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        if (amount < 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount cannot be negative. Use /economy take.");
            return true;
        }
        plugin.getEconomy().setBalance(owner, amount);
        plugin.saveAccount(owner);
        plugin.getTransactions().log(TransactionType.ADMIN_SET, sender.getName(), args[1], amount, null);
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            target.sendMessage(ChatColor.GREEN + "Your balance was set to " + plugin.getEconomy().format(amount) + ".");
        }
        sender.sendMessage(ChatColor.GREEN + "Set " + args[1] + "'s balance to " + plugin.getEconomy().format(amount) + ".");
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy give <player> <amount>");
            return true;
        }
        String owner = plugin.linkedOwner(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        if (amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }
        if (plugin.getEconomy().deposit(owner, amount)) {
            plugin.saveAccount(owner);
            plugin.getTransactions().log(TransactionType.ADMIN_GIVE, sender.getName(), args[1], amount, null);
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You were given " + plugin.getEconomy().format(amount) + ".");
            }
            sender.sendMessage(ChatColor.GREEN + "Gave " + args[1] + " " + plugin.getEconomy().format(amount) + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Account cannot hold that much.");
        }
        return true;
    }

    private boolean take(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy take <player> <amount>");
            return true;
        }
        String owner = plugin.linkedOwner(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        if (amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }
        if (plugin.getEconomy().withdraw(owner, amount)) {
            plugin.saveAccount(owner);
            plugin.getTransactions().log(TransactionType.ADMIN_TAKE, sender.getName(), args[1], amount, null);
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                target.sendMessage(ChatColor.RED + plugin.getEconomy().format(amount) + " was taken from your balance.");
            }
            sender.sendMessage(ChatColor.GREEN + "Took " + plugin.getEconomy().format(amount) + " from " + args[1] + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Player does not have that much.");
        }
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy reset <player>");
            return true;
        }
        String owner = plugin.linkedOwner(args[1]);
        Account acc = plugin.getEconomy().getAccount(owner);
        acc.restoreForLoad(plugin.getConfig().getDouble("currency.starting-balance", 100.0), 0.0, 500);
        plugin.saveAccount(owner);
        plugin.getTransactions().log(TransactionType.ADMIN_RESET, sender.getName(), args[1], acc.getBalance(), null);
        sender.sendMessage(ChatColor.GREEN + "Reset " + args[1] + "'s account.");
        return true;
    }
}