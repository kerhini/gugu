package com.jukeboxboat.listener;

import com.jukeboxboat.JukeboxBoatPlugin;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработка размещения лодки-проигрывателя из предмета.
 */
public class BoatPlaceListener implements Listener {

    private final JukeboxBoatPlugin plugin;
    private final Map<UUID, Long> recentPlacers = new ConcurrentHashMap<>();

    public BoatPlaceListener(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Перехватываем ПКМ предметом Лодки-проигрывателя по воде или блоку.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getManager().isJukeboxBoatItem(item)) return;

        // RayTrace с учетом жидкостей (воды)
        RayTraceResult result = player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS);
        if (result == null || result.getHitBlock() == null) return;

        Location spawnLoc = result.getHitPosition().toLocation(player.getWorld());
        spawnLoc.setYaw(player.getLocation().getYaw());
        spawnLoc.setPitch(0);

        // Отменяем стандартное размещение
        event.setCancelled(true);

        recentPlacers.put(player.getUniqueId(), System.currentTimeMillis());

        // Спавним чистую лодку (без клиентского сундука) и ставим в неё 3D Jukebox
        Boat boat = (Boat) player.getWorld().spawnEntity(spawnLoc, EntityType.BOAT);
        Boat.Type woodType = getBoatWoodType(item.getType());
        if (woodType != null) {
            boat.setBoatType(woodType);
        }

        plugin.getManager().markAsJukeboxBoat(boat);
        boat.getWorld().playSound(spawnLoc, Sound.ENTITY_BOAT_PADDLE_WATER, 1.0f, 1.0f);

        // Потребляем предмет в режиме выживания
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (item.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    /**
     * Fallback при спавне лодки.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        if (plugin.getManager().isJukeboxBoat(boat)) return;

        long now = System.currentTimeMillis();
        for (Player p : boat.getWorld().getPlayers()) {
            Long time = recentPlacers.get(p.getUniqueId());
            if (time != null && (now - time) < 1500) {
                if (p.getLocation().distanceSquared(boat.getLocation()) < 25) {
                    plugin.getManager().markAsJukeboxBoat(boat);
                    break;
                }
            }
        }
    }

    private Boat.Type getBoatWoodType(Material material) {
        String name = material.name();
        if (name.contains("BAMBOO")) return Boat.Type.BAMBOO;
        if (name.contains("BIRCH")) return Boat.Type.BIRCH;
        if (name.contains("SPRUCE")) return Boat.Type.SPRUCE;
        if (name.contains("JUNGLE")) return Boat.Type.JUNGLE;
        if (name.contains("ACACIA")) return Boat.Type.ACACIA;
        if (name.contains("DARK_OAK")) return Boat.Type.DARK_OAK;
        if (name.contains("MANGROVE")) return Boat.Type.MANGROVE;
        if (name.contains("CHERRY")) return Boat.Type.CHERRY;
        return Boat.Type.OAK;
    }
}
