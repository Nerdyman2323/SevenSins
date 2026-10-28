package com.sevensins.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for creating and identifying custom Sin items.
 * Uses PersistentDataContainer for reliable NBT-based identification.
 */
public class ItemUtils {

    // PDC keys
    private static final String SIN_WEAPON_KEY = "sin_weapon";
    private static final String SIN_CORE_KEY = "sin_core";
    private static final String SIN_ALTAR_KEY = "sin_altar";

    /**
     * Build a Sin weapon ItemStack with full metadata, attributes, and PDC tag.
     */
    public static ItemStack buildSinWeapon(Plugin plugin, SinType sin) {
        ItemStack item = new ItemStack(sin.getWeaponBase());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display name (Adventure API)
        meta.displayName(legacy(sin.getWeaponName()));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(legacy("§7" + sin.getSinDisplayName()));
        lore.add(Component.empty());
        lore.add(legacy("§8Right-click to activate sin ability."));
        lore.add(legacy("§8Shift+Right for charged ability."));
        lore.add(Component.empty());
        lore.add(legacy(getWeaponLoreLine(sin)));
        meta.lore(lore);

        // Enchantments for base power feel
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        // Damage attribute
        meta.addAttributeModifier(
            Attribute.ATTACK_DAMAGE,
            new AttributeModifier(
                new NamespacedKey(plugin, "sin_damage_" + sin.name().toLowerCase()),
                getSinDamage(sin),
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
            )
        );

        // Attack speed
        meta.addAttributeModifier(
            Attribute.ATTACK_SPEED,
            new AttributeModifier(
                new NamespacedKey(plugin, "sin_speed_" + sin.name().toLowerCase()),
                -2.4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
            )
        );

        // Custom model data for resource pack texture override
        meta.setCustomModelData(getSinModelData(sin));

        // PDC tag: "sin_weapon" = sin name
        NamespacedKey key = new NamespacedKey(plugin, SIN_WEAPON_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sin.name());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Build the Core drop item from a boss kill.
     */
    public static ItemStack buildSinCore(Plugin plugin, SinType sin) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(legacy("§5§l" + sin.name().charAt(0) + sin.name().substring(1).toLowerCase() + " Core"));
        List<Component> lore = new ArrayList<>();
        lore.add(legacy("§7A fragment of " + sin.getSinDisplayName()));
        lore.add(legacy("§8Required to forge the " + sin.getWeaponName()));
        meta.lore(lore);

        meta.setCustomModelData(1000 + sin.ordinal());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        NamespacedKey key = new NamespacedKey(plugin, SIN_CORE_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sin.name());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Build the Summoning Altar block item (uses Crying Obsidian as base).
     */
    public static ItemStack buildAltarItem(Plugin plugin) {
        ItemStack item = new ItemStack(Material.CRYING_OBSIDIAN);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(legacy("§5§lSeven Sins Altar"));
        List<Component> lore = new ArrayList<>();
        lore.add(legacy("§7An ancient altar of forbidden power."));
        lore.add(legacy("§8Place it, then offer tribute above."));
        lore.add(legacy("§8The sins will answer."));
        meta.lore(lore);

        meta.setCustomModelData(2000);

        NamespacedKey key = new NamespacedKey(plugin, SIN_ALTAR_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    // ── Identification helpers ──────────────────────────────────────────────

    public static boolean isSinWeapon(Plugin plugin, ItemStack item) {
        return item != null && item.hasItemMeta() &&
               item.getItemMeta().getPersistentDataContainer()
                   .has(new NamespacedKey(plugin, SIN_WEAPON_KEY), PersistentDataType.STRING);
    }

    public static SinType getSinFromWeapon(Plugin plugin, ItemStack item) {
        if (!isSinWeapon(plugin, item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer()
                         .get(new NamespacedKey(plugin, SIN_WEAPON_KEY), PersistentDataType.STRING);
        return SinType.fromString(name);
    }

    public static boolean isSinCore(Plugin plugin, ItemStack item) {
        return item != null && item.hasItemMeta() &&
               item.getItemMeta().getPersistentDataContainer()
                   .has(new NamespacedKey(plugin, SIN_CORE_KEY), PersistentDataType.STRING);
    }

    public static SinType getSinFromCore(Plugin plugin, ItemStack item) {
        if (!isSinCore(plugin, item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer()
                         .get(new NamespacedKey(plugin, SIN_CORE_KEY), PersistentDataType.STRING);
        return SinType.fromString(name);
    }

    public static boolean isAltarItem(Plugin plugin, ItemStack item) {
        return item != null && item.hasItemMeta() &&
               item.getItemMeta().getPersistentDataContainer()
                   .has(new NamespacedKey(plugin, SIN_ALTAR_KEY), PersistentDataType.BOOLEAN);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private static double getSinDamage(SinType sin) {
        return switch (sin) {
            case LUST    -> 10.0;
            case GREED   -> 9.0;
            case SLOTH   -> 8.0;
            case WRATH   -> 14.0;
            case ENVY    -> 11.0;
            case PRIDE   -> 12.0;
            case GLUTTONY -> 13.0;
        };
    }

    private static int getSinModelData(SinType sin) {
        // Custom model data range 101–107; resource pack maps these to textures
        return 100 + sin.ordinal() + 1;
    }

    private static String getWeaponLoreLine(SinType sin) {
        return switch (sin) {
            case LUST    -> "§dAbility: Charm — enslaves nearby foes.";
            case GREED   -> "§6Ability: Gold Rush — multiplies drops.";
            case SLOTH   -> "§7Ability: Torpor — slows all nearby.";
            case WRATH   -> "§cAbility: Ignite — sets the earth ablaze.";
            case ENVY    -> "§aAbility: Mimic — copies target's armor.";
            case PRIDE   -> "§eAbility: Exalt — launches enemies skyward.";
            case GLUTTONY -> "§bAbility: Devour — steals target's life.";
        };
    }
}
