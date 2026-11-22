package com.edeathstash.listener;

import com.edeathstash.EDeathStash;
import com.edeathstash.manager.ConfigManager;
import com.edeathstash.manager.StashManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StashInteractListener implements Listener {

    private final EDeathStash plugin;
    private final ConfigManager config;
    private final StashManager stashManager;
    private final Set<UUID> previewViewers = new HashSet<>();

    public StashInteractListener(EDeathStash plugin, ConfigManager config, StashManager stashManager) {
        this.plugin = plugin;
        this.config = config;
        this.stashManager = stashManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !stashManager.isStash(item)) {
            return;
        }

        Action action = event.getAction();
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        if (!left && !right) return;

        event.setCancelled(true);

        if (left) {
            openPreview(player, item);
            return;
        }

        // Right-click: claim
        List<ItemStack> contents = stashManager.getContents(item);
        if (contents.isEmpty()) {
            player.sendMessage(config.msg("empty-stash"));
            // still remove empty stash
            removeOne(player, item, event.getHand());
            return;
        }

        int[] result = claimIntoInventory(player, contents);
        removeOne(player, item, event.getHand());

        if (result[1] > 0) {
            player.sendMessage(config.msg("claimed-partial")
                    .replace("%claimed%", String.valueOf(result[0]))
                    .replace("%dropped%", String.valueOf(result[1])));
        } else {
            player.sendMessage(config.msg("claimed").replace("%count%", String.valueOf(result[0])));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (stashManager.isStash(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(config.msg("cannot-place"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!previewViewers.contains(player.getUniqueId())) return;
        String title = event.getView().getTitle();
        if (title != null && title.contains("Death Stash")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!previewViewers.contains(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            previewViewers.remove(player.getUniqueId());
        }
    }

    private void openPreview(Player player, ItemStack stash) {
        List<ItemStack> contents = stashManager.getContents(stash);
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, config.msg("preview-title"));
        int slot = 0;
        for (ItemStack stack : contents) {
            if (slot >= size) break;
            if (stack != null) {
                inv.setItem(slot++, stack.clone());
            }
        }
        previewViewers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    private int[] claimIntoInventory(Player player, List<ItemStack> contents) {
        int claimed = 0;
        int dropped = 0;
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) continue;
            var leftover = player.getInventory().addItem(stack.clone());
            if (leftover.isEmpty()) {
                claimed++;
            } else {
                boolean anyAdded = leftover.values().stream().anyMatch(i -> i.getAmount() < stack.getAmount());
                if (anyAdded || leftover.size() < stack.getAmount()) {
                    // partial
                }
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                    dropped++;
                }
                if (leftover.isEmpty()) {
                    claimed++;
                } else {
                    // count as partial claim of original stack
                    claimed++;
                }
            }
        }
        return new int[]{claimed, dropped};
    }

    private void removeOne(Player player, ItemStack item, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (stashManager.isStash(off)) {
                if (off.getAmount() <= 1) player.getInventory().setItemInOffHand(null);
                else off.setAmount(off.getAmount() - 1);
            }
            return;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (stashManager.isStash(main)) {
            if (main.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
            else main.setAmount(main.getAmount() - 1);
        }
    }
}
