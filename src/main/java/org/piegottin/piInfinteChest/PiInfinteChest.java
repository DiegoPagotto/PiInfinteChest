package org.piegottin.piInfinteChest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.domain.ChestData;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PiInfinteChest extends JavaPlugin implements Listener {

    private final Map<Location, ChestData> infiniteChests = new HashMap<>();
    private final Map<Inventory, Location> openInventories = new HashMap<>();
    private final Map<Player, Integer> inventoryUpdateTasks = new HashMap<>();


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadChests();
        registerInfiniteChestRecipeIfNeeded();
        getLogger().info("PiInfinteChest has been enabled!");
    }

    @Override
    public void onDisable() {
        saveChests();
        getLogger().info("PiInfinteChest has been disabled!");
    }


    private File chestFile;
    private FileConfiguration chestConfig;

    private void saveChests() {
        chestFile = new File(getDataFolder(), "chests.yml");
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);

        for (Map.Entry<Location, ChestData> entry : infiniteChests.entrySet()) {
            Location loc = entry.getKey();
            ChestData data = entry.getValue();

            String path = "chests." + loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            chestConfig.set(path + ".material", data.getTrackedMaterial() != null ? data.getTrackedMaterial().name() : null);
            chestConfig.set(path + ".count", data.getStoredAmount());
        }

        try {
            chestConfig.save(chestFile);
        } catch (IOException e) {
            getLogger().severe("Could not save chests.yml!");
            e.printStackTrace();
        }
    }

    private void loadChests() {
        chestFile = new File(getDataFolder(), "chests.yml");
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);

        if (!chestConfig.contains("chests")) return;

        for (String key : chestConfig.getConfigurationSection("chests").getKeys(false)) {
            String[] parts = key.split(",");
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
            Material material = chestConfig.getString("chests." + key + ".material") != null
                    ? Material.valueOf(chestConfig.getString("chests." + key + ".material"))
                    : null;
            int count = chestConfig.getInt("chests." + key + ".count");

            ChestData data = new ChestData();
            if (material != null) {
                data.addItems(material, count);
            }

            infiniteChests.put(loc, data);
        }
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
                    ChatColor.GRAY + "Coloque no chão para",
                    ChatColor.GRAY + "obter um baú infinito."
            ));
            chestItem.setItemMeta(meta);
        }
        player.getInventory().addItem(chestItem);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
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

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Guardado: " + ChatColor.YELLOW + data.getStoredAmount());
                lore.add("");
                lore.add(ChatColor.GREEN + "➤ Clique Esquerdo: Retirar 64");
                lore.add(ChatColor.GREEN + "➤ Clique Direito: Retirar 1");
                lore.add(ChatColor.GREEN + "➤ Shift + Clique Esquerdo: Retirar tudo possível");
                lore.add(ChatColor.AQUA + "➤ Shift + Clique em um item: Depositar no baú");

                trackedMeta.setLore(lore);
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

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getRawSlot();

        Location chestLoc = openInventories.get(view.getTopInventory());
        if (chestLoc == null) return;
        ChestData data = infiniteChests.get(chestLoc);
        if (data == null) return;

        // Interaction with the Infinite Chest (Top inventory)
        if (clickedInventory == view.getTopInventory()) {
            event.setCancelled(true);

            if (slot == 13) { // Center slot (Chest logic)
                ItemStack cursorItem = event.getCursor(); // Item the player is holding

                // Deposit Logic (Click & drag an item into the chest slot)
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    Material cursorMaterial = cursorItem.getType();
                    if (data.getTrackedMaterial() == null) {
                        // Chest starts tracking this material
                        data.addItems(cursorMaterial, cursorItem.getAmount());
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    } else if (data.getTrackedMaterial() == cursorMaterial) {
                        // Add items to the chest if it matches the tracked type
                        data.addItems(cursorMaterial, cursorItem.getAmount());
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    } else {
                        player.sendMessage(ChatColor.RED + "This chest is already tracking " + data.getTrackedMaterial().name());
                    }
                }
                // Withdraw Logic
                else {
                    Material trackedMaterial = data.getTrackedMaterial();
                    if (trackedMaterial == null) return; // No item is tracked yet

                    int withdrawAmount = 0;
                    if (event.isShiftClick() && event.isLeftClick()) {
                        // Shift + Left Click → Take as much as possible until inventory is full
                        withdrawAmount = data.getStoredAmount();
                    } else if (event.isLeftClick()) {
                        // Left Click → Withdraw a full stack (or max available)
                        withdrawAmount = Math.min(64, data.getStoredAmount());
                    } else if (event.isRightClick()) {
                        // Right Click → Withdraw only 1 item
                        withdrawAmount = Math.min(1, data.getStoredAmount());
                    }

                    if (withdrawAmount > 0) {
                        ItemStack withdrawStack = new ItemStack(trackedMaterial, withdrawAmount);
                        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(withdrawStack);

                        if (leftovers.isEmpty()) {
                            data.removeItems(withdrawAmount);
                        } else {
                            // Only remove the items successfully added to inventory
                            int actuallyWithdrawn = withdrawAmount - leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                            data.removeItems(actuallyWithdrawn);
                        }
                    }
                }

                // Update the GUI slot to reflect changes
                updateCenterSlot(view.getTopInventory(), data);
            }
        }
        // Handling shift-click deposit (From player inventory)
        else if (clickedInventory == player.getInventory() && event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            Material clickedMaterial = clickedItem.getType();
            if (data.getTrackedMaterial() == null) {
                // First-time deposit sets the chest type
                data.addItems(clickedMaterial, clickedItem.getAmount());
                clickedInventory.setItem(event.getSlot(), null); // Remove item from inventory
            } else if (data.getTrackedMaterial() == clickedMaterial) {
                // Deposit matching items into the Infinite Chest
                data.addItems(clickedMaterial, clickedItem.getAmount());
                clickedInventory.setItem(event.getSlot(), null);
            } else {
                player.sendMessage(ChatColor.RED + "This chest is already tracking " + data.getTrackedMaterial().name());
            }

            // Update GUI to reflect changes
            updateCenterSlot(view.getTopInventory(), data);
            event.setCancelled(true);
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
                } else {
                    event.setCancelled(true);
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
            inv.setItem(13, tracked);
        }
    }

    private void registerInfiniteChestRecipeIfNeeded() {
        ItemStack infiniteChest = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = infiniteChest.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Infinite Chest");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Coloque no chão para",
                    ChatColor.GRAY + "obter um baú infinito."
            ));
            infiniteChest.setItemMeta(meta);
        }

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "infinite_chest"), infiniteChest);
        recipe.shape(
                "GDG",
                "DCD",
                "GDG"
        );

        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('C', Material.CHEST);

        if(Bukkit.getRecipe(new NamespacedKey(this, "infinite_chest")) == null) {
            Bukkit.addRecipe(recipe);
            Bukkit.getLogger().info("Infinite Chest recipe registered!");
        }
    }
}