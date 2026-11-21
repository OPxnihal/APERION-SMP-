package com.aperion.wanted;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AperionWanted extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private PlayerStore store;
    private final Map<UUID, Map<UUID, Long>> alertTimes = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> trackingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> oldCompassTargets = new ConcurrentHashMap<>();
    private BukkitTask detectionTask;

    @Override public void onEnable() {
        saveDefaultConfig();
        try { store = new PlayerStore(new java.io.File(getDataFolder(), "wanted.sqlite")); }
        catch (Exception e) { getLogger().severe("Could not open wanted database: " + e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this, this);
        for (String cmd : List.of("wanted", "bounty", "track")) {
            PluginCommand c = getCommand(cmd);
            if (c != null) { c.setExecutor(this); c.setTabCompleter(this); }
        }
        startDetection();
        getLogger().info("APERION Wanted enabled. SQLite storage ready.");
    }

    @Override public void onDisable() {
        if (detectionTask != null) detectionTask.cancel();
        trackingTasks.values().forEach(BukkitTask::cancel);
        for (Map.Entry<UUID, Location> e : oldCompassTargets.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey()); if (p != null && e.getValue() != null) p.setCompassTarget(e.getValue());
        }
        if (store != null) try { store.close(); } catch (SQLException e) { getLogger().warning(e.getMessage()); }
    }

    private PlayerRecord record(Player p) { return record(p.getUniqueId(), p.getName()); }
    private PlayerRecord record(UUID id, String name) {
        try { return store.get(id, name); } catch (SQLException e) { getLogger().warning("Database read failed: " + e.getMessage()); return new PlayerRecord(id, name, 0, 0, 0, 0); }
    }
    private void save(PlayerRecord r) { try { store.save(r); } catch (SQLException e) { getLogger().warning("Database write failed: " + e.getMessage()); } }

    private String rank(int score) {
        String result = "Unknown";
        for (Map<?, ?> row : getConfig().getMapList("wanted.ranks")) {
            Object min = row.get("minimum"), name = row.get("name");
            if (min instanceof Number n && name != null && score >= n.intValue()) result = String.valueOf(name);
        }
        return result;
    }
    private int rankMinimum(String rank) {
        for (Map<?, ?> row : getConfig().getMapList("wanted.ranks")) if (rank.equalsIgnoreCase(String.valueOf(row.get("name")))) return ((Number) row.get("minimum")).intValue();
        return 0;
    }
    private String msg(String key) { return color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, "")); }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    private boolean enabled() { return getConfig().getBoolean("system.enabled", true); }

    private void startDetection() {
        if (detectionTask != null) detectionTask.cancel();
        long interval = Math.max(10, getConfig().getLong("system.detection-interval-ticks", 20));
        detectionTask = Bukkit.getScheduler().runTaskTimer(this, this::detectNearby, interval, interval);
    }

    private void detectNearby() {
        refreshTabNames();
        if (!enabled()) return;
        double radius = Math.max(1, getConfig().getDouble("system.detection-radius", 48));
        double r2 = radius * radius;
        long cooldown = getConfig().getLong("system.alert-cooldown-seconds", 45) * 1000L;
        int minimum = getConfig().getInt("system.alert-minimum-score", 1000);
        for (Player wanted : Bukkit.getOnlinePlayers()) {
            PlayerRecord wr = record(wanted);
            if (wr.score < minimum) continue;
            for (Player observer : wanted.getWorld().getPlayers()) {
                if (observer.equals(wanted) || observer.getLocation().distanceSquared(wanted.getLocation()) > r2) continue;
                Map<UUID, Long> seen = alertTimes.computeIfAbsent(observer.getUniqueId(), k -> new ConcurrentHashMap<>());
                long now = System.currentTimeMillis();
                if (now - seen.getOrDefault(wanted.getUniqueId(), 0L) < cooldown) continue;
                seen.put(wanted.getUniqueId(), now);
                int distance = (int) Math.round(observer.getLocation().distance(wanted.getLocation()));
                String title = getConfig().getString("system.alert-title", "&4MOST WANTED PLAYER DETECTED");
                String sub = getConfig().getString("system.alert-subtitle", "&cA dangerous player is nearby: &f{player}").replace("{player}", wanted.getName());
                observer.sendTitle(color(title), color(sub), 5, 55, 10);
                String chat = getConfig().getString("system.alert-chat", "&cMOST WANTED DETECTED: {player}").replace("{player}", wanted.getName()).replace("{distance}", String.valueOf(distance));
                observer.sendMessage(color(chat));
                playAlertSound(observer);
                Map<UUID, Long> targetSeen = alertTimes.computeIfAbsent(wanted.getUniqueId(), k -> new ConcurrentHashMap<>());
                if (now - targetSeen.getOrDefault(observer.getUniqueId(), 0L) >= cooldown) {
                    targetSeen.put(observer.getUniqueId(), now);
                    wanted.sendTitle(color(getConfig().getString("system.target-alert-title", "&cBOUNTY HUNTER NEARBY")), color(getConfig().getString("system.target-alert-subtitle", "&eA player has entered your detection range.")), 5, 55, 10);
                    wanted.sendMessage(color(getConfig().getString("system.target-alert-chat", "&cWARNING: &eA bounty hunter is nearby!")));
                    playAlertSound(wanted);
                }
            }
        }
    }

    private void playAlertSound(Player player) {
        try { player.playSound(player.getLocation(), Sound.valueOf(getConfig().getString("system.alert-sound", "ENTITY_ELDER_GUARDIAN_CURSE")), 1.0f, 0.7f); } catch (IllegalArgumentException ignored) {}
    }

    private void refreshTabNames() {
        if (!getConfig().getBoolean("tab-list.enabled", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRecord r = record(player);
            String rankName = rank(r.score);
            if (r.score <= 0 || rankName.equalsIgnoreCase("Unknown")) {
                player.setPlayerListName(color(getConfig().getString("tab-list.normal-format", "&f{player}").replace("{player}", player.getName())));
            } else {
                String format = getConfig().getString("tab-list.wanted-format", "&c⚠ &f{player} &8[&c{rank}&8]");
                player.setPlayerListName(color(format.replace("{player}", player.getName()).replace("{rank}", rankName.toUpperCase(Locale.ROOT))));
            }
        }
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PlayerRecord vr = record(victim); vr.streak = 0; save(vr);
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !enabled()) return;
        PlayerRecord kr = record(killer);
        kr.kills++; kr.streak++;
        int base = getConfig().getInt("wanted.kill-score", 25);
        int bonus = Math.min(getConfig().getInt("wanted.streak-bonus-cap", 50), Math.max(0, kr.streak - 1) * getConfig().getInt("wanted.streak-bonus-per-kill", 5));
        kr.score = Math.max(0, kr.score + base + bonus);
        save(kr);
        refreshTabNames();
        killer.sendMessage(color("&8[&cWanted&8] &7PvP kill recorded. &c+" + (base + bonus) + " Wanted &8| &f" + rank(kr.score) + " &8| &7Streak: &f" + kr.streak));
        payBounty(killer, victim, vr);
    }

    private void payBounty(Player killer, Player victim, PlayerRecord target) {
        long cooldown = getConfig().getLong("bounty.same-pair-cooldown-hours", 24) * 3_600_000L;
        try {
            if (!store.mayClaim(killer.getUniqueId(), victim.getUniqueId(), cooldown)) return;
            List<String> pool = getConfig().getStringList("bounty.rewards." + rank(target.score));
            int totalItems = 0;
            if (!pool.isEmpty()) {
                String reward = pool.get(new Random().nextInt(pool.size()));
                String[] parts = reward.split(":");
                Material material = Material.matchMaterial(parts[0]);
                int amount = 1;
                if (parts.length > 1) {
                    String[] range = parts[1].split("-");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : min;
                    if (min < 1 || max < min) throw new IllegalArgumentException("Invalid reward amount: " + reward);
                    amount = min + new Random().nextInt(max - min + 1);
                }
                if (material == null || amount <= 0) { getLogger().warning("Invalid bounty reward: " + reward); return; }
                giveItem(killer, material, amount); totalItems += amount;
            }
            if (target.bounty > 0) {
                Material custom = Material.matchMaterial(getConfig().getString("bounty.custom-bounty-material", "DIAMOND"));
                if (custom != null) { giveItem(killer, custom, target.bounty); totalItems += target.bounty; }
            }
            if (totalItems == 0) return;
            store.recordClaim(killer.getUniqueId(), victim.getUniqueId());
            killer.sendMessage(color("&6Bounty claimed! &eReward delivered for eliminating " + victim.getName() + "."));
        } catch (Exception e) { getLogger().warning("Bounty payout failed: " + e.getMessage()); }
    }

    private void giveItem(Player player, Material material, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(remaining, material.getMaxStackSize());
            ItemStack item = new ItemStack(material, stack);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            remaining -= stack;
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (cmd.equals("track")) return track(sender, args);
        if (cmd.equals("bounty")) return bounty(sender, args);
        return wanted(sender, args);
    }

    private boolean wanted(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Use /wanted <player> from console."); return true; }
            showRecord(sender, record(p)); return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (List.of("reload", "enable", "disable", "reset").contains(sub)) {
            if (!sender.hasPermission("aperionwanted.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            switch (sub) {
                case "reload" -> { reloadConfig(); startDetection(); sender.sendMessage(color("&aAPERION Wanted config reloaded.")); }
                case "enable" -> { getConfig().set("system.enabled", true); saveConfig(); sender.sendMessage(color("&aWanted system enabled.")); }
                case "disable" -> { getConfig().set("system.enabled", false); saveConfig(); sender.sendMessage(color("&cWanted system disabled.")); }
                case "reset" -> {
                    if (args.length < 2) { sender.sendMessage("/wanted reset <player>"); return true; }
                    Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage(msg("player-not-found")); return true; }
                    PlayerRecord r = record(target); r.score = 0; r.streak = 0; r.kills = 0; save(r); refreshTabNames(); sender.sendMessage(color("&aReset Wanted stats for " + target.getName()));
                }
            }
            return true;
        }
        if (sub.equals("top")) {
            List<PlayerRecord> rows = new ArrayList<>(); for (Player p : Bukkit.getOnlinePlayers()) rows.add(record(p));
            rows.sort(Comparator.comparingInt((PlayerRecord r) -> r.score).reversed()); sender.sendMessage(color("&8&m-----&r &cAPERION MOST WANTED &8&m-----"));
            int i = 1; for (PlayerRecord r : rows) { sender.sendMessage(color("&c" + i++ + ". &f" + r.lastName + " &8- &e" + r.score + " &7(" + rank(r.score) + ")")); if (i > 10) break; } return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage(msg("player-not-found")); return true; }
        if (!sender.hasPermission("aperionwanted.check") && !sender.hasPermission("aperionwanted.admin")) { sender.sendMessage(msg("no-permission")); return true; }
        showRecord(sender, record(target)); return true;
    }

    private void showRecord(CommandSender sender, PlayerRecord r) {
        sender.sendMessage(color("&8&m--------&r &cAPERION WANTED &8&m--------"));
        sender.sendMessage(color("&7Player: &f" + r.lastName));
        sender.sendMessage(color("&7Rank: &c" + rank(r.score)));
        sender.sendMessage(color("&7Wanted score: &f" + r.score + " &8| &7Kills: &f" + r.kills + " &8| &7Streak: &f" + r.streak));
        sender.sendMessage(color("&7Bounty: &6" + r.bounty));
    }

    private boolean bounty(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage("/bounty <player> | /bounty set <player> <amount> | /bounty remove <player>"); return true; }
        if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("aperionwanted.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length < (args[0].equalsIgnoreCase("set") ? 3 : 2)) { sender.sendMessage("/bounty set <player> <amount> OR /bounty remove <player>"); return true; }
            Player p = Bukkit.getPlayerExact(args[1]); if (p == null) { sender.sendMessage(msg("player-not-found")); return true; }
            PlayerRecord r = record(p);
            if (args[0].equalsIgnoreCase("remove")) r.bounty = 0;
            else try { r.bounty = Math.max(0, Integer.parseInt(args[2])); } catch (NumberFormatException e) { sender.sendMessage("Amount must be a whole number."); return true; }
            save(r); sender.sendMessage(color("&aBounty updated for " + p.getName() + ".")); return true;
        }
        Player p = Bukkit.getPlayerExact(args[0]); if (p == null) { sender.sendMessage(msg("player-not-found")); return true; }
        PlayerRecord r = record(p); sender.sendMessage(color("&c" + p.getName() + " &7is &f" + rank(r.score) + " &7with a bounty value of &6" + r.bounty + ".")); return true;
    }

    private boolean track(CommandSender sender, String[] args) {
        if (!(sender instanceof Player hunter)) { sender.sendMessage(msg("player-only")); return true; }
        if (!hunter.hasPermission("aperionwanted.track")) { hunter.sendMessage(msg("no-permission")); return true; }
        if (!enabled() || !getConfig().getBoolean("tracking.enabled", true)) { hunter.sendMessage(msg("disabled")); return true; }
        if (args.length < 1) { hunter.sendMessage("/track <wanted-player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(hunter)) { hunter.sendMessage(msg("player-not-found")); return true; }
        if (!hunter.getWorld().equals(target.getWorld())) { hunter.sendMessage(color("&cYou cannot track players in another dimension.")); return true; }
        PlayerRecord tr = record(target);
        if (tr.score <= 0) { hunter.sendMessage(color("&cThat player is not wanted.")); return true; }
        double max = getConfig().getDouble("tracking.maximum-distance", 256);
        if (hunter.getLocation().distanceSquared(target.getLocation()) > max * max) { hunter.sendMessage(color("&cThat player is too far away to track.")); return true; }
        if (getConfig().getBoolean("tracking.require-line-of-sight", true) && !hunter.hasLineOfSight(target)) { hunter.sendMessage(color("&cYou must have a clear line of sight to acquire the target.")); return true; }
        BukkitTask old = trackingTasks.remove(hunter.getUniqueId()); if (old != null) old.cancel();
        oldCompassTargets.putIfAbsent(hunter.getUniqueId(), hunter.getCompassTarget().clone());
        long end = System.currentTimeMillis() + getConfig().getLong("tracking.duration-seconds", 60) * 1000L;
        long interval = Math.max(10, getConfig().getLong("tracking.update-interval-ticks", 20));
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!hunter.isOnline() || !target.isOnline() || !hunter.getWorld().equals(target.getWorld()) || System.currentTimeMillis() >= end || (getConfig().getBoolean("tracking.require-line-of-sight", true) && !hunter.hasLineOfSight(target))) {
                stopTracking(hunter, true); return;
            }
            if (hunter.getLocation().distanceSquared(target.getLocation()) > max * max) { stopTracking(hunter, true); return; }
            hunter.setCompassTarget(target.getLocation());
        }, 0L, interval);
        trackingTasks.put(hunter.getUniqueId(), task);
        int distance = (int) Math.round(hunter.getLocation().distance(target.getLocation()));
        hunter.sendMessage(color(getConfig().getString("tracking.message", "Tracking {player}: {distance} blocks.").replace("{player}", target.getName()).replace("{distance}", String.valueOf(distance))));
        return true;
    }

    private void stopTracking(Player p, boolean notify) {
        BukkitTask task = trackingTasks.remove(p.getUniqueId()); if (task != null) task.cancel();
        Location old = oldCompassTargets.remove(p.getUniqueId()); if (old != null && p.isOnline()) p.setCompassTarget(old);
        if (notify && p.isOnline()) p.sendMessage(color(getConfig().getString("tracking.expired-message", "&7Tracking expired.")));
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && (command.getName().equalsIgnoreCase("wanted") || command.getName().equalsIgnoreCase("bounty"))) {
            List<String> values = new ArrayList<>(List.of("top", "reload", "enable", "disable", "reset", "set", "remove"));
            Bukkit.getOnlinePlayers().forEach(p -> values.add(p.getName())); return values.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 1 && command.getName().equalsIgnoreCase("track")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && command.getName().equalsIgnoreCase("bounty") && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
