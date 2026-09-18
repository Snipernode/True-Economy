package com.economy.plugin;

import com.economy.plugin.seasonal.SeasonalEventManager;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EventsCommand implements CommandExecutor {
    private final EconomyPlugin plugin;

    public EventsCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getSeasonal() == null) {
            sender.sendMessage(ChatColor.RED + "Seasonal events are disabled.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("history")) {
            return history(sender);
        }
        return status(sender);
    }

    private String describe(SeasonalEventManager.MarketEvent event, boolean active) {
        String type = event.type == SeasonalEventManager.EventType.CRISIS ? "Crisis"
                : event.type == SeasonalEventManager.EventType.BOOM ? "Boom" : "Event";
        String state = active ? ChatColor.GREEN + " (active)" : ChatColor.DARK_GRAY + " (ended)";
        return ChatColor.AQUA + "[" + type + "]" + ChatColor.WHITE + " " + event.commodityId
                + ChatColor.GRAY + " x" + event.multiplier + state;
    }

    private boolean status(CommandSender sender) {
        SeasonalEventManager seasonal = plugin.getSeasonal();
        List<SeasonalEventManager.MarketEvent> active = seasonal.getActiveEvents();
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Seasonal Market Events:");
        if (active.isEmpty()) {
            long remaining = Math.max(0L, (seasonal.getNextEventTime() - System.currentTimeMillis()) / 1000L);
            sender.sendMessage(ChatColor.GRAY + "No active events. Next event rolls in ~" + ChatColor.WHITE + remaining + "s");
        } else {
            for (SeasonalEventManager.MarketEvent e : active) {
                sender.sendMessage(describe(e, true));
            }
        }
        return true;
    }

    private boolean history(CommandSender sender) {
        List<SeasonalEventManager.MarketEvent> history = plugin.getSeasonal().getHistory();
        if (history.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No market events have occurred yet.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Recent Market Events:");
        for (SeasonalEventManager.MarketEvent e : history) {
            sender.sendMessage(describe(e, e.isActive()));
        }
        return true;
    }
}