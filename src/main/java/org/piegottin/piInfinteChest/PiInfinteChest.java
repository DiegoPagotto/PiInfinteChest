package org.piegottin.piInfinteChest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.domain.ChestData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class PiInfinteChest extends JavaPlugin implements Listener {

    private final Map<Location, ChestData> infiniteChests = new HashMap<>();
    private final Map<Inventory, Location> openInventories = new HashMap<>();
    private final Map<Player, Integer> inventoryUpdateTasks = new HashMap<>();


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PiInfinteChest has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PiInfinteChest has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("infinitechest")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                giveInfiniteChestItem(player);
                player.sendMessage(ChatColor.GREEN + "You have received an Infinite Chest!");
            } else {
                sender.sendMessage("This command can only be used by players.");
            }
            return true;
        }
        return false;
    }

    private void giveInfiniteChestItem(Player player) {
        ItemStack chestItem = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = chestItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Infinite Chest");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Place to open its GUI and set",
                    ChatColor.GRAY + "an item to be tracked infinitely."
            ));
            chestItem.setItemMeta(meta);
        }
        player.getInventory().addItem(chestItem);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Infinite Chest")) {
            Block block = event.getBlock();
            if (block.getType() == Material.CHEST) {
                infiniteChests.put(block.getLocation(), new ChestData());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Infinite Chest placed!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location loc = event.getClickedBlock().getLocation();
            if (infiniteChests.containsKey(loc)) {
                event.setCancelled(true);
                openInfiniteChestGUI(event.getPlayer(), loc);
            }
        }
    }

    private void openInfiniteChestGUI(Player player, Location chestLocation) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Infinite Chest");

        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta phMeta = placeholder.getItemMeta();
        if (phMeta != null) {
            phMeta.setDisplayName(" ");
            placeholder.setItemMeta(phMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, placeholder);
        }

        updateCenterSlot(inv, chestLocation);

        openInventories.put(inv, chestLocation);
        player.openInventory(inv);

        startInventoryUpdateTask(player, inv, chestLocation);
    }

    private void updateCenterSlot(Inventory inv, Location chestLocation) {
        ChestData data = infiniteChests.get(chestLocation);
        if (data != null && data.getTrackedMaterial() != null) {
            ItemStack tracked = new ItemStack(data.getTrackedMaterial(), 1);
            ItemMeta trackedMeta = tracked.getItemMeta();
            if (trackedMeta != null) {
                trackedMeta.setDisplayName(ChatColor.GOLD + data.getTrackedMaterial().name());
                trackedMeta.setLore(Arrays.asList(ChatColor.GRAY + "Stored: " + data.getCount()));
                tracked.setItemMeta(trackedMeta);
            }
            inv.setItem(13, tracked);
        } else {
            inv.setItem(13, null);
        }
    }

    private void startInventoryUpdateTask(Player player, Inventory inventory, Location chestLocation) {
        if (inventoryUpdateTasks.containsKey(player))
            return;

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!openInventories.containsKey(inventory)) {
                Bukkit.getScheduler().cancelTask(inventoryUpdateTasks.get(player));
                inventoryUpdateTasks.remove(player);
                return;
            }

            updateCenterSlot(inventory, chestLocation);
            player.updateInventory();
        }, 0L, 20L);

        inventoryUpdateTasks.put(player, taskId);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        if (openInventories.containsKey(inv)) {
            openInventories.remove(inv);
        }

        if (inventoryUpdateTasks.containsKey(player)) {
            Bukkit.getScheduler().cancelTask(inventoryUpdateTasks.get(player));
            inventoryUpdateTasks.remove(player);
        }
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(ChatColor.DARK_GRAY + "Infinite Chest")) {
            return;
        }
        if (event.getClickedInventory() == view.getTopInventory()) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 13) {
                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    Location chestLoc = openInventories.get(view.getTopInventory());
                    if (chestLoc == null) return;
                    ChestData data = infiniteChests.get(chestLoc);
                    if (data != null) {
                        if (data.getTrackedMaterial() == null) {
                            data.addItems(cursorItem.getType(), cursorItem.getAmount());
                            event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
                        } else {
                            if (data.getTrackedMaterial() == cursorItem.getType()) {
                                data.addItems(cursorItem.getType(), cursorItem.getAmount());
                                event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
                            } else {
                                event.getWhoClicked().sendMessage(ChatColor.RED + "This chest is already tracking "
                                        + data.getTrackedMaterial().name());
                            }
                        }
                        updateCenterSlot(view.getTopInventory(), data);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Infinite Chest")) {
            if (event.getInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof Chest) {
            Chest destChest = (Chest) event.getDestination().getHolder();
            Location loc = destChest.getLocation();
            if (infiniteChests.containsKey(loc)) {
                ChestData data = infiniteChests.get(loc);
                ItemStack movingItem = event.getItem();
                if (data.getTrackedMaterial() != null && movingItem.getType() == data.getTrackedMaterial()) {
                    Bukkit.getLogger().info("Adding " + movingItem.getI18NDisplayName() + " x" + movingItem.getAmount());

                    data.addItems(movingItem.getType(), movingItem.getAmount());
                    event.getSource().removeItem(new ItemStack(movingItem.getType(), movingItem.getAmount()));
                    event.getDestination().clear();
                }
            }
        }
    }

    private void updateCenterSlot(Inventory inv, ChestData data) {
        if (data.getTrackedMaterial() != null) {
            ItemStack tracked = new ItemStack(data.getTrackedMaterial(), 1);
            ItemMeta meta = tracked.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + data.getTrackedMaterial().name());
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Stored: " + data.getCount()));
                tracked.setItemMeta(meta);
            }
            inv.setItem(13, tracked);
        }
    }
}