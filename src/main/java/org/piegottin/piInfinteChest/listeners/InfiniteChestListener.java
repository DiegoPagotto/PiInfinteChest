package org.piegottin.piInfinteChest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.domain.ChestData;
import org.piegottin.piInfinteChest.gui.GUIManager;
import org.piegottin.piInfinteChest.managers.ChestManager;

import java.util.HashMap;

public class InfiniteChestListener implements Listener {
    private static final String INFINITE_CHEST_NAME = ChatColor.GOLD + "Infinite Chest";
    private static final String INFINITE_CHEST_TITLE = ChatColor.DARK_GRAY + "Infinite Chest";
    private final JavaPlugin plugin;
    private final ChestManager chestManager;
    private final GUIManager guiManager;

    public InfiniteChestListener(JavaPlugin plugin, ChestManager chestManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isInfiniteChestItem(item)) {
            Block block = event.getBlock();
            if (block.getType() == Material.CHEST) {
                chestManager.getInfiniteChests().put(block.getLocation(), new ChestData());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Infinite Chest placed!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location loc = event.getClickedBlock().getLocation();
            if (chestManager.getInfiniteChests().containsKey(loc)) {
                event.setCancelled(true);
                guiManager.openInfiniteChestGUI(event.getPlayer(), loc);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(INFINITE_CHEST_TITLE))
            return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getRawSlot();
        Location chestLoc = guiManager.getOpenInventories().get(view.getTopInventory());
        if (chestLoc == null)
            return;
        ChestData data = chestManager.getInfiniteChests().get(chestLoc);
        if (data == null)
            return;

        if (clickedInventory == view.getTopInventory()) {
            event.setCancelled(true);
            if (slot == 13) {
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    handleDeposit(player, data, event);
                } else {
                    handleWithdrawal(player, data, event);
                }
                guiManager.updateCenterSlot(view.getTopInventory(), data);
            }
        } else if (clickedInventory == player.getInventory() && event.isShiftClick()) {
            handlePlayerShiftClickDeposit(player, data, clickedInventory, event.getSlot(), view);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(INFINITE_CHEST_TITLE)
                && event.getInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof Chest) {
            Chest destChest = (Chest) event.getDestination().getHolder();
            Location loc = destChest.getLocation();
            if (chestManager.getInfiniteChests().containsKey(loc)) {
                ChestData data = chestManager.getInfiniteChests().get(loc);
                ItemStack movingItem = event.getItem();
                if (data.getTrackedMaterial() != null && movingItem.getType() == data.getTrackedMaterial()) {
                    Bukkit.getLogger().info("Adding " + movingItem.getI18NDisplayName() + " x" + movingItem.getAmount());
                    data.addItems(movingItem.getType(), movingItem.getAmount());
                    event.getSource().removeItem(new ItemStack(movingItem.getType(), movingItem.getAmount()));
                    event.getDestination().clear();
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();
        guiManager.getOpenInventories().remove(inv);
        guiManager.cancelInventoryUpdateTask(player);
    }

    private void handleDeposit(Player player, ChestData data, InventoryClickEvent event) {
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            Material cursorMaterial = cursorItem.getType();
            if (data.getTrackedMaterial() == null || data.getTrackedMaterial() == cursorMaterial) {
                data.addItems(cursorMaterial, cursorItem.getAmount());
                player.setItemOnCursor(new ItemStack(Material.AIR));
            } else {
                player.sendMessage(ChatColor.RED + "This chest is already tracking " + data.getTrackedMaterial().name());
            }
        }
    }

    private void handleWithdrawal(Player player, ChestData data, InventoryClickEvent event) {
        Material trackedMaterial = data.getTrackedMaterial();
        if (trackedMaterial == null)
            return;
        int withdrawAmount = 0;
        if (event.isShiftClick() && event.isLeftClick()) {
            withdrawAmount = data.getStoredAmount();
        } else if (event.isLeftClick()) {
            withdrawAmount = Math.min(64, data.getStoredAmount());
        } else if (event.isRightClick()) {
            withdrawAmount = Math.min(1, data.getStoredAmount());
        }
        if (withdrawAmount > 0) {
            ItemStack withdrawStack = new ItemStack(trackedMaterial, withdrawAmount);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(withdrawStack);
            if (leftovers.isEmpty()) {
                data.removeItems(withdrawAmount);
            } else {
                int actuallyWithdrawn = withdrawAmount - leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                data.removeItems(actuallyWithdrawn);
            }
        }
    }

    private void handlePlayerShiftClickDeposit(Player player, ChestData data, org.bukkit.inventory.Inventory inventory, int slot, InventoryView view) {
        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR)
            return;
        Material clickedMaterial = clickedItem.getType();
        if (data.getTrackedMaterial() == null || data.getTrackedMaterial() == clickedMaterial) {
            data.addItems(clickedMaterial, clickedItem.getAmount());
            inventory.setItem(slot, null);
        } else {
            player.sendMessage(ChatColor.RED + "This chest is already tracking " + data.getTrackedMaterial().name());
        }
        guiManager.updateCenterSlot(view.getTopInventory(), data);
    }

    private boolean isInfiniteChestItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
            return false;
        return ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Infinite Chest");
    }
}