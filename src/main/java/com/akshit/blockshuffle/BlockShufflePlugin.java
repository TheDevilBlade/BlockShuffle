
package com.akshit.blockshuffle;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BlockShufflePlugin extends JavaPlugin implements Listener {
    private final Map<Player, Material> playerTargets = new HashMap<>();
    private final Map<Player, Boolean> playerCompleted = new HashMap<>();
    private final Random random = new Random();
    private BukkitRunnable roundTimer;
    private int roundTime = 300; // 5 minutes

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Block Shuffle Plugin enabled");
    }

    @Override
    public void onDisable() {
        if (roundTimer != null) roundTimer.cancel();
        getLogger().info("Block Shuffle Plugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "startshuffle":
                startGame();
                break;
            case "myblock":
                if (!playerTargets.containsKey(player)) {
                    player.sendMessage(ChatColor.RED + "You don't have a target block.");
                } else {
                    player.sendMessage(ChatColor.GOLD + "Your target block is: " + ChatColor.GREEN + playerTargets.get(player));
                }
                break;
        }
        return true;
    }

    private void startGame() {
        playerTargets.clear();
        playerCompleted.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Material target = getRandomBlock();
            playerTargets.put(p, target);
            playerCompleted.put(p, false);
            p.sendMessage(ChatColor.YELLOW + "Your target block is: " + ChatColor.GREEN + target);
            updateScoreboard(p);
        }

        if (roundTimer != null) roundTimer.cancel();

        roundTimer = new BukkitRunnable() {
            int timeLeft = roundTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    this.cancel();
                    eliminatePlayers();
                    startGame();
                    return;
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()); // clear scoreboard
                    updateScoreboard(p);
                }
                timeLeft--;
            }
        };
        roundTimer.runTaskTimer(this, 0, 20);
    }

    private Material getRandomBlock() {
        Material[] materials = Material.values();
        Material block;
        do {
            block = materials[random.nextInt(materials.length)];
        } while (!block.isBlock() || block == Material.BARRIER || block == Material.AIR);
        return block;
    }

    private void eliminatePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!playerCompleted.getOrDefault(p, false)) {
                p.sendMessage(ChatColor.RED + "You have been eliminated!");
                p.setGameMode(GameMode.SPECTATOR);
            } else {
                p.sendMessage(ChatColor.GREEN + "You survived the round!");
            }
        }
    }

    private void updateScoreboard(Player player) {
        // Optional: Implement a scoreboard with teams, points, timers
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!playerTargets.containsKey(player)) return;
        if (playerCompleted.getOrDefault(player, false)) return;

        Material blockBelow = player.getLocation().getBlock().getRelative(0, -1, 0).getType();
        if (blockBelow == playerTargets.get(player)) {
            playerCompleted.put(player, true);
            player.sendMessage(ChatColor.GREEN + "You've found your block: " + blockBelow);
        }
    }
}
