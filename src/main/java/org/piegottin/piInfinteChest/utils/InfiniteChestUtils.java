package org.piegottin.piInfinteChest.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.piegottin.piInfinteChest.domain.ChestData;
import org.piegottin.piInfinteChest.gui.GUIManager;

import java.util.HashMap;

public class InfiniteChestUtils {
    public static void handleDeposit(Player player, ChestData data, InventoryClickEvent event) {
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            Material cursorMaterial = cursorItem.getType();
            if (data.getTrackedMaterial() == null || data.getTrackedMaterial() == cursorMaterial) {
                data.addItems(cursorMaterial, cursorItem.getAmount());
                player.setItemOnCursor(new ItemStack(Material.AIR));
            } else {
                ItemStack stack = new ItemStack(cursorMaterial, cursorItem.getAmount());
                player.sendMessage(ChatColor.RED + "Este baú já está guardando " + stack.getI18NDisplayName());
            }
        }
    }

    public static void handleWithdrawal(Player player, ChestData data, InventoryClickEvent event) {
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

    public static void handlePlayerShiftClickDeposit(Player player, ChestData data, org.bukkit.inventory.Inventory inventory, int slot, InventoryView view, GUIManager guiManager) {
        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR)
            return;
        Material clickedMaterial = clickedItem.getType();
        if (data.getTrackedMaterial() == null || data.getTrackedMaterial() == clickedMaterial) {
            data.addItems(clickedMaterial, clickedItem.getAmount());
            inventory.setItem(slot, null);
        } else {
            player.sendMessage(ChatColor.RED + "Este baú já está guardando " + clickedItem.getI18NDisplayName());
        }
        guiManager.updateCenterSlot(view.getTopInventory(), data);
    }

    public static boolean isInfiniteChestItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
            return false;
        return ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Baú Infinito");
    }

    public static boolean isTryingToPlaceBlockAdjacentToChest(PlayerInteractEvent event, Player player) {
        boolean isHoldingBlock = event.getItem() != null && event.getItem().getType().isBlock();

        return isHoldingBlock &&
               event.getAction() == Action.RIGHT_CLICK_BLOCK &&
               player.isSneaking();

    }
}
