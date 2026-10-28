package com.sevensins.listeners;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.SinType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit for cleanup and welcome message display.
 */
public class PlayerInteractListener implements Listener {

    private final SevenSinsPlugin plugin;

    public PlayerInteractListener(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Small delay so the message arrives after login spam
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(legacy("§5§m               §r §5Seven Deadly Sins §m               "));
            player.sendMessage(legacy("§7Craft the §5Altar §7to summon Sin Bosses."));
            player.sendMessage(legacy("§7Defeat bosses for §5Sin Cores §7to forge weapons."));
            player.sendMessage(legacy("§7Type §e/sevensins help §7for admin commands."));
        }, 60L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getWeaponManager().clearCooldowns(event.getPlayer().getUniqueId());
    }

    private net.kyori.adventure.text.Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
