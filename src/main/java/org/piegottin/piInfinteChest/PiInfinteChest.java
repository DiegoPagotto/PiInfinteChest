package org.piegottin.piInfinteChest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.commands.InfiniteChestCommand;
import org.piegottin.piInfinteChest.gui.GUIManager;
import org.piegottin.piInfinteChest.listeners.InfiniteChestListener;
import org.piegottin.piInfinteChest.managers.ChestManager;

public final class PiInfinteChest extends JavaPlugin {
    private ChestManager chestManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        chestManager = new ChestManager(this);
        guiManager = new GUIManager(this, chestManager);

        getServer().getPluginManager().registerEvents(new InfiniteChestListener(chestManager, guiManager), this);
        getCommand("infinitechest").setExecutor(new InfiniteChestCommand(guiManager));

        chestManager.loadChests();
        registerInfiniteChestRecipe();
        getLogger().info("PiInfinteChest has been enabled!");
    }

    @Override
    public void onDisable() {
        chestManager.saveChests();
        getLogger().info("PiInfinteChest has been disabled!");
    }

    private void registerInfiniteChestRecipe() {
        ItemStack infiniteChest = guiManager.getInfiniteChestItem();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "infinite_chest"), infiniteChest);
        recipe.shape(
                "GDG",
                "DCD",
                "GDG"
        );
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('C', Material.CHEST);

        if (Bukkit.getRecipe(new NamespacedKey(this, "infinite_chest")) == null) {
            Bukkit.addRecipe(recipe);
            getLogger().info("Infinite Chest recipe registered!");
        }
    }
}
