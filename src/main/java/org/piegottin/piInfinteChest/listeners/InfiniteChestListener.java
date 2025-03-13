package org.piegottin.piInfinteChest.listeners;

import org.bukkit.*;
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
import org.piegottin.piInfinteChest.domain.ChestData;
import org.piegottin.piInfinteChest.gui.GUIManager;
import org.piegottin.piInfinteChest.managers.ChestManager;

import java.util.HashMap;

import static org.piegottin.piInfinteChest.utils.InfiniteChestUtils.*;

public class InfiniteChestListener implements Listener {
    private static final String INFINITE_CHEST_TITLE = ChatColor.GOLD + "Baú Infinito";
    private final ChestManager chestManager;
    private final GUIManager guiManager;

    public InfiniteChestListener(ChestManager chestManager, GUIManager guiManager) {
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
                event.getPlayer().sendMessage(ChatColor.GREEN + "Baú infinito colocado!");

                Location loc = block.getLocation();
                World world = loc.getWorld();

                world.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.YELLOW, 2.0f);
                world.spawnParticle(Particle.REDSTONE, loc.add(0.5, 1.2, 0.5),
                        50,
                        1.0, 1.0, 1.0,
                        0.1, dustOptions);

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        if (event.getClickedBlock().getType() == Material.CHEST) {
            Location loc = event.getClickedBlock().getLocation();
            if (chestManager.getInfiniteChests().containsKey(loc)) {
                Player player = event.getPlayer();

                if (isTryingToPlaceBlockAdjacentToChest(event, player)) return;

                event.setCancelled(true);
                guiManager.openInfiniteChestGUI(player, loc);
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
            } else if (slot == 26) {
                if (data.getStoredAmount() > 0) {
                    player.sendMessage(ChatColor.RED + "Você não pode remover o baú enquanto ele contém itens.");
                } else {
                    chestManager.getInfiniteChests().remove(chestLoc);
                    player.closeInventory();
                    player.getInventory().addItem(guiManager.getInfiniteChestItem());
                    player.sendMessage(ChatColor.GREEN + "Baú infinito removido!");

                    chestLoc.getBlock().setType(Material.AIR);


                    World world = chestLoc.getWorld();

                    world.playSound(chestLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    world.spawnParticle(Particle.SMOKE_NORMAL, chestLoc.add(0.5, 1.0, 0.5),
                            50,
                            1.0, 1.0, 1.0,
                            0.05);

                }
            }
        } else if (clickedInventory == player.getInventory() && event.isShiftClick()) {
            handlePlayerShiftClickDeposit(player, data, clickedInventory, event.getSlot(), view, guiManager);
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


}