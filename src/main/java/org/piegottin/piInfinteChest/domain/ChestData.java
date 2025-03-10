package org.piegottin.piInfinteChest.domain;

import org.bukkit.Material;

public class ChestData {
    private Material trackedMaterial;
    private int count;

    public Material getTrackedMaterial() {
        return trackedMaterial;
    }

    public void setTrackedMaterial(Material trackedMaterial) {
        this.trackedMaterial = trackedMaterial;
    }

    public int getCount() {
        return count;
    }

    public void addItems(Material material, int amount) {
        if (trackedMaterial == null) {
            trackedMaterial = material;
        }
        count += amount;
    }
}
