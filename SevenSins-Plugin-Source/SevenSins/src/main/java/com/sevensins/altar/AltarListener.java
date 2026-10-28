package com.sevensins.altar;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.ParticleUtils;
import com.sevensins.utils.SinType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles all altar interaction:
 *  - Placing the altar block registers it with AltarManager
 *  - Breaking it unregisters
 *  - Right-clicking on the altar triggers an offering scan + boss summon
 */
public class AltarListener implements Listener {

    private final SevenSinsPlugin plugin;

    public AltarListener(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Place altar ──────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ItemUtils.isAltarItem(plugin, event.getItemInHand())) return;
        Block placed = event.getBlockPlaced();
        plugin.getAltarManager().registerAltar(placed.getLocation());
        event.getPlayer().sendMessage(legacy("§5§lSeven Sins Altar §r§7placed. " +
            "Drop the required offerings on top, then right-click to summon."));
    }

    // ── Break altar ──────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!plugin.getAltarManager().isAltar(loc)) return;
        plugin.getAltarManager().unregisterAltar(loc);
        // Drop the altar item on break
        loc.getWorld().dropItemNaturally(loc, ItemUtils.buildAltarItem(plugin));
        event.setDropItems(false);
        event.getPlayer().sendMessage(legacy("§8The altar goes silent."));
    }

    // ── Right-click altar ─────────────────────────────────────────────────────

    @EventHandler
    public void onAltarInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (!plugin.getAltarManager().isAltar(clicked.getLocation())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location altarLoc = clicked.getLocation();

        // Cooldown check
        if (plugin.getAltarManager().isOnCooldown(altarLoc)) {
            long rem = plugin.getAltarManager().getCooldownRemaining(altarLoc);
            player.sendMessage(legacy("§cThis altar is exhausted. It will reawaken in §e" + rem + "§c seconds."));
            return;
        }

        // Scan for offerings
        SinType matched = plugin.getAltarManager().detectOfferings(altarLoc);
        if (matched == null) {
            player.sendMessage(legacy("§7The altar stirs... but the offerings are incomplete."));
            showRequiredOfferings(player);
            return;
        }

        // Consume offerings and begin summon sequence
        plugin.getAltarManager().consumeOfferings(altarLoc, matched);
        plugin.getAltarManager().setCooldown(altarLoc);

        beginSummonSequence(player, altarLoc, matched);
    }

    // ── Summon sequence ───────────────────────────────────────────────────────

    private void beginSummonSequence(Player player, Location altarLoc, SinType sin) {
        player.sendMessage(legacy("§5The altar accepts your offering... §d" + sin.getSinDisplayName() + " §5awakens!"));
        altarLoc.getWorld().playSound(altarLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);

        // Dramatic 3-second countdown before spawn
        new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (count > 0) {
                    altarLoc.getWorld().playSound(altarLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    ParticleUtils.spawnSpiralPillar(altarLoc, sin.getParticleColor(), 8);
                    altarLoc.getWorld().getPlayers()
                        .forEach(p -> p.sendMessage(legacy("§8..." + count + "...")));
                    count--;
                } else {
                    // Spawn boss 5 blocks above altar
                    Location spawnLoc = altarLoc.clone().add(0, 3, 0);
                    LivingEntity boss = plugin.getBossManager().spawnBoss(spawnLoc, sin);
                    if (boss == null) {
                        player.sendMessage(legacy("§cSummon failed — no valid spawn location."));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showRequiredOfferings(Player player) {
        player.sendMessage(legacy("§7Required offerings per sin:"));
        for (SinType sin : SinType.values()) {
            StringBuilder sb = new StringBuilder("§8  " + sin.getSinDisplayName() + " §7→ ");
            for (Material m : sin.getAltarOfferings()) {
                sb.append(formatMaterial(m)).append(", ");
            }
            player.sendMessage(legacy(sb.substring(0, sb.length() - 2)));
        }
    }

    private String formatMaterial(Material m) {
        return m.name().replace("_", " ").toLowerCase();
    }

    private net.kyori.adventure.text.Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
