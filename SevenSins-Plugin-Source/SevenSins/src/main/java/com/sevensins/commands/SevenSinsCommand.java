package com.sevensins.commands;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.SinType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /sevensins and /summonboss commands.
 *
 * /sevensins give <sin|altar|core> [sin]  — give an item to self
 * /sevensins reload                        — reload config
 * /sevensins list                          — list all sins
 * /summonboss <sin>                        — spawn boss at player location
 */
public class SevenSinsCommand implements CommandExecutor {

    private final SevenSinsPlugin plugin;

    public SevenSinsCommand(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("summonboss")) {
            return handleSummonBoss(sender, args);
        }
        return handleSevenSins(sender, args);
    }

    private boolean handleSevenSins(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> showHelp(sender);

            case "give" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("In-game only."); return true; }
                if (args.length < 2) { p.sendMessage(legacy("§cUsage: /sevensins give <weapon|altar|core> [sin]")); return true; }
                switch (args[1].toLowerCase()) {
                    case "altar" -> {
                        p.getInventory().addItem(ItemUtils.buildAltarItem(plugin));
                        p.sendMessage(legacy("§5Given: Seven Sins Altar"));
                    }
                    case "weapon" -> {
                        if (args.length < 3) { p.sendMessage(legacy("§cSpecify a sin: " + listSins())); return true; }
                        SinType sin = SinType.fromString(args[2]);
                        if (sin == null) { p.sendMessage(legacy("§cUnknown sin. Options: " + listSins())); return true; }
                        p.getInventory().addItem(ItemUtils.buildSinWeapon(plugin, sin));
                        p.sendMessage(legacy("§5Given: " + sin.getWeaponName()));
                    }
                    case "core" -> {
                        if (args.length < 3) { p.sendMessage(legacy("§cSpecify a sin: " + listSins())); return true; }
                        SinType sin = SinType.fromString(args[2]);
                        if (sin == null) { p.sendMessage(legacy("§cUnknown sin. Options: " + listSins())); return true; }
                        p.getInventory().addItem(ItemUtils.buildSinCore(plugin, sin));
                        p.sendMessage(legacy("§5Given: " + sin.name() + " Core"));
                    }
                    default -> p.sendMessage(legacy("§cUsage: /sevensins give <weapon|altar|core> [sin]"));
                }
            }

            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(legacy("§aSevenSins config reloaded."));
            }

            case "list" -> {
                sender.sendMessage(legacy("§5Seven Deadly Sins:"));
                for (SinType sin : SinType.values()) {
                    sender.sendMessage(legacy("  §7" + sin.name() + " → " + sin.getWeaponName() +
                        " | Boss: " + sin.getBossDisplayName()));
                }
            }

            default -> showHelp(sender);
        }
        return true;
    }

    private boolean handleSummonBoss(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("In-game only."); return true; }
        if (args.length < 1) { p.sendMessage(legacy("§cUsage: /summonboss <sin>")); return true; }

        SinType sin = SinType.fromString(args[0]);
        if (sin == null) {
            p.sendMessage(legacy("§cUnknown sin. Options: " + listSins()));
            return true;
        }

        plugin.getBossManager().spawnBoss(p.getLocation(), sin);
        p.sendMessage(legacy("§5Summoning " + sin.getBossDisplayName() + "§5..."));
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(legacy("§5=== SevenSins Commands ==="));
        sender.sendMessage(legacy("§e/ss give weapon <sin> §7— Give yourself a sin weapon"));
        sender.sendMessage(legacy("§e/ss give core <sin>   §7— Give yourself a sin core"));
        sender.sendMessage(legacy("§e/ss give altar        §7— Give yourself the altar block"));
        sender.sendMessage(legacy("§e/ss list              §7— List all sins/weapons/bosses"));
        sender.sendMessage(legacy("§e/ss reload            §7— Reload config.yml"));
        sender.sendMessage(legacy("§e/summonboss <sin>     §7— Spawn a boss at your location"));
        sender.sendMessage(legacy("§7Sins: " + listSins()));
    }

    private String listSins() {
        StringBuilder sb = new StringBuilder();
        for (SinType s : SinType.values()) sb.append(s.name().toLowerCase()).append(", ");
        return sb.substring(0, sb.length() - 2);
    }

    private net.kyori.adventure.text.Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
