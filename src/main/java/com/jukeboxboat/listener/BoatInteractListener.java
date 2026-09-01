package com.jukeboxboat.listener;

import com.jukeboxboat.JukeboxBoatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Перехват взаимодействия с лодкой-проигрывателем и её 3D-моделью:
 * 1. ПКМ с пластинкой в руке -> вставить / заменить пластинку
 * 2. Shift + ПКМ -> извлечь пластинку (и заглушить музыку)
 * 3. Обычный ПКМ без пластинки -> сесть в лодку за весла
 */
public class BoatInteractListener implements Listener {

    private final JukeboxBoatPlugin plugin;
    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    public BoatInteractListener(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        Boat boat = null;
        if (clicked instanceof Boat b) {
            boat = b;
        } else if (clicked.getVehicle() instanceof Boat vb) {
            boat = vb;
        }

        if (boat == null || !plugin.getManager().isJukeboxBoat(boat)) return;

        // Отменяем стандартное событие, чтобы полностью управлять поведением
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Защита от двойных кликов пакетов (200 мс)
        long now = System.currentTimeMillis();
        Long lastClick = clickDebounce.get(player.getUniqueId());
        if (lastClick != null && (now - lastClick) < 200) {
            return;
        }
        clickDebounce.put(player.getUniqueId(), now);

        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean holdingDisc = plugin.getManager().isMusicDisc(handItem);

        // =================================================================
        // ВАРИАНТ 1: Игрок держит музыкальную пластинку -> Вставить / Заменить
        // =================================================================
        if (holdingDisc) {
            if (plugin.getManager().hasDisc(boat)) {
                plugin.getManager().ejectDisc(boat, player);
            }

            plugin.getManager().insertDisc(boat, handItem, player);
            player.sendActionBar(Component.text("♪ Диск вставлен! Музыка играет ♪", NamedTextColor.GREEN));
            player.playSound(boat.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            return;
        }

        // =================================================================
        // ВАРИАНТ 2: Игрок нажал Shift + ПКМ (присели) -> Извлечь пластинку
        // =================================================================
        if (player.isSneaking()) {
            if (plugin.getManager().hasDisc(boat)) {
                plugin.getManager().ejectDisc(boat, player);
                player.sendActionBar(Component.text("♪ Диск извлечён", NamedTextColor.YELLOW));
                player.playSound(boat.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendActionBar(Component.text("В лодке нет пластинки (вставьте пластинку ПКМ)", NamedTextColor.GRAY));
            }
            return;
        }

        // =================================================================
        // ВАРИАНТ 3: Обычный клик ПКМ (не на шифте, без диска) -> Сесть в лодку!
        // =================================================================
        if (!boat.getPassengers().contains(player)) {
            plugin.getManager().enterBoat(boat, player);
        }
    }
}
