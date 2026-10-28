package com.sevensins;

import com.sevensins.altar.AltarListener;
import com.sevensins.altar.AltarManager;
import com.sevensins.bosses.BossManager;
import com.sevensins.commands.SevenSinsCommand;
import com.sevensins.listeners.PlayerInteractListener;
import com.sevensins.listeners.WeaponAbilityListener;
import com.sevensins.managers.CraftingManager;
import com.sevensins.managers.WeaponManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SevenSinsPlugin extends JavaPlugin {

    private static SevenSinsPlugin instance;
    private WeaponManager weaponManager;
    private BossManager bossManager;
    private AltarManager altarManager;
    private CraftingManager craftingManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize managers
        weaponManager = new WeaponManager(this);
        bossManager = new BossManager(this);
        altarManager = new AltarManager(this);
        craftingManager = new CraftingManager(this);

        // Register crafting recipes
        craftingManager.registerAllRecipes();

        // Register listeners
        getServer().getPluginManager().registerEvents(new WeaponAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarListener(this), this);
        getServer().getPluginManager().registerEvents(bossManager, this);

        // Register commands
        getCommand("sevensins").setExecutor(new SevenSinsCommand(this));
        getCommand("summonboss").setExecutor(new SevenSinsCommand(this));

        getLogger().info("=== Seven Deadly Sins Plugin Enabled ===");
        getLogger().info("7 Weapons | 7 Bosses | Custom Altar | Paper 1.21.1");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.removeAllBosses();
        getLogger().info("SevenSins disabled. The sins sleep.");
    }

    public static SevenSinsPlugin getInstance() { return instance; }
    public WeaponManager getWeaponManager() { return weaponManager; }
    public BossManager getBossManager() { return bossManager; }
    public AltarManager getAltarManager() { return altarManager; }
    public CraftingManager getCraftingManager() { return craftingManager; }
}
