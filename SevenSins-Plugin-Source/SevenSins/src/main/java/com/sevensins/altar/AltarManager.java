package com.sevensins.altar;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.ParticleUtils;
import com.sevensins.utils.SinType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Tracks all placed Sin Altars by location.
 * Manages offering detection, cooldowns, and ambient particle loops.
 */
public class AltarManager {

    private final SevenSinsPlugin plugin;

    // Set of placed altar locations
    private final Set<Location> altarLocations = new HashSet<>();
    // Cooldown: location → last summon timestamp
    private final Map<Location, Long> altarCooldowns = new HashMap<>();
    // Ambient task IDs per altar location
    private final Map<Location, Integer> ambientTasks = new HashMap<>();

    public AltarManager(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public void registerAltar(Location loc) {
        Location normalized = normalizeLocation(loc);
        altarLocations.add(normalized);
        startAmbientEffect(normalized);
        plugin.getLogger().info("Altar registered at " + normalized);
    }

    public void unregisterAltar(Location loc) {
        Location normalized = normalizeLocation(loc);
        altarLocations.remove(normalized);
        stopAmbientEffect(normalized);
    }

    public boolean isAltar(Location loc) {
        return altarLocations.contains(normalizeLocation(loc));
    }

    // ── Cooldown ─────────────────────────────────────────────────────────────

    public boolean isOnCooldown(Location loc) {
        Long last = altarCooldowns.get(normalizeLocation(loc));
        if (last == null) return false;
        long cooldownMs = plugin.getConfig().getLong("altar.summon-cooldown", 300L) * 1000L;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    public long getCooldownRemaining(Location loc) {
        Long last = altarCooldowns.get(normalizeLocation(loc));
        if (last == null) return 0;
        long cooldownMs = plugin.getConfig().getLong("altar.summon-cooldown", 300L) * 1000L;
        return Math.max(0, (cooldownMs - (System.currentTimeMillis() - last)) / 1000);
    }

    public void setCooldown(Location loc) {
        altarCooldowns.put(normalizeLocation(loc), System.currentTimeMillis());
    }

    // ── Offering detection ────────────────────────────────────────────────────

    /**
     * Scans item entities within the altar's radius and checks if the ground items
     * match a specific sin's altar offerings. Returns the matched SinType, or null.
     */
    public SinType detectOfferings(Location altarLoc) {
        double radius = plugin.getConfig().getDouble("altar.activation-radius", 2.0);

        // Collect all item-entity materials near the altar
        Set<Material> groundItems = new HashSet<>();
        altarLoc.getWorld().getNearbyEntities(altarLoc, radius, radius + 1, radius)
            .stream()
            .filter(e -> e instanceof Item)
            .map(e -> ((Item) e).getItemStack().getType())
            .forEach(groundItems::add);

        if (groundItems.isEmpty()) return null;

        // Check each sin's required offerings
        for (SinType sin : SinType.values()) {
            boolean allPresent = true;
            for (Material required : sin.getAltarOfferings()) {
                if (!groundItems.contains(required)) {
                    allPresent = false;
                    break;
                }
            }
            if (allPresent) return sin;
        }
        return null;
    }

    /**
     * Consumes (removes) the offering items near the altar after a successful match.
     */
    public void consumeOfferings(Location altarLoc, SinType sin) {
        double radius = plugin.getConfig().getDouble("altar.activation-radius", 2.0);
        Set<Material> toConsume = new HashSet<>(Arrays.asList(sin.getAltarOfferings()));

        altarLoc.getWorld().getNearbyEntities(altarLoc, radius, radius + 1, radius)
            .stream()
            .filter(e -> e instanceof Item)
            .map(e -> (Item) e)
            .filter(item -> toConsume.contains(item.getItemStack().getType()))
            .forEach(Item::remove);
    }

    // ── Ambient FX ────────────────────────────────────────────────────────────

    private void startAmbientEffect(Location loc) {
        if (ambientTasks.containsKey(loc)) return;
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!altarLocations.contains(loc)) {
                    cancel();
                    ambientTasks.remove(loc);
                    return;
                }
                // Cycle through sin colors based on time
                SinType[] sins = SinType.values();
                SinType current = sins[(int)((System.currentTimeMillis() / 3000) % sins.length)];
                ParticleUtils.altarAmbient(loc.clone().add(0.5, 0, 0.5), current.getParticleColor());

                // Occasional chime sound
                if (Math.random() < 0.15) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
        ambientTasks.put(loc, taskId);
    }

    private void stopAmbientEffect(Location loc) {
        Integer taskId = ambientTasks.remove(loc);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Set<Location> getAltarLocations() { return Collections.unmodifiableSet(altarLocations); }
}
