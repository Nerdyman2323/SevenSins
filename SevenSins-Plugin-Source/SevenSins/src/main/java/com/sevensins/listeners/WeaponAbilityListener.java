package com.sevensins.listeners;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.ParticleUtils;
import com.sevensins.utils.SinType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles weapon ability triggering:
 *  - Right-click in air/block = active ability
 *  - Hit entity = passive on-hit effect
 */
public class WeaponAbilityListener implements Listener {

    private final SevenSinsPlugin plugin;

    public WeaponAbilityListener(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Right-click ability ──────────────────────────────────────────────────

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isSinWeapon(plugin, hand)) return;

        SinType sin = ItemUtils.getSinFromWeapon(plugin, hand);
        if (sin == null) return;

        if (plugin.getWeaponManager().isOnCooldown(player.getUniqueId(), sin)) {
            long rem = plugin.getWeaponManager().getCooldownRemaining(player.getUniqueId(), sin);
            player.sendActionBar(legacy("§c" + sin.getWeaponName() + " cooldown: §e" + rem + "s"));
            return;
        }

        triggerAbility(player, sin, false);
        plugin.getWeaponManager().setCooldown(player.getUniqueId(), sin);
        event.setCancelled(true);
    }

    // ── On-hit passive ───────────────────────────────────────────────────────

    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isSinWeapon(plugin, hand)) return;

        SinType sin = ItemUtils.getSinFromWeapon(plugin, hand);
        if (sin == null) return;

        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // On-hit passive effects
        applyOnHitEffect(player, target, sin);
    }

    // ── Ability implementations ──────────────────────────────────────────────

    private void triggerAbility(Player player, SinType sin, boolean charged) {
        switch (sin) {
            case LUST    -> lustAbility(player);
            case GREED   -> greedAbility(player);
            case SLOTH   -> slothAbility(player);
            case WRATH   -> wrathAbility(player);
            case ENVY    -> envyAbility(player);
            case PRIDE   -> prideAbility(player);
            case GLUTTONY -> gluttonyAbility(player);
        }

        // Activate sound + particle burst
        ParticleUtils.burstEntity(player, sin.getParticle(), 25);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.2f);
        player.sendActionBar(legacy(sin.getWeaponName() + " §r§7— ability activated!"));
    }

    /** LUST — Charm: applies weakness+slowness to all hostiles in 12-block radius */
    private void lustAbility(Player player) {
        ParticleUtils.heartExplosion(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1.0f, 0.8f);
        getNearbyMobs(player, 12).forEach(mob -> {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 1));
            // If mob has an AI target, redirect it away from player for 3s
            if (mob instanceof Mob m && m.getTarget() instanceof Player) {
                m.setTarget(null);
            }
        });
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
    }

    /** GREED — Gold Rush: triples loot from the next kill (simulated via temp tag) + coin burst */
    private void greedAbility(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.4f);
        ParticleUtils.spawnColorRing(player.getLocation(), SinType.GREED.getParticleColor(), 24, 3.0);
        // Drop gold nuggets in an explosion ring around player
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            Location dropLoc = player.getLocation().clone().add(
                2.5 * Math.cos(angle), 0.5, 2.5 * Math.sin(angle));
            player.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.GOLD_NUGGET, 1 + (int)(Math.random() * 4)));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 200, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 1));
    }

    /** SLOTH — Torpor: freezes all nearby mobs in place temporarily */
    private void slothAbility(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.0f, 0.5f);
        ParticleUtils.burstEntity(player, org.bukkit.Particle.SMOKE, 50);
        getNearbyMobs(player, 15).forEach(mob -> {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 10));  // Near-freeze
            mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 3));
            mob.setVelocity(new Vector(0, 0, 0));
        });
        // Player gets free movement
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
    }

    /** WRATH — Ignite: 8-block fire ring, sets everyone ablaze */
    private void wrathAbility(Player player) {
        ParticleUtils.wrathFireRing(player.getLocation(), 8.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
        player.getWorld().createExplosion(player.getLocation(), 0f, false, false);
        getNearbyMobs(player, 8).forEach(mob -> {
            mob.setFireTicks(200);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
        });
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
    }

    /** ENVY — Mimic: steal the closest mob's strength; apply Poison to it */
    private void envyAbility(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1.0f, 1.2f);
        ParticleUtils.burstEntity(player, org.bukkit.Particle.DRAGON_BREATH, 30);
        // Buff player to mimic strength from target
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 1));
        // Poison nearest hostile
        getNearbyMobs(player, 10).stream().min(Comparator.comparingDouble(m ->
            m.getLocation().distanceSquared(player.getLocation())))
            .ifPresent(closest -> {
                closest.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 2));
                closest.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            });
    }

    /** PRIDE — Exalt: launch all nearby mobs upward + self-levitation control */
    private void prideAbility(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 0.6f);
        getNearbyMobs(player, 12).forEach(mob -> {
            mob.setVelocity(new Vector(0, 2.8, 0));
            ParticleUtils.prideBeam(mob.getLocation(), 6);
        });
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1));
        player.setVelocity(new Vector(0, 1.5, 0));
    }

    /** GLUTTONY — Devour: drain life from all nearby mobs to self */
    private void gluttonyAbility(Player player) {
        ParticleUtils.devourSwirl(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1.0f, 0.7f);
        double healed = 0;
        for (LivingEntity mob : getNearbyMobs(player, 8)) {
            double drain = Math.min(mob.getHealth(), 6.0);
            mob.damage(drain, player);
            healed += drain * 0.5;
        }
        double finalHeal = Math.min(healed, player.getAttribute(
            org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() - player.getHealth());
        if (finalHeal > 0) player.setHealth(player.getHealth() + finalHeal);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 2));
    }

    // ── On-hit passives ───────────────────────────────────────────────────────

    private void applyOnHitEffect(Player player, LivingEntity target, SinType sin) {
        switch (sin) {
            case LUST ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
            case GREED ->
                target.getWorld().dropItemNaturally(target.getLocation(),
                    new ItemStack(Material.GOLD_NUGGET, 1));
            case SLOTH ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, true));
            case WRATH -> {
                target.setFireTicks(60);
                ParticleUtils.burstEntity(target, org.bukkit.Particle.FLAME, 8);
            }
            case ENVY ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0, false, true));
            case PRIDE -> {
                Vector up = target.getVelocity().clone().add(new Vector(0, 0.8, 0));
                target.setVelocity(up);
            }
            case GLUTTONY -> {
                double steal = 2.0;
                if (target.getHealth() > steal) {
                    target.setHealth(target.getHealth() - steal);
                    player.setHealth(Math.min(
                        player.getHealth() + steal,
                        player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                    ));
                    ParticleUtils.devourSwirl(target.getLocation());
                }
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private List<LivingEntity> getNearbyMobs(Player player, double radius) {
        return player.getNearbyEntities(radius, radius, radius).stream()
            .filter(e -> e instanceof LivingEntity && !(e instanceof Player))
            .map(e -> (LivingEntity) e)
            .toList();
    }

    private net.kyori.adventure.text.Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
