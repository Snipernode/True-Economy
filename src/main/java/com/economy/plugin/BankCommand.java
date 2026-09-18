package com.economy.plugin;

import com.economy.plugin.banking.Loan;
import com.economy.plugin.core.Account;
import com.economy.plugin.logging.TransactionType;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankCommand implements CommandExecutor {
    private final EconomyPlugin plugin;

    public BankCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getBankManager() == null) {
            sender.sendMessage(ChatColor.RED + "Banking is disabled.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank <deposit|withdraw|interest|loan|repay|credit>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "deposit":
                return deposit(sender, args);
            case "withdraw":
                return withdraw(sender, args);
            case "interest":
                return interest(sender);
            case "loan":
                return loan(sender, args);
            case "repay":
                return repay(sender, args);
            case "credit":
                return credit(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /bank <deposit|withdraw|interest|loan|repay|credit>");
                return true;
        }
    }

    private boolean accountRequired(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have bank accounts.");
            return false;
        }
        return true;
    }

    private double parseAmount(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private boolean deposit(CommandSender sender, String[] args) {
        if (!accountRequired(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank deposit <amount>");
            return true;
        }
        Player player = (Player) sender;
        double amount = parseAmount(args[1]);
        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);
        if (!acc.depositBank(amount)) {
            player.sendMessage(ChatColor.RED + "Insufficient wallet funds.");
            return true;
        }
        plugin.getTransactions().log(TransactionType.BANK_DEPOSIT, owner, "bank", amount, null);
        plugin.saveAccount(owner);
        player.sendMessage(ChatColor.GREEN + "Deposited " + plugin.getEconomy().format(amount) + " to your bank. Wallet: "
                + plugin.getEconomy().format(acc.getBalance()) + ", Bank: " + plugin.getEconomy().format(acc.getBankBalance()) + ".");
        return true;
    }

    private boolean withdraw(CommandSender sender, String[] args) {
        if (!accountRequired(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank withdraw <amount>");
            return true;
        }
        Player player = (Player) sender;
        double amount = parseAmount(args[1]);
        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);
        if (!acc.withdrawBank(amount)) {
            player.sendMessage(ChatColor.RED + "Insufficient bank balance.");
            return true;
        }
        plugin.getTransactions().log(TransactionType.BANK_WITHDRAW, "bank", owner, amount, null);
        plugin.saveAccount(owner);
        player.sendMessage(ChatColor.GREEN + "Withdrew " + plugin.getEconomy().format(amount) + " from your bank. Wallet: "
                + plugin.getEconomy().format(acc.getBalance()) + ", Bank: " + plugin.getEconomy().format(acc.getBankBalance()) + ".");
        return true;
    }

    private boolean interest(CommandSender sender) {
        double ratePercent = plugin.getConfig().getDouble("banking.interest-rate-per-cycle", 0.001) * 100.0;
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Banking:");
        sender.sendMessage(ChatColor.GRAY + "Interest rate: " + ChatColor.WHITE + String.format("%.1f%% per cycle", ratePercent));
        sender.sendMessage(ChatColor.GRAY + "Max account balance: " + ChatColor.WHITE
                + plugin.getEconomy().format(plugin.getBankManager().getMaxAccountBalance()));
        return true;
    }

    private boolean loan(CommandSender sender, String[] args) {
        if (!accountRequired(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank loan <amount>");
            return true;
        }
        Player player = (Player) sender;
        double amount = parseAmount(args[1]);
        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);
        if (!plugin.getBankManager().isLoanAllowed(acc.getCreditScore())) {
            player.sendMessage(ChatColor.RED + "Your credit score is too low for a loan (need "
                    + plugin.getBankManager().getDefaultMinCreditScore() + ").");
            return true;
        }
        if (amount > plugin.getBankManager().getMaxLoanAmount()) {
            player.sendMessage(ChatColor.RED + "Loan exceeds the maximum of "
                    + plugin.getEconomy().format(plugin.getBankManager().getMaxLoanAmount()) + ".");
            return true;
        }
        try {
            Loan loan = plugin.getBankManager().createLoan(owner, amount, acc.getCreditScore());
            acc.modifyBalance(amount);
            plugin.getTransactions().log(TransactionType.LOAN_ISSUE, "bank", owner, amount, null);
            plugin.saveAccount(owner);
            player.sendMessage(ChatColor.GREEN + "Loan approved: " + plugin.getEconomy().format(amount)
                    + " credited. Repay " + plugin.getEconomy().format(loan.getRemaining())
                    + " by the due date (see /bank credit).");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private boolean repay(CommandSender sender, String[] args) {
        if (!accountRequired(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bank repay <amount>");
            return true;
        }
        Player player = (Player) sender;
        double amount = parseAmount(args[1]);
        if (amount <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);
        List<Loan> loans = plugin.getBankManager().getLoans(owner);
        Loan target = null;
        for (Loan loan : loans) {
            if (loan.getStatus() == Loan.Status.ACTIVE) {
                target = loan;
                break;
            }
        }
        if (target == null) {
            player.sendMessage(ChatColor.RED + "You have no active loans to repay.");
            return true;
        }
        double payment = Math.min(amount, target.getRemaining());
        if (!acc.canAfford(payment)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds.");
            return true;
        }
        acc.modifyBalance(-payment);
        target.repay(payment);
        int creditRepair = plugin.getBankManager().creditChangeForRepayment();
        if (target.isFullyRepaid()) {
            acc.setCreditScore(acc.getCreditScore() + creditRepair);
            player.sendMessage(ChatColor.GREEN + "Loan fully repaid! Credit score +" + creditRepair + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Repaid " + plugin.getEconomy().format(payment)
                    + ". Remaining: " + plugin.getEconomy().format(target.getRemaining()) + ".");
        }
        plugin.getTransactions().log(TransactionType.LOAN_REPAY, owner, "bank", payment, null);
        plugin.saveAccount(owner);
        return true;
    }

    private boolean credit(CommandSender sender) {
        if (!accountRequired(sender)) {
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        Account acc = plugin.getEconomy().getAccount(owner);
        List<Loan> loans = plugin.getBankManager().getLoans(owner);
        double outstanding = 0.0;
        for (Loan loan : loans) {
            if (loan.getStatus() != Loan.Status.ACTIVE) {
                continue;
            }
            outstanding += loan.getRemaining();
        }
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Credit Report:");
        player.sendMessage(ChatColor.GRAY + "Credit score: " + ChatColor.WHITE + acc.getCreditScore());
        player.sendMessage(ChatColor.GRAY + "Active loans: " + ChatColor.WHITE + loans.size()
                + ChatColor.GRAY + "  Outstanding: " + ChatColor.WHITE + plugin.getEconomy().format(outstanding));
        return true;
    }
}