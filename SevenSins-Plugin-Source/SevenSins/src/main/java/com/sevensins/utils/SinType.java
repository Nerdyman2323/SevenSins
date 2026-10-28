package com.sevensins.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Enum representing all Seven Deadly Sins.
 * Each entry defines colors, particles, display names, base weapon material,
 * and which Minecraft mob forms the boss base.
 */
public enum SinType {

    LUST(
        "§d§lSin of Lust",
        "§5Desire Blade",
        Color.fromRGB(255, 105, 180),  // Hot pink
        Particle.HEART,
        Material.NETHERITE_SWORD,
        "RAVAGER",         // Boss mob type string
        "§d§lAriadne, the Seductress",
        new Material[]{    // Altar offering items
            Material.ROSE_BUSH,
            Material.PINK_DYE,
            Material.ENCHANTED_BOOK
        }
    ),

    GREED(
        "§6§lSin of Greed",
        "§6Midas Scepter",
        Color.fromRGB(255, 215, 0),    // Gold
        Particle.CRIT,
        Material.GOLDEN_SWORD,
        "ELDER_GUARDIAN",
        "§6§lMammon, the Hoarder",
        new Material[]{
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.EMERALD
        }
    ),

    SLOTH(
        "§8§lSin of Sloth",
        "§7Torpor Staff",
        Color.fromRGB(100, 100, 100),  // Grey
        Particle.SMOKE,
        Material.NETHERITE_HOE,
        "ELDER_GUARDIAN",
        "§8§lBelphegor, the Sleeper",
        new Material[]{
            Material.GRAY_WOOL,
            Material.SPIDER_EYE,
            Material.FERMENTED_SPIDER_EYE
        }
    ),

    WRATH(
        "§c§lSin of Wrath",
        "§4Fury Axe",
        Color.fromRGB(200, 20, 20),    // Deep red
        Particle.FLAME,
        Material.NETHERITE_AXE,
        "WARDEN",
        "§c§lAmon, the Destroyer",
        new Material[]{
            Material.NETHER_STAR,
            Material.BLAZE_POWDER,
            Material.MAGMA_CREAM
        }
    ),

    ENVY(
        "§a§lSin of Envy",
        "§2Viper Dagger",
        Color.fromRGB(0, 180, 80),     // Green
        Particle.DRAGON_BREATH,
        Material.NETHERITE_SWORD,
        "WITCH",
        "§a§lLeviathan, the Envious",
        new Material[]{
            Material.POISONOUS_POTATO,
            Material.GREEN_DYE,
            Material.PHANTOM_MEMBRANE
        }
    ),

    PRIDE(
        "§e§lSin of Pride",
        "§e Sovereign Crown",
        Color.fromRGB(255, 255, 0),    // Bright yellow
        Particle.END_ROD,
        Material.NETHERITE_SWORD,
        "ENDER_DRAGON",  // Not actual dragon — uses its loot table; boss is a Phantom cluster
        "§e§lLucifer, the Magnificent",
        new Material[]{
            Material.END_CRYSTAL,
            Material.CHORUS_FRUIT,
            Material.ENDER_EYE
        }
    ),

    GLUTTONY(
        "§b§lSin of Gluttony",
        "§3Devourer's Maw",
        Color.fromRGB(0, 180, 200),    // Cyan
        Particle.CLOUD,
        Material.NETHERITE_SWORD,
        "HOGLIN",
        "§b§lBeelzebub, the Devourer",
        new Material[]{
            Material.COOKED_PORKCHOP,
            Material.GOLDEN_APPLE,
            Material.HONEY_BOTTLE
        }
    );

    private final String sinDisplayName;
    private final String weaponName;
    private final Color particleColor;
    private final Particle particle;
    private final Material weaponBase;
    private final String bossMobType;
    private final String bossDisplayName;
    private final Material[] altarOfferings;

    SinType(String sinDisplayName, String weaponName, Color particleColor,
            Particle particle, Material weaponBase, String bossMobType,
            String bossDisplayName, Material[] altarOfferings) {
        this.sinDisplayName = sinDisplayName;
        this.weaponName = weaponName;
        this.particleColor = particleColor;
        this.particle = particle;
        this.weaponBase = weaponBase;
        this.bossMobType = bossMobType;
        this.bossDisplayName = bossDisplayName;
        this.altarOfferings = altarOfferings;
    }

    public String getSinDisplayName()   { return sinDisplayName; }
    public String getWeaponName()       { return weaponName; }
    public Color getParticleColor()     { return particleColor; }
    public Particle getParticle()       { return particle; }
    public Material getWeaponBase()     { return weaponBase; }
    public String getBossMobType()      { return bossMobType; }
    public String getBossDisplayName()  { return bossDisplayName; }
    public Material[] getAltarOfferings() { return altarOfferings; }

    /** Core item material dropped by this boss */
    public Material getCoreDropMaterial() { return Material.NETHER_STAR; }

    /** Returns SinType from string name, case-insensitive */
    public static SinType fromString(String name) {
        for (SinType s : values()) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
