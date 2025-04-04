package com.sytmc.complaint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ComplaintToll extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private List<Complaint> complaints;

    @Override
    public void onEnable() {
        // Ensure the config file exists
        createConfigFile();

        // Load the config file
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        // Load stored complaints
        complaints = loadComplaints();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (config != null) {
            // Save complaints to config file
            config.set("complaints", complaints);
            try {
                config.save(new File(getDataFolder(), "config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createConfigFile() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
                defaultConfig.set("complaints", new ArrayList<>());
                defaultConfig.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Complaint> loadComplaints() {
        List<?> rawList = config.getList("complaints", new ArrayList<>());
        List<Complaint> complaintList = new ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Complaint) {
                    complaintList.add((Complaint) obj);
                }
            }
        }
        return complaintList;
    }

    // Complaint data model
    private static class Complaint {
        String targetPlayer;
        String reason;
        String reporter;
        String timestamp;

        public Complaint(String targetPlayer, String reason, String reporter, String timestamp) {
            this.targetPlayer = targetPlayer;
            this.reason = reason;
            this.reporter = reporter;
            this.timestamp = timestamp;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("complaint")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage("§6Usage: /complaint <player> <reason>");
                return true;
            }

            String target = args[0];
            String reason = String.join(" ", args).substring(target.length() + 1);

            // Create a new complaint
            Complaint complaint = new Complaint(
                    target,
                    reason,
                    player.getName(),
                    String.valueOf(System.currentTimeMillis())
            );

            complaints.add(complaint);
            player.sendMessage("§aSuccessfully submitted a complaint against player " + target + "!");
            return true;
        }
        return false;
    }

    // Admin join notification
    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("complaint.admin")) {
            if (!complaints.isEmpty()) {
                player.sendMessage("§c§lThere are unresolved player complaints:");
                for (Complaint complaint : complaints) {
                    player.sendMessage(String.format(
                            "§6[%s] §b%s §7complained about §b%s §7- Reason: §f%s",
                            complaint.timestamp,
                            complaint.reporter,
                            complaint.targetPlayer,
                            complaint.reason
                    ));
                }
            }
        }
    }
}