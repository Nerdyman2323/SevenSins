package com.sevensins.managers;

import com.sevensins.SevenSinsPlugin;
import com.sevensins.utils.ItemUtils;
import com.sevensins.utils.SinType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers all Seven Sins crafting recipes.
 *
 * Recipe logic:
 *  - Altar: 4 Obsidian + 4 Crying Obsidian + 1 Nether Star (3x3)
 *  - Each weapon: sin core (center) + netherite/gold base + 2 sin-specific materials
 */
public class CraftingManager {

    private final SevenSinsPlugin plugin;

    public CraftingManager(SevenSinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAllRecipes() {
        registerAltarRecipe();
        for (SinType sin : SinType.values()) {
            registerWeaponRecipe(sin);
        }
        plugin.getLogger().info("All Seven Sins recipes registered.");
    }

    // ── Altar ───────────────────────────────────────────────────────────────

    private void registerAltarRecipe() {
        ItemStack altar = ItemUtils.buildAltarItem(plugin);
        NamespacedKey key = new NamespacedKey(plugin, "sin_altar");
        ShapedRecipe recipe = new ShapedRecipe(key, altar);
        recipe.shape("OBO", "BNB", "OBO");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('B', Material.CRYING_OBSIDIAN);
        recipe.setIngredient('N', Material.NETHER_STAR);
        plugin.getServer().addRecipe(recipe);
    }

    // ── Weapons ─────────────────────────────────────────────────────────────

    private void registerWeaponRecipe(SinType sin) {
        ItemStack weapon = ItemUtils.buildSinWeapon(plugin, sin);
        NamespacedKey key = new NamespacedKey(plugin, "sin_weapon_" + sin.name().toLowerCase());

        ItemStack core = ItemUtils.buildSinCore(plugin, sin);

        // Layout varies per sin for a unique feel, but all share core in center
        ShapedRecipe recipe = switch (sin) {
            case LUST    -> lustRecipe(key, weapon, core);
            case GREED   -> greedRecipe(key, weapon, core);
            case SLOTH   -> slothRecipe(key, weapon, core);
            case WRATH   -> wrathRecipe(key, weapon, core);
            case ENVY    -> envyRecipe(key, weapon, core);
            case PRIDE   -> prideRecipe(key, weapon, core);
            case GLUTTONY -> gluttonyRecipe(key, weapon, core);
        };

        plugin.getServer().addRecipe(recipe);
    }

    // ── Per-sin recipe layouts ───────────────────────────────────────────────
    // All use the Sin Core (which has custom NBT) as the 'C' ingredient.
    // Due to Bukkit API limitations, only non-custom items can be exact-matched in shaped recipes.
    // The core is matched by material (NETHER_STAR) — players obtain cores from boss drops.

    private ShapedRecipe lustRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("PCP", "NSN", " N ");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('S', Material.NETHERITE_SWORD);
        r.setIngredient('P', Material.PINK_DYE);
        return r;
    }

    private ShapedRecipe greedRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("GCG", "GSG", " G ");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('G', Material.GOLD_BLOCK);
        r.setIngredient('S', Material.GOLDEN_SWORD);
        return r;
    }

    private ShapedRecipe slothRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape(" C ", "WHW", " N ");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('W', Material.GRAY_WOOL);
        r.setIngredient('H', Material.NETHERITE_HOE);
        r.setIngredient('N', Material.NETHERITE_INGOT);
        return r;
    }

    private ShapedRecipe wrathRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("FBF", "BAB", "FCF");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('F', Material.BLAZE_POWDER);
        r.setIngredient('B', Material.MAGMA_BLOCK);
        r.setIngredient('A', Material.NETHERITE_AXE);
        return r;
    }

    private ShapedRecipe envyRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("PCP", "NSN", "PCP");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('P', Material.PHANTOM_MEMBRANE);
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('S', Material.NETHERITE_SWORD);
        return r;
    }

    private ShapedRecipe prideRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("ECE", "ESE", " E ");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('E', Material.END_CRYSTAL);
        r.setIngredient('S', Material.NETHERITE_SWORD);
        return r;
    }

    private ShapedRecipe gluttonyRecipe(NamespacedKey key, ItemStack out, ItemStack core) {
        ShapedRecipe r = new ShapedRecipe(key, out);
        r.shape("HCH", "HSH", "HGH");
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('H', Material.HONEY_BLOCK);
        r.setIngredient('S', Material.NETHERITE_SWORD);
        r.setIngredient('G', Material.GOLDEN_APPLE);
        return r;
    }
}
