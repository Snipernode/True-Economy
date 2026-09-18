package com.economy.plugin.gui;

import com.economy.plugin.EconomyPlugin;
import com.economy.plugin.core.Account;
import com.economy.plugin.market.Commodity;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class MarketGui extends PaginatedGui {
    private static final String MARKET_ACCOUNT = "__market__";

    private final EconomyPlugin plugin;
    private final List<Commodity> items = new ArrayList<>();

    public MarketGui(EconomyPlugin plugin, Player player) {
        super(player, "&2&lTrue Economy Market");
        this.plugin = plugin;
        if (plugin.getMarket() != null) {
            items.addAll(plugin.getMarket().getAll());
        }
    }

    @Override
    protected List<ItemStack> getItems() {
        List<ItemStack> result = new ArrayList<>();
        for (Commodity c : items) {
            ItemStack item = namedItem(Material.BARRIER, "&b" + c.getDisplayName(), loreFor(c));
            item.setAmount(Math.max(1, Math.min(64, (int) Math.round(c.getSupply()))));
            result.add(item);
        }
        return result;
    }

    private List<String> loreFor(Commodity c) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "id: " + c.getId());
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.WHITE + plugin.getEconomy().format(c.getCurrentPrice()));
        lore.add(ChatColor.GRAY + "Supply: " + Math.round(c.getSupply()) + "  Demand: " + Math.round(c.getDemand()));
        lore.add(ChatColor.GREEN + "Left-click: buy 1");
        lore.add(ChatColor.GREEN + "Right-click: sell 1");
        return lore;
    }

    @Override
    protected void onItemClick(int slot, ClickType click) {
        int index = getPage() * PAGE_SIZE + slot;
        if (index < 0 || index >= items.size()) {
            return;
        }
        Commodity c = items.get(index);
        Player player = getPlayer();
        String owner = plugin.linkedOwner(player.getName());
        double price = c.getCurrentPrice();

        boolean selling = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;
        double units = (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) ? 16.0 : 1.0;
        double total = price * units;

        if (selling) {
            Account marketAcc = plugin.getEconomy().getExistingAccount(MARKET_ACCOUNT).orElse(null);
            if (marketAcc == null || !marketAcc.canAfford(total)) {
                player.sendMessage(ChatColor.RED + "The market cannot afford to buy that right now.");
                return;
            }
            c.sell(units, plugin.getMarket().getVolatility());
            plugin.getEconomy().transfer(MARKET_ACCOUNT, owner, total);
            plugin.getTransactions().log(com.economy.plugin.logging.TransactionType.MARKET_SELL, owner, MARKET_ACCOUNT, total, c.getId());
            player.sendMessage(ChatColor.GREEN + "Sold " + Math.round(units) + " " + c.getDisplayName() + " for " + plugin.getEconomy().format(total) + ".");
        } else {
            double tax = plugin.getGovernment().levyTransactionTax("default", total);
            double totalWithTax = total + tax;
            if (!plugin.getEconomy().transfer(owner, MARKET_ACCOUNT, totalWithTax)) {
                player.sendMessage(ChatColor.RED + "You cannot afford " + plugin.getEconomy().format(totalWithTax) + ".");
                return;
            }
            c.buy(units, plugin.getMarket().getVolatility());
            plugin.getTransactions().log(com.economy.plugin.logging.TransactionType.MARKET_BUY, MARKET_ACCOUNT, owner, total, c.getId());
            player.sendMessage(ChatColor.GREEN + "Bought " + Math.round(units) + " " + c.getDisplayName() + " for " + plugin.getEconomy().format(total) + (tax > 0.0 ? ChatColor.GRAY + " (tax " + plugin.getEconomy().format(tax) + ")" : "") + ".");
        }
        refresh();
    }
}