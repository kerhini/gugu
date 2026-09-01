package com.jukeboxboat.listener;

import com.jukeboxboat.JukeboxBoatPlugin;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/**
 * Отслеживание движения лодки для обновления позиции звука.
 */
public class BoatMoveListener implements Listener {

    private final JukeboxBoatPlugin plugin;

    public BoatMoveListener(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        if (!plugin.getManager().isJukeboxBoat(boat)) return;
        if (!plugin.getManager().hasDisc(boat)) return;
        if (!plugin.isMovingPlaybackAllowed()) return;

        plugin.getManager().updatePlayerPosition(boat);
    }
}
