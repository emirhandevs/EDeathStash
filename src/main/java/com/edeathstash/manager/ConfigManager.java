package com.edeathstash.manager;

import com.edeathstash.EDeathStash;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConfigManager {

    private final EDeathStash plugin;
    private FileConfiguration cfg;

    private boolean collectPlayerKills;
    private boolean collectMobKills;
    private int maxStashSlots;
    private boolean skipEmpty;
    private boolean dropStashItem;
    private Material stashMaterial;
    private String stashName;
    private List<String> stashLore;
    private int customModelData;
    private final Set<String> enabledWorlds = new HashSet<>();
    private final Set<String> disabledWorlds = new HashSet<>();
    private String prefix;

    public ConfigManager(EDeathStash plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
        collectPlayerKills = cfg.getBoolean("collect-player-kills", true);
        collectMobKills = cfg.getBoolean("collect-mob-kills", true);
        maxStashSlots = Math.max(1, Math.min(54, cfg.getInt("max-stash-slots", 54)));
        skipEmpty = cfg.getBoolean("skip-empty-stashes", true);
        dropStashItem = cfg.getBoolean("drop-stash-item", true);

        String mat = cfg.getString("stash-item.material", "CHEST");
        try {
            stashMaterial = Material.valueOf(mat.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            stashMaterial = Material.CHEST;
        }
        stashName = color(cfg.getString("stash-item.name", "&6Death Stash"));
        stashLore = new ArrayList<>();
        for (String line : cfg.getStringList("stash-item.lore")) {
            stashLore.add(color(line));
        }
        customModelData = cfg.getInt("stash-item.custom-model-data", 0);

        enabledWorlds.clear();
        for (String w : cfg.getStringList("enabled-worlds")) {
            enabledWorlds.add(w.toLowerCase(Locale.ROOT));
        }
        disabledWorlds.clear();
        for (String w : cfg.getStringList("disabled-worlds")) {
            disabledWorlds.add(w.toLowerCase(Locale.ROOT));
        }
        prefix = color(cfg.getString("messages.prefix", "&8[&6eDeathStash&8] &f"));
    }

    public boolean isWorldEnabled(String world) {
        String k = world.toLowerCase(Locale.ROOT);
        if (disabledWorlds.contains(k)) return false;
        if (enabledWorlds.isEmpty()) return true;
        return enabledWorlds.contains(k);
    }

    public String msg(String path) {
        return color(cfg.getString("messages." + path, path).replace("%prefix%", prefix));
    }

    public List<String> msgList(String path) {
        List<String> out = new ArrayList<>();
        for (String line : cfg.getStringList("messages." + path)) {
            out.add(color(line.replace("%prefix%", prefix)));
        }
        return out;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public boolean isCollectPlayerKills() { return collectPlayerKills; }
    public boolean isCollectMobKills() { return collectMobKills; }
    public int getMaxStashSlots() { return maxStashSlots; }
    public boolean isSkipEmpty() { return skipEmpty; }
    public boolean isDropStashItem() { return dropStashItem; }
    public Material getStashMaterial() { return stashMaterial; }
    public String getStashName() { return stashName; }
    public List<String> getStashLore() { return stashLore; }
    public int getCustomModelData() { return customModelData; }
    public String getPrefix() { return prefix; }
}
