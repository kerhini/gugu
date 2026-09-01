package com.jukeboxboat.listener;

import com.jukeboxboat.JukeboxBoatPlugin;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

/**
 * Обработка уничтожения лодки — остановка музыки, удаление BlockDisplay и выброс диска с предметом лодки.
 */
public class BoatDestroyListener implements Listener {

    private final JukeboxBoatPlugin plugin;

    public BoatDestroyListener(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        if (!plugin.getManager().isJukeboxBoat(boat)) return;

        if (event.isCancelled()) return;

        plugin.getManager().handleBoatDestroyed(boat);
    }
}
