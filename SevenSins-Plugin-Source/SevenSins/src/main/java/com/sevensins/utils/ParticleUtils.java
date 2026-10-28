package com.sevensins.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Centralised particle effect utilities for weapons, bosses, and the altar.
 */
public class ParticleUtils {

    /**
     * Spawn a ring of colored dust particles around a location.
     */
    public static void spawnColorRing(Location center, Color color, int count, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }

    /**
     * Spiral pillar — used for boss spawns and altar activation.
     */
    public static void spawnSpiralPillar(Location base, Color color, int height) {
        World world = base.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 2.0f);
        for (int y = 0; y <= height; y++) {
            double angle = y * 0.7;
            double x = base.getX() + 1.0 * Math.cos(angle);
            double z = base.getZ() + 1.0 * Math.sin(angle);
            Location loc = new Location(world, x, base.getY() + y, z);
            world.spawnParticle(Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0, dust);
        }
    }

    /**
     * Burst of standard particles on an entity (ability activation).
     */
    public static void burstEntity(Entity entity, Particle particle, int count) {
        Location loc = entity.getLocation().add(0, 1, 0);
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Radial explosion of LUST heart particles.
     */
    public static void heartExplosion(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * 3.0;
            Location loc = center.clone().add(
                radius * Math.cos(angle),
                Math.random() * 2,
                radius * Math.sin(angle)
            );
            world.spawnParticle(Particle.HEART, loc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Lava/fire ring for WRATH ignite ability.
     */
    public static void wrathFireRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI * i) / 30;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(Particle.FLAME, loc, 5, 0.1, 0.5, 0.1, 0.05);
            world.spawnParticle(Particle.LAVA, loc, 2, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * PRIDE launch — beam rising from target.
     */
    public static void prideBeam(Location bottom, int height) {
        World world = bottom.getWorld();
        if (world == null) return;
        for (int y = 0; y < height; y++) {
            Location loc = bottom.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, loc, 3, 0.2, 0.0, 0.2, 0.02);
        }
    }

    /**
     * GLUTTONY devour swirl inward toward player.
     */
    public static void devourSwirl(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI * i) / 30;
            double radius = 3.0 - (i * 0.1);
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 1, z);
            world.spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * Altar idle ambient glow — called on a repeating task.
     */
    public static void altarAmbient(Location altarLoc, Color color) {
        spawnColorRing(altarLoc.clone().add(0, 0.5, 0), color, 16, 1.5);
        spawnColorRing(altarLoc.clone().add(0, 1.0, 0), color, 12, 1.0);
    }

    /**
     * Boss death explosion burst.
     */
    public static void bossDeathBurst(Location loc, Color color) {
        World world = loc.getWorld();
        if (world == null) return;
        spawnSpiralPillar(loc, color, 12);
        Particle.DustOptions dust = new Particle.DustOptions(color, 3.0f);
        world.spawnParticle(Particle.DUST, loc.clone().add(0, 2, 0), 80, 2, 2, 2, 0, dust);
        world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 5, 1, 1, 1, 0.1);
    }
}
