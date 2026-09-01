package com.jukeboxboat.command;

import com.jukeboxboat.JukeboxBoatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Команды для управления лодками-проигрывателями с проверкой прав доступа.
 *
 * jukeboxboat.admin — полный доступ
 * jukeboxboat.command.give — выдача предметов
 * jukeboxboat.command.convert — превращение лодок
 * jukeboxboat.command.info — просмотр информации
 * jukeboxboat.command.reload — перезагрузка конфига
 */
public class JukeboxBoatCommand implements CommandExecutor, TabCompleter {

    private final JukeboxBoatPlugin plugin;

    public JukeboxBoatCommand(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean checkPerm(CommandSender sender, String permission) {
        if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(Component.text("У вас нет прав для выполнения этой команды! (" + permission + ")", NamedTextColor.RED));
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "give" -> {
                if (!checkPerm(sender, "jukeboxboat.command.give")) yield true;
                yield handleGive(sender, args);
            }
            case "convert" -> {
                if (!checkPerm(sender, "jukeboxboat.command.convert")) yield true;
                yield handleConvert(sender);
            }
            case "info" -> {
                if (!checkPerm(sender, "jukeboxboat.command.info")) yield true;
                yield handleInfo(sender);
            }
            case "reload" -> {
                if (!checkPerm(sender, "jukeboxboat.command.reload")) yield true;
                yield handleReload(sender);
            }
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    // =====================================================================
    // Give — выдать лодку-проигрыватель
    // =====================================================================

    private boolean handleGive(CommandSender sender, String[] args) {
        Player target = null;
        Material boatMaterial = Material.OAK_BOAT;

        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                Material parsedMat = parseBoatMaterial(args[1]);
                if (parsedMat != null && sender instanceof Player p) {
                    target = p;
                    boatMaterial = parsedMat;
                } else {
                    sender.sendMessage(Component.text("Игрок " + args[1] + " не найден в сети!", NamedTextColor.RED));
                    return true;
                }
            }
        }

        if (args.length >= 3) {
            Material parsedMat = parseBoatMaterial(args[2]);
            if (parsedMat != null) {
                boatMaterial = parsedMat;
            }
        }

        if (target == null) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Использование из консоли: /jbboat give <игрок> [тип_дерева]", NamedTextColor.RED));
                return true;
            }
        }

        ItemStack boatItem = plugin.getManager().createJukeboxBoatItem(boatMaterial);
        var leftovers = target.getInventory().addItem(boatItem);
        for (ItemStack leftover : leftovers.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        target.sendMessage(Component.text("Вам выдана ", NamedTextColor.GREEN)
                .append(Component.text("♪ Лодка-проигрыватель ♪", NamedTextColor.GOLD))
                .append(Component.text("! Поставьте её на воду.", NamedTextColor.GREEN)));

        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Лодка-проигрыватель выдана игроку " + target.getName() + "!", NamedTextColor.GREEN));
        }

        return true;
    }

    private Material parseBoatMaterial(String input) {
        String upper = input.toUpperCase();
        if (!upper.endsWith("_BOAT") && !upper.endsWith("_RAFT")) {
            upper = upper + "_BOAT";
        }
        try {
            return Material.valueOf(upper);
        } catch (IllegalArgumentException e) {
            if (input.equalsIgnoreCase("bamboo") || input.equalsIgnoreCase("raft")) {
                return Material.BAMBOO_RAFT;
            }
            return null;
        }
    }

    // =====================================================================
    // Convert — конвертировать ближайшую лодку
    // =====================================================================

    private boolean handleConvert(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        Boat nearestBoat = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof Boat boat) {
                double dist = entity.getLocation().distanceSquared(player.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestBoat = boat;
                }
            }
        }

        if (nearestBoat == null) {
            player.sendMessage(Component.text("Рядом с вами (в радиусе 6 блоков) не найдено лодок!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getManager().isJukeboxBoat(nearestBoat)) {
            player.sendMessage(Component.text("Эта лодка уже является проигрывателем!", NamedTextColor.YELLOW));
            return true;
        }

        plugin.getManager().markAsJukeboxBoat(nearestBoat);
        player.sendMessage(Component.text("Лодка успешно превращена в проигрыватель с блоком Jukebox! ♪", NamedTextColor.GREEN));
        return true;
    }

    // =====================================================================
    // Info — информация о лодке
    // =====================================================================

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        Entity target = player.getTargetEntity(6);

        if (!(target instanceof Boat boat)) {
            player.sendMessage(Component.text("Наведите прицел на лодку!", NamedTextColor.RED));
            return true;
        }

        boolean isJukebox = plugin.getManager().isJukeboxBoat(boat);
        ItemStack disc = plugin.getManager().getInsertedDisc(boat);

        player.sendMessage(Component.text("═══ Информация о лодке ═══", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Тип: ", NamedTextColor.GRAY)
                .append(Component.text(isJukebox ? "Лодка-проигрыватель (Jukebox) ♪" : "Обычная лодка",
                        isJukebox ? NamedTextColor.GREEN : NamedTextColor.WHITE)));

        if (isJukebox) {
            if (disc != null) {
                String discInfo = disc.getType().name();
                if (plugin.getMixerHook().isAvailable()) {
                    String title = plugin.getMixerHook().getMixerDiscTitle(disc);
                    if (title != null) discInfo = title + " (Mixer)";
                    String source = plugin.getMixerHook().getMixerDiscSource(disc);
                    if (source != null) {
                        player.sendMessage(Component.text("Источник: ", NamedTextColor.GRAY)
                                .append(Component.text(source, NamedTextColor.AQUA)));
                    }
                }
                player.sendMessage(Component.text("Диск: ", NamedTextColor.GRAY)
                        .append(Component.text(discInfo, NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("Диск: ", NamedTextColor.GRAY)
                        .append(Component.text("пуста (вставьте пластинку ПКМ)", NamedTextColor.YELLOW)));
            }
        }

        player.sendMessage(Component.text("Mixer Reloaded: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getMixerHook().isAvailable() ? "✓ подключен" : "✗ не найден",
                        plugin.getMixerHook().isAvailable() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        return true;
    }

    // =====================================================================
    // Reload — перезагрузка
    // =====================================================================

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("Конфигурация JukeboxBoat успешно перезагружена!", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══ JukeboxBoat Команды ═══", NamedTextColor.GOLD));
        if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.give")) {
            sender.sendMessage(Component.text("/jbboat give [игрок] [дерево]", NamedTextColor.YELLOW)
                    .append(Component.text(" — выдать лодку-проигрыватель", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.convert")) {
            sender.sendMessage(Component.text("/jbboat convert", NamedTextColor.YELLOW)
                    .append(Component.text(" — превратить ближайшую лодку в проигрыватель", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.info")) {
            sender.sendMessage(Component.text("/jbboat info", NamedTextColor.YELLOW)
                    .append(Component.text(" — информация о лодке под прицелом", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.reload")) {
            sender.sendMessage(Component.text("/jbboat reload", NamedTextColor.YELLOW)
                    .append(Component.text(" — перезагрузить конфигурацию", NamedTextColor.GRAY)));
        }
    }

    // =====================================================================
    // Tab Completion
    // =====================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.give")) sub.add("give");
            if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.convert")) sub.add("convert");
            if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.info")) sub.add("info");
            if (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.reload")) sub.add("reload");
            return sub.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")
                && (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.give"))) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            suggestions.addAll(List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo"));
            return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")
                && (sender.hasPermission("jukeboxboat.admin") || sender.hasPermission("jukeboxboat.command.give"))) {
            return List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
