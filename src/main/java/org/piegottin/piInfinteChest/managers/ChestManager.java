package org.piegottin.piInfinteChest.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.piegottin.piInfinteChest.domain.ChestData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChestManager {
    private final Map<Location, ChestData> infiniteChests = new HashMap<>();
    private final JavaPlugin plugin;
    private final File chestFile;
    private FileConfiguration chestConfig;
    private static final String CHESTS_CONFIG_PATH = "chests";

    public ChestManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.chestFile = new File(plugin.getDataFolder(), "chests.yml");
        this.chestConfig = YamlConfiguration.loadConfiguration(chestFile);

        Bukkit.getScheduler().runTaskTimer(plugin, this::saveChests, 0L, 6000L);
    }

    public Map<Location, ChestData> getInfiniteChests() {
        return infiniteChests;
    }

    public void saveChests() {
        for (Map.Entry<Location, ChestData> entry : infiniteChests.entrySet()) {
            Location loc = entry.getKey();
            ChestData data = entry.getValue();
            String path = buildPathFromLocation(loc);
            chestConfig.set(path + ".material", data.getTrackedMaterial() != null ? data.getTrackedMaterial().name() : null);
            chestConfig.set(path + ".count", data.getStoredAmount());
        }
        try {
            chestConfig.save(chestFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chests.yml!");
            e.printStackTrace();
        }
    }

    public void loadChests() {
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);
        if (!chestConfig.contains(CHESTS_CONFIG_PATH))
            return;
        for (String key : chestConfig.getConfigurationSection(CHESTS_CONFIG_PATH).getKeys(false)) {
            Location loc = parseLocationFromKey(key);
            String materialString = chestConfig.getString(CHESTS_CONFIG_PATH + "." + key + ".material");
            Material material = (materialString != null) ? Material.valueOf(materialString) : null;
            int count = chestConfig.getInt(CHESTS_CONFIG_PATH + "." + key + ".count");

            ChestData data = new ChestData();
            if (material != null) {
                data.addItems(material, count);
            }
            infiniteChests.put(loc, data);
        }
    }

    private String buildPathFromLocation(Location loc) {
        return CHESTS_CONFIG_PATH + "." + loc.getWorld().getName() + "," +
                loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location parseLocationFromKey(String key) {
        String[] parts = key.split(",");
        String worldName = parts[0];
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }
}
