package io.starlightmc.adminmode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class AdminModePlugin extends JavaPlugin implements Listener {
    private final List<Player> adminModeEnabled = new ArrayList<>();
    private final Map<Player, PermissionAttachment> attachments = new HashMap<>();

    private final List<LogEntry> logs = new ArrayList<>();

    private Path logsPath;

    @Override
    public void onEnable() {
        logsPath = getDataFolder().toPath().resolve("logs.txt");
        try {
            Files.createFile(logsPath);
        } catch (IOException e) {
            getLogger().warning("Cannot save logs path! Commands wont be logged.");
        }
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        adminModeEnabled.forEach(player -> {
            player.removeAttachment(attachments.get(player));
        });
        saveLogs();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("adminmode")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be executed in-game.");
            return true;
        }
        if (!sender.hasPermission("adminmode.toggle")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        String reason = String.join(" ", args);
        toggleAdminMode((Player) sender, reason);
        return true;
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isAdminMode(player)) {
            String command = event.getMessage();
            if (getConfig().getBoolean("log-commands")) {
                logEvent(player, "command", command);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (getConfig().getBoolean("log-creative-inventory-actions")) {
            logEvent(player, "creative_inventory", event.getCursor().getType().toString());
        }
    }

    public void toggleAdminMode(Player player, String reason) {
        boolean newValue = !isAdminMode(player);

        boolean announceEnter = getConfig().getBoolean("announce-enter");
        boolean announceLeave = getConfig().getBoolean("announce-leave");

        if (newValue) {
            if (announceEnter) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("announce-enter-message")
                        .replace("{name}", player.getName())
                        .replace("{reason}", reason)));
            }
            adminModeEnabled.add(player);
            PermissionAttachment attachment = player.addAttachment(this);
            getAdminModePermissions().forEach(perm -> attachment.setPermission(perm, true));
            attachments.put(player, attachment);
        } else {
            if (announceLeave) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("announce-leave-message")
                        .replace("{name}", player.getName())
                        .replace("{reason}", reason)));
            }
            adminModeEnabled.remove(player);
            player.removeAttachment(attachments.get(player));
            attachments.remove(player);
        }
    }

    public boolean isAdminMode(Player player) {
        return adminModeEnabled.contains(player);
    }

    public List<String> getAdminModePermissions() {
        return getConfig().getStringList("admin-mode-permissions");
    }

    public void logEvent(Player player, String type, String text) {
        logs.add(new LogEntry(new Date(), player.getName(), type, text));
    }

    public void saveLogs() {
        if (ensureLogFile()) {
            List<String> lines = logs.stream().map(LogEntry::toString).collect(Collectors.toList());
            try {
                Files.write(logsPath, lines, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean ensureLogFile() {
        return logsPath != null;
    }
}
