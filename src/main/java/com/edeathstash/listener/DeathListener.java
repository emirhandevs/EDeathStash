package com.edeathstash.listener;

import com.edeathstash.EDeathStash;
import com.edeathstash.manager.ConfigManager;
import com.edeathstash.manager.StashManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final EDeathStash plugin;
    private final ConfigManager config;
    private final StashManager stashManager;

    public DeathListener(EDeathStash plugin, ConfigManager config, StashManager stashManager) {
        this.plugin = plugin;
        this.config = config;
        this.stashManager = stashManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (!killer.hasPermission("edeathstash.use")) return;
        if (!config.isWorldEnabled(victim.getWorld().getName())) return;

        boolean isPlayer = victim instanceof Player;
        if (isPlayer && !config.isCollectPlayerKills()) return;
        if (!isPlayer && !config.isCollectMobKills()) return;

        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && !drop.getType().isAir()) {
                drops.add(drop.clone());
            }
        }

        if (drops.isEmpty() && config.isSkipEmpty()) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(event.getDroppedExp()); // keep xp normal

        if (!config.isDropStashItem()) {
            return;
        }

        ItemStack stash = stashManager.createStash(drops);
        victim.getWorld().dropItemNaturally(victim.getLocation(), stash);

        if (config.msg("stash-created") != null && !config.msg("stash-created").isEmpty()) {
            killer.sendMessage(config.msg("stash-created").replace("%count%", String.valueOf(drops.size())));
        }
    }
}
