package com.jukeboxboat.listener;

import com.jukeboxboat.JukeboxBoatPlugin;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/**
 * Отслеживает посадку и высадку игрока из лодки-проигрывателя,
 * чтобы привязать аудиопоток к плывущему игроку (Mixer Portable Player).
 */
public class BoatPassengerListener implements Listener {

    private final JukeboxBoatPlugin plugin;

    public BoatPassengerListener(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getEntered() instanceof Player player)) return;
        if (!plugin.getManager().isJukeboxBoat(boat)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (boat.isValid() && player.isOnline() && boat.getPassengers().contains(player)) {
                plugin.getManager().handlePlayerEntered(boat, player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getExited() instanceof Player player)) return;
        if (!plugin.getManager().isJukeboxBoat(boat)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (boat.isValid()) {
                plugin.getManager().handlePlayerExited(boat, player);
            }
        });
    }
}
