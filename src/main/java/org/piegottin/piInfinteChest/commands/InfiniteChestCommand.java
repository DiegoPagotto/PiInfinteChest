package org.piegottin.piInfinteChest.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.piegottin.piInfinteChest.gui.GUIManager;

public class InfiniteChestCommand implements CommandExecutor {
    private final GUIManager guiManager;

    public InfiniteChestCommand(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("infinitechest")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(guiManager.getInfiniteChestItem());
                player.sendMessage(ChatColor.GREEN + "You have received an Infinite Chest!");
            } else {
                sender.sendMessage("This command can only be used by players.");
            }
            return true;
        }
        return false;
    }
}