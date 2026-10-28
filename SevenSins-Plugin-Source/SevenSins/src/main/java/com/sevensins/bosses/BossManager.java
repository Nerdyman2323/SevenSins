package com.sevensins.bosses;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.ParticleUtils;
import com.sevensins.utils.SinType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages spawning, ability ticks, boss bars, and death handling for all seven sin bosses.
 * Each boss is a modified vanilla mob with custom attributes, a BossBar, and phased abilities.
 */
public class BossManager implements Listener {

    private final SevenSinsPlugin plugin;
    private final NamespacedKey bossKey;

    // Active bosses: entityUUID → SinType
    private final Map<UUID, SinType> activeBosses = new HashMap<>();
    // Boss bars per entity
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public BossManager(SevenSinsPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "sin_boss");
        startAbilityTicker();
    }

    // ── Spawning ────────────────────────────────────────────────────────────

    /**
     * Spawns a boss for the given SinType at the given location.
     * Returns the spawned LivingEntity.
     */
    public LivingEntity spawnBoss(Location loc, SinType sin) {
        World world = loc.getWorld();
        if (world == null) return null;

        LivingEntity boss = switch (sin) {
            case LUST    -> spawnRavager(loc, sin);
            case GREED   -> spawnElderGuardian(loc, sin);
            case SLOTH   -> spawnSlothBoss(loc, sin);
            case WRATH   -> spawnWarden(loc, sin);
            case ENVY    -> spawnEnvyBoss(loc, sin);
            case PRIDE   -> spawnPrideBoss(loc, sin);
            case GLUTTONY -> spawnGluttonyBoss(loc, sin);
        };

        if (boss == null) return null;

        applyBossAttributes(boss, sin);
        tagBoss(boss, sin);
        createBossBar(boss, sin);
        activeBosses.put(boss.getUniqueId(), sin);

        // Dramatic spawn effects
        world.strikeLightningEffect(loc);
        ParticleUtils.spawnSpiralPillar(loc, sin.getParticleColor(), 15);
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        // Announce
        Component msg = LegacyComponentSerializer.legacySection()
            .deserialize(sin.getSinDisplayName() + " §r§7has awakened!");
        world.getPlayers().forEach(p -> p.sendMessage(msg));

        plugin.getLogger().info("Spawned boss: " + sin.getBossDisplayName() + " at " + loc);
        return boss;
    }

    // ── Individual boss spawn methods ────────────────────────────────────────

    private Ravager spawnRavager(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, Ravager.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
        });
    }

    private ElderGuardian spawnElderGuardian(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, ElderGuardian.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
        });
    }

    private Ravager spawnSlothBoss(Location loc, SinType sin) {
        // Sloth uses a massive slow Ravager
        return loc.getWorld().spawn(loc, Ravager.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 3, false, false));
        });
    }

    private Warden spawnWarden(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, Warden.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
        });
    }

    private Witch spawnEnvyBoss(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, Witch.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
        });
    }

    private Phantom spawnPrideBoss(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, Phantom.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
            e.setSize(10); // giant phantom
        });
    }

    private Hoglin spawnGluttonyBoss(Location loc, SinType sin) {
        return loc.getWorld().spawn(loc, Hoglin.class, e -> {
            e.customName(legacy(sin.getBossDisplayName()));
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
        });
    }

    // ── Attribute configuration ─────────────────────────────────────────────

    private void applyBossAttributes(LivingEntity boss, SinType sin) {
        String path = "bosses." + sin.name().toLowerCase() + ".";
        double health = plugin.getConfig().getDouble(path + "health", 400.0);
        double speed  = plugin.getConfig().getDouble(path + "speed-multiplier", 0.25);
        double damage = plugin.getConfig().getDouble(path + "damage-multiplier", 1.5);

        AttributeInstance maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) { maxHealth.setBaseValue(health); boss.setHealth(health); }

        AttributeInstance movSpeed = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movSpeed != null) movSpeed.setBaseValue(speed);

        AttributeInstance atkDamage = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atkDamage != null) atkDamage.setBaseValue(atkDamage.getBaseValue() * damage);

        // Scale visual size via bounding box hack — Paper API
        // Note: actual size attribute only works on slimes/phantoms natively.
        // For other mobs we use a potion strength trick and name tag to suggest scale.
        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
    }

    private void tagBoss(LivingEntity boss, SinType sin) {
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, sin.name());
    }

    // ── Boss bar ─────────────────────────────────────────────────────────────

    private void createBossBar(LivingEntity boss, SinType sin) {
        BarColor color = switch (sin) {
            case LUST    -> BarColor.PINK;
            case GREED   -> BarColor.YELLOW;
            case SLOTH   -> BarColor.WHITE;
            case WRATH   -> BarColor.RED;
            case ENVY    -> BarColor.GREEN;
            case PRIDE   -> BarColor.YELLOW;
            case GLUTTONY -> BarColor.BLUE;
        };

        BossBar bar = plugin.getServer().createBossBar(
            sin.getBossDisplayName().replaceAll("§[0-9a-fk-or]", ""),
            color, BarStyle.SEGMENTED_10
        );

        // Add all nearby players in the world
        if (boss.getWorld() != null) {
            boss.getWorld().getPlayers().forEach(bar::addPlayer);
        }

        bossBars.put(boss.getUniqueId(), bar);
    }

    // ── Periodic ability ticker ──────────────────────────────────────────────

    private void startAbilityTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, SinType> entry : new HashMap<>(activeBosses).entrySet()) {
                    UUID uid = entry.getKey();
                    SinType sin = entry.getValue();
                    Entity e = plugin.getServer().getEntity(uid);
                    if (e == null || e.isDead() || !(e instanceof LivingEntity boss)) {
                        activeBosses.remove(uid);
                        removeBossBar(uid);
                        continue;
                    }

                    // Update boss bar
                    updateBossBar(uid, boss, sin);
                    // Ambient particles
                    ParticleUtils.altarAmbient(boss.getLocation(), sin.getParticleColor());
                    // Phase abilities every 5 seconds (100 ticks)
                    if (plugin.getServer().getCurrentTick() % 100 == 0) {
                        useBossAbility(boss, sin);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void updateBossBar(UUID uid, LivingEntity boss, SinType sin) {
        BossBar bar = bossBars.get(uid);
        if (bar == null) return;
        double maxHp = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
        bar.setProgress(Math.max(0, boss.getHealth() / maxHp));

        // Add nearby players who joined late
        if (boss.getWorld() != null) {
            boss.getNearbyEntities(64, 64, 64).stream()
                .filter(en -> en instanceof Player)
                .map(en -> (Player) en)
                .forEach(bar::addPlayer);
        }
    }

    private void removeBossBar(UUID uid) {
        BossBar bar = bossBars.remove(uid);
        if (bar != null) { bar.removeAll(); }
    }

    // ── Abilities ────────────────────────────────────────────────────────────

    private void useBossAbility(LivingEntity boss, SinType sin) {
        switch (sin) {
            case LUST    -> lustAbility(boss);
            case GREED   -> greedAbility(boss);
            case SLOTH   -> slothAbility(boss);
            case WRATH   -> wrathAbility(boss);
            case ENVY    -> envyAbility(boss);
            case PRIDE   -> prideAbility(boss);
            case GLUTTONY -> gluttonyAbility(boss);
        }
    }

    private void lustAbility(LivingEntity boss) {
        // Charm: applies Nausea + Slowness to nearby players, pulls them toward boss
        ParticleUtils.heartExplosion(boss.getLocation());
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.2f, 0.8f);
        getNearbyPlayers(boss, 12).forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
            // Gentle pull
            org.bukkit.util.Vector dir = boss.getLocation().toVector()
                .subtract(p.getLocation().toVector()).normalize().multiply(0.6);
            dir.setY(0.15);
            p.setVelocity(dir);
        });
    }

    private void greedAbility(LivingEntity boss) {
        // Gold Rush: summons 3 zombie assistants holding gold; brief blindness on players
        Location loc = boss.getLocation();
        boss.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
        ParticleUtils.burstEntity(boss, boss.getWorld() != null ? org.bukkit.Particle.CRIT : org.bukkit.Particle.CRIT, 40);
        getNearbyPlayers(boss, 15).forEach(p ->
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0))
        );
        for (int i = 0; i < 3; i++) {
            Zombie z = loc.getWorld().spawn(loc.clone().add(randomOffset(), 0, randomOffset()), Zombie.class);
            z.customName(legacy("§6Greed's Thrall"));
            z.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        }
    }

    private void slothAbility(LivingEntity boss) {
        // Torpor: massive slowness + mining fatigue area
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
        ParticleUtils.burstEntity(boss, org.bukkit.Particle.SMOKE, 60);
        getNearbyPlayers(boss, 18).forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2));
        });
    }

    private void wrathAbility(LivingEntity boss) {
        // Ignite ring: fire ring, sets nearby players on fire
        ParticleUtils.wrathFireRing(boss.getLocation(), 5.0);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
        getNearbyPlayers(boss, 8).forEach(p -> {
            p.setFireTicks(100);
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 1));
        });
        // Also strike lightning on a random nearby player
        getNearbyPlayers(boss, 12).stream().findAny().ifPresent(p ->
            boss.getWorld().strikeLightning(p.getLocation())
        );
    }

    private void envyAbility(LivingEntity boss) {
        // Mimic: steals a random armor piece concept — applies Poison + Wither
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITCH_THROW, 1.0f, 1.2f);
        ParticleUtils.burstEntity(boss, org.bukkit.Particle.DRAGON_BREATH, 30);
        getNearbyPlayers(boss, 14).forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
        });
    }

    private void prideAbility(LivingEntity boss) {
        // Exalt: launches a random player upward
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 0.7f);
        getNearbyPlayers(boss, 20).stream().findAny().ifPresent(p -> {
            p.setVelocity(new org.bukkit.util.Vector(0, 3.5, 0));
            ParticleUtils.prideBeam(p.getLocation(), 10);
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1));
        });
    }

    private void gluttonyAbility(LivingEntity boss) {
        // Devour: drain health from nearby players into boss
        ParticleUtils.devourSwirl(boss.getLocation());
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1.0f, 0.6f);
        getNearbyPlayers(boss, 10).forEach(p -> {
            double steal = 4.0;
            p.damage(steal, boss);
            double newHp = Math.min(boss.getHealth() + steal,
                Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue());
            boss.setHealth(newHp);
        });
    }

    // ── Death handling ───────────────────────────────────────────────────────

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        UUID uid = dead.getUniqueId();
        if (!activeBosses.containsKey(uid)) return;

        SinType sin = activeBosses.remove(uid);
        removeBossBar(uid);

        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Drop sin core (always)
        event.getDrops().add(ItemUtils.buildSinCore(plugin, sin));
        // Drop sin weapon (chance — balanced at 20%)
        if (Math.random() < 0.20) event.getDrops().add(ItemUtils.buildSinWeapon(plugin, sin));
        // Bonus netherite
        event.getDrops().add(new ItemStack(Material.NETHERITE_INGOT, 2 + (int)(Math.random() * 3)));
        // XP
        event.setDroppedExp(500 + sin.ordinal() * 50);

        // Death effects
        ParticleUtils.bossDeathBurst(dead.getLocation(), sin.getParticleColor());
        dead.getWorld().strikeLightningEffect(dead.getLocation());
        dead.getWorld().playSound(dead.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);

        // Announce death
        Component msg = LegacyComponentSerializer.legacySection()
            .deserialize("§7The " + sin.getSinDisplayName() + " §r§7has been vanquished! A Sin Core has been dropped.");
        dead.getWorld().getPlayers().forEach(p -> p.sendMessage(msg));

        plugin.getLogger().info("Boss killed: " + sin.name());
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!activeBosses.containsKey(victim.getUniqueId())) return;
        SinType sin = activeBosses.get(victim.getUniqueId());

        // Rage phase: below 50% HP the boss gains extra effects
        double maxHp = Objects.requireNonNull(victim.getAttribute(Attribute.MAX_HEALTH)).getValue();
        if (victim.getHealth() < maxHp * 0.5) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 1, false, false));
            // Flash red particle on hit
            ParticleUtils.burstEntity(victim, org.bukkit.Particle.CRIT_MAGIC, 10);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isBoss(Entity entity) {
        return entity instanceof LivingEntity &&
               entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    public SinType getBossSin(Entity entity) {
        String name = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        return name != null ? SinType.fromString(name) : null;
    }

    public void removeAllBosses() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        activeBosses.clear();
    }

    private List<Player> getNearbyPlayers(LivingEntity entity, double radius) {
        return entity.getNearbyEntities(radius, radius, radius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .toList();
    }

    private double randomOffset() { return (Math.random() - 0.5) * 4; }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
