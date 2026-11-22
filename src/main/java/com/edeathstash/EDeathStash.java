package com.edeathstash;

import com.edeathstash.listener.DeathListener;
import com.edeathstash.listener.StashInteractListener;
import com.edeathstash.manager.ConfigManager;
import com.edeathstash.manager.StashManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class EDeathStash extends JavaPlugin {

    private ConfigManager configManager;
    private StashManager stashManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.reload();
        stashManager = new StashManager(this, configManager);

        getServer().getPluginManager().registerEvents(
                new DeathListener(this, configManager, stashManager), this);
        getServer().getPluginManager().registerEvents(
                new StashInteractListener(this, configManager, stashManager), this);

        getLogger().info("eDeathStash enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("eDeathStash disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (name.equals("stash")) {
            if (!sender.hasPermission("edeathstash.use")) {
                sender.sendMessage(configManager.msg("no-permission"));
                return true;
            }
            List<String> help = configManager.msgList("stash-help");
            if (help.isEmpty()) {
                sender.sendMessage(configManager.getPrefix() + "Kill mobs/players to collect drops into a Death Stash chest.");
            } else {
                for (String line : help) {
                    sender.sendMessage(line);
                }
            }
            return true;
        }

        if (name.equals("edeathstash")) {
            if (!sender.hasPermission("edeathstash.admin")) {
                sender.sendMessage(configManager.msg("no-permission"));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(configManager.getPrefix() + "Usage: /edeathstash <reload|give> [player]");
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    reloadConfig();
                    configManager.reload();
                    sender.sendMessage(configManager.msg("reloaded"));
                }
                case "give" -> {
                    Player target;
                    if (args.length >= 2) {
                        target = Bukkit.getPlayerExact(args[1]);
                    } else if (sender instanceof Player p) {
                        target = p;
                    } else {
                        sender.sendMessage(configManager.msg("invalid-player"));
                        return true;
                    }
                    if (target == null) {
                        sender.sendMessage(configManager.msg("invalid-player"));
                        return true;
                    }
                    target.getInventory().addItem(stashManager.createStash(List.of()));
                    sender.sendMessage(configManager.msg("give-success").replace("%player%", target.getName()));
                }
                default -> sender.sendMessage(configManager.getPrefix() + "Usage: /edeathstash <reload|give> [player]");
            }
            return true;
        }
        return false;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StashManager getStashManager() {
        return stashManager;
    }
}
