package com.jukeboxboat.util;

import com.jukeboxboat.JukeboxBoatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Рендерит кастомный парящий символ (Ч'К в двойном кольце)
 * БЕЗ какого-либо фона (100% прозрачность) и БЕЗ рамок карт.
 */
public class CustomParticleEffect {

    private final JukeboxBoatPlugin plugin;

    private static final Component SYMBOL_TEXT = Component.text()
            .append(Component.text(" ╭───╮  ○\n", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
            .append(Component.text("⸨ ч'к ⸩\n", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
            .append(Component.text(" ╰───╯", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
            .build();

    public CustomParticleEffect(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Спавнит парящий светящийся символ без фона.
     */
    public void spawnFloatingSymbol(Location jbLocation) {
        double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.35;
        double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.35;
        Location spawnLoc = jbLocation.clone().add(offsetX, 0.75, offsetZ);

        TextDisplay display = (TextDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        display.text(SYMBOL_TEXT);
        display.setBillboard(Display.Billboard.CENTER); // Всегда лицом к игроку
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Полностью прозрачный фон (БЕЗ черного)
        display.setShadowed(false);
        display.setSeeThrough(false);
        display.setBrightness(new Display.Brightness(15, 15)); // Максимальное белое свечение
        display.setInterpolationDuration(1);
        display.setTeleportDuration(1);

        float initialScale = 0.42f;
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(initialScale, initialScale, initialScale),
                new AxisAngle4f(0, 0, 0, 1)
        ));

        // Плавный полет вверх с покачиванием
        new BukkitRunnable() {
            int ticks = 0;
            final double driftX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.025;
            final double driftZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.025;

            @Override
            public void run() {
                if (!display.isValid() || display.isDead()) {
                    cancel();
                    return;
                }

                ticks++;
                if (ticks > 28) { // 1.4 секунды полета
                    display.remove();
                    cancel();
                    return;
                }

                // Движение вверх
                display.teleport(display.getLocation().add(driftX, 0.04, driftZ));

                // Плавное увеличение и растворение
                float currentScale = (ticks < 20)
                        ? (initialScale + ticks * 0.006f)
                        : (initialScale * (28 - ticks) / 8.0f);

                display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(currentScale, currentScale, currentScale),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
