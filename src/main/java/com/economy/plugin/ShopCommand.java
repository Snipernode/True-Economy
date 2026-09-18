package com.economy.plugin;

import com.economy.plugin.shop.PlayerShop;
import com.economy.plugin.shop.ShopCatalog;
import com.economy.plugin.shop.ShopGui;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private final EconomyPlugin plugin;

    public ShopCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getShopManager() == null) {
            sender.sendMessage(ChatColor.RED + "Shops are disabled on this server.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("open")) {
            return openMenu(sender);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                return create(sender, args);
            case "list":
                return list(sender);
            case "manage":
                return manage(sender, args);
            case "hire":
                return hire(sender, args);
            case "fire":
                return fire(sender, args);
            case "wage":
                return wage(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /shop <create|list|manage|hire|fire|wage> or /shop for the menu");
                return true;
        }
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the shop menu.");
            return true;
        }
        Player player = (Player) sender;
        ShopCatalog catalog = plugin.getShopCatalog();
        if (catalog == null) {
            player.sendMessage(ChatColor.RED + "The shop catalog is not configured.");
            return true;
        }
        ShopGui gui = new ShopGui(player, plugin.getShopMenuTitle(), catalog, plugin.getDiscoveryTracker());
        gui.open();
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop create <name>");
            return true;
        }
        Player player = (Player) sender;
        String owner = plugin.linkedOwner(player.getName());
        try {
            PlayerShop shop = plugin.getShopManager().createShop(owner, args[1], plugin.getShopStartingCapital());
            player.sendMessage(ChatColor.GREEN + "Shop '" + shop.getName() + "' created. Use /shop manage " + shop.getName() + " to view it.");
        } catch (IllegalStateException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players own shops.");
            return true;
        }
        Player player = (Player) sender;
        List<PlayerShop> shops = plugin.getShopManager().getShops(plugin.linkedOwner(player.getName()));
        if (shops.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You don't own any shops. Create one: /shop create <name>");
            return true;
        }
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Your shops:");
        for (PlayerShop shop : shops) {
            player.sendMessage(ChatColor.AQUA + "- " + shop.getName() + ChatColor.GRAY + "  employees "
                    + shop.getEmployees().size() + "/" + shop.getMaxEmployees() + ", revenue "
                    + plugin.getEconomy().format(shop.getRevenue()));
        }
        return true;
    }

    private boolean manage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop manage <name>");
            return true;
        }
        Player player = (Player) sender;
        PlayerShop shop = findShop(player, args[1], true);
        if (shop == null) {
            return true;
        }
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Shop: " + shop.getName()
                + ChatColor.GRAY + "  (owner " + shop.getOwner() + ")");
        player.sendMessage(ChatColor.GRAY + "Revenue: " + ChatColor.WHITE + plugin.getEconomy().format(shop.getRevenue())
                + ChatColor.GRAY + "  Wages owed: " + ChatColor.WHITE + plugin.getEconomy().format(shop.wagesOwed()));
        player.sendMessage(ChatColor.GRAY + "Net profit after wages: " + ChatColor.WHITE
                + plugin.getEconomy().format(shop.netProfitAfterWages()));
        player.sendMessage(ChatColor.GRAY + "Production line: " + ChatColor.WHITE
                + plugin.getEconomy().format(shop.getProductionLineOutput()));
        player.sendMessage(ChatColor.GRAY + "Employees: " + ChatColor.WHITE
                + (shop.getEmployees().isEmpty() ? "none" : String.join(", ", shop.getEmployees())));
        player.sendMessage(ChatColor.GRAY + "Wage rate: " + ChatColor.WHITE + plugin.getEconomy().format(shop.getWageRate()));
        return true;
    }

    private boolean hire(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop hire <name> <player>");
            return true;
        }
        Player player = (Player) sender;
        PlayerShop shop = findShop(player, args[1], true);
        if (shop == null) {
            return true;
        }
        if (!shop.hire(plugin.linkedOwner(args[2]))) {
            player.sendMessage(ChatColor.RED + "Hire failed (full, or already hired).");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Hired " + args[2] + " at " + shop.getName() + ".");
        return true;
    }

    private boolean fire(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop fire <name> <player>");
            return true;
        }
        Player player = (Player) sender;
        PlayerShop shop = findShop(player, args[1], true);
        if (shop == null) {
            return true;
        }
        if (!shop.fire(plugin.linkedOwner(args[2]))) {
            player.sendMessage(ChatColor.RED + "That player is not an employee.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Fired " + args[2] + " from " + shop.getName() + ".");
        return true;
    }

    private boolean wage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop wage <name> <amount>");
            return true;
        }
        Player player = (Player) sender;
        PlayerShop shop = findShop(player, args[1], true);
        if (shop == null) {
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        shop.setWageRate(Math.max(0.0, amount));
        player.sendMessage(ChatColor.GREEN + "Wage rate for " + shop.getName() + " set to "
                + plugin.getEconomy().format(shop.getWageRate()) + ".");
        return true;
    }

    private PlayerShop findShop(Player player, String name, boolean requireOwner) {
        String owner = plugin.linkedOwner(player.getName());
        for (PlayerShop shop : plugin.getShopManager().getShops(owner)) {
            if (shop.getName().equalsIgnoreCase(name)) {
                return shop;
            }
        }
        for (PlayerShop shop : plugin.getShopManager().getShops(owner)) {
            if (shop.getName().startsWith(name + "#")) {
                return shop;
            }
        }
        player.sendMessage(ChatColor.RED + "You don't own a shop named '" + name + "'.");
        return null;
    }
}