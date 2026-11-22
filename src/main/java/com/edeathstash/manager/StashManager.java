package com.edeathstash.manager;

import com.edeathstash.EDeathStash;
import com.edeathstash.util.ItemSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class StashManager {

    private final EDeathStash plugin;
    private final ConfigManager config;
    private final NamespacedKey keyStash;
    private final NamespacedKey keyData;

    public StashManager(EDeathStash plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.keyStash = new NamespacedKey(plugin, "death_stash");
        this.keyData = new NamespacedKey(plugin, "stash_data");
    }

    public ItemStack createStash(List<ItemStack> items) {
        List<ItemStack> limited = new ArrayList<>();
        int max = config.getMaxStashSlots();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            if (limited.size() >= max) break;
            limited.add(item.clone());
        }

        ItemStack chest = new ItemStack(config.getStashMaterial(), 1);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(config.getStashName());
            List<String> lore = new ArrayList<>(config.getStashLore());
            lore.add("");
            lore.add(org.bukkit.ChatColor.GRAY + "Stacks: " + org.bukkit.ChatColor.YELLOW + limited.size());
            meta.setLore(lore);
            if (config.getCustomModelData() > 0) {
                meta.setCustomModelData(config.getCustomModelData());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyStash, PersistentDataType.BYTE, (byte) 1);
            pdc.set(keyData, PersistentDataType.STRING, ItemSerializer.serialize(limited));
            chest.setItemMeta(meta);
        }
        return chest;
    }

    public boolean isStash(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyStash, PersistentDataType.BYTE);
    }

    public List<ItemStack> getContents(ItemStack item) {
        if (!isStash(item)) return new ArrayList<>();
        String data = item.getItemMeta().getPersistentDataContainer().get(keyData, PersistentDataType.STRING);
        return ItemSerializer.deserialize(data);
    }

    /**
     * Claim all items into player inventory. Returns [claimedStacks, droppedStacks].
     */
    public int[] claim(Player player, ItemStack stash) {
        List<ItemStack> contents = getContents(stash);
        int claimed = 0;
        int dropped = 0;
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) continue;
            var leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                claimed++;
            } else {
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                    dropped++;
                }
                if (leftover.size() < 1) {
                    claimed++;
                }
            }
        }
        // remove one stash from hand / inventory
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isStash(hand)) {
            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
        } else {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (isStash(off)) {
                if (off.getAmount() <= 1) {
                    player.getInventory().setItemInOffHand(null);
                } else {
                    off.setAmount(off.getAmount() - 1);
                }
            }
        }
        return new int[]{claimed, dropped};
    }

    public NamespacedKey getKeyStash() {
        return keyStash;
    }
}
