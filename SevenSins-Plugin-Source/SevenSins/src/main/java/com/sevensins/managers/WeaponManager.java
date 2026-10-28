package com.sevensins.managers;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.SinType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active ability cooldowns for each player per sin.
 */
public class WeaponManager {

    private final SevenSinsPlugin plugin;
    // Map<PlayerUUID, Map<SinType, lastUsedTick>>
    private final Map<UUID, Map<SinType, Long>> cooldowns = new HashMap<>();

    public WeaponManager(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID player, SinType sin) {
        if (!cooldowns.containsKey(player)) return false;
        Long lastUsed = cooldowns.get(player).get(sin);
        if (lastUsed == null) return false;
        long cooldownTicks = plugin.getConfig().getLong(
            "weapons." + sin.name().toLowerCase() + "-cooldown", 200L);
        return (System.currentTimeMillis() - lastUsed) < cooldownTicks * 50L; // ticks → ms
    }

    public long getCooldownRemaining(UUID player, SinType sin) {
        if (!cooldowns.containsKey(player)) return 0;
        Long lastUsed = cooldowns.get(player).get(sin);
        if (lastUsed == null) return 0;
        long cooldownMs = plugin.getConfig().getLong(
            "weapons." + sin.name().toLowerCase() + "-cooldown", 200L) * 50L;
        long elapsed = System.currentTimeMillis() - lastUsed;
        return Math.max(0, (cooldownMs - elapsed) / 1000);
    }

    public void setCooldown(UUID player, SinType sin) {
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                 .put(sin, System.currentTimeMillis());
    }

    public void clearCooldowns(UUID player) {
        cooldowns.remove(player);
    }
}
