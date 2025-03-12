package org.piegottin.piInfinteChest.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.domain.ChestData;
import org.piegottin.piInfinteChest.managers.ChestManager;

import java.util.*;

public class GUIManager {
    private static final String INFINITE_CHEST_NAME = ChatColor.GOLD + "Infinite Chest";
    private static final String INFINITE_CHEST_TITLE = ChatColor.DARK_GRAY + "Infinite Chest";
    private final JavaPlugin plugin;
    private final ChestManager chestManager;
    private final Map<Inventory, Location> openInventories = new HashMap<>();
    private final Map<Player, Integer> inventoryUpdateTasks = new HashMap<>();

    public GUIManager(JavaPlugin plugin, ChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
    }

    public ItemStack getInfiniteChestItem() {
        ItemStack chestItem = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = chestItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(INFINITE_CHEST_NAME);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Coloque no chão para",
                    ChatColor.GRAY + "obter um baú infinito."
            ));
            chestItem.setItemMeta(meta);
        }
        return chestItem;
    }

    public void openInfiniteChestGUI(Player player, Location chestLocation) {
        Inventory inv = Bukkit.createInventory(null, 27, INFINITE_CHEST_TITLE);
        ItemStack placeholder = getPlaceholderItem();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, placeholder);
        }
        updateCenterSlot(inv, chestLocation);
        openInventories.put(inv, chestLocation);
        player.openInventory(inv);
        startInventoryUpdateTask(player, inv, chestLocation);
    }

    private ItemStack getPlaceholderItem() {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    public void updateCenterSlot(Inventory inv, Location chestLocation) {
        ChestData data = chestManager.getInfiniteChests().get(chestLocation);
        if (data != null && data.getTrackedMaterial() != null) {
            inv.setItem(13, createTrackedItem(data));
        } else {
            inv.setItem(13, null);
        }
    }

    public void updateCenterSlot(Inventory inv, ChestData data) {
        if (data.getTrackedMaterial() != null) {
            inv.setItem(13, createTrackedItem(data));
        } else {
            inv.setItem(13, null);
        }
    }

    private ItemStack createTrackedItem(ChestData data) {
        ItemStack tracked = new ItemStack(data.getTrackedMaterial(), 1);
        ItemMeta meta = tracked.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + tracked.getI18NDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Guardado: " + ChatColor.YELLOW + data.getStoredAmount());
            lore.add("");
            lore.add(ChatColor.GREEN + "➤ Clique Esquerdo: Retirar 64");
            lore.add(ChatColor.GREEN + "➤ Clique Direito: Retirar 1");
            lore.add(ChatColor.GREEN + "➤ Shift + Clique Esquerdo: Retirar tudo possível");
            lore.add(ChatColor.AQUA + "➤ Shift + Clique em um item: Depositar no baú");
            meta.setLore(lore);
            tracked.setItemMeta(meta);
        }
        return tracked;
    }

    private void startInventoryUpdateTask(Player player, Inventory inventory, Location chestLocation) {
        if (inventoryUpdateTasks.containsKey(player))
            return;
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!openInventories.containsKey(inventory)) {
                cancelInventoryUpdateTask(player);
                return;
            }
            updateCenterSlot(inventory, chestLocation);
            player.updateInventory();
        }, 0L, 20L);
        inventoryUpdateTasks.put(player, taskId);
    }

    public void cancelInventoryUpdateTask(Player player) {
        if (inventoryUpdateTasks.containsKey(player)) {
            Bukkit.getScheduler().cancelTask(inventoryUpdateTasks.get(player));
            inventoryUpdateTasks.remove(player);
        }
    }

    public Map<Inventory, Location> getOpenInventories() {
        return openInventories;
    }
}