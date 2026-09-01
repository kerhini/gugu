package com.jukeboxboat.mixer;

import com.jukeboxboat.JukeboxBoatPlugin;
import me.andromedov.mixer.api.MixerApi;
import me.andromedov.mixer.api.disc.MixerDisc;
import me.andromedov.mixer.api.disc.MixerDiscService;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Интеграция с Mixer Reloaded (soft dependency).
 * Если Mixer установлен — используем его API для:
 * - Определения кастомных дисков
 * - Извлечения URL источника аудио
 * - Воспроизведения через Mixer audio player
 */
public class MixerReloadedHook {

    private final JukeboxBoatPlugin plugin;
    private boolean available;

    public MixerReloadedHook(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    /**
     * Проверяет, доступен ли Mixer Reloaded.
     */
    private void checkAvailability() {
        try {
            // Проверяем, что плагин загружен
            if (plugin.getServer().getPluginManager().getPlugin("Mixer") == null) {
                available = false;
                return;
            }

            // Проверяем, что API зарегистрирован в ServicesManager
            Optional<MixerApi> api = MixerApi.getIfAvailable();
            available = api.isPresent();
        } catch (NoClassDefFoundError | Exception e) {
            // Класс MixerApi не найден — Mixer не установлен
            available = false;
        }
    }

    /**
     * Доступен ли Mixer Reloaded.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Проверяет, является ли ItemStack диском Mixer.
     */
    public boolean isMixerDisc(ItemStack item) {
        if (!available || item == null) return false;
        try {
            MixerDiscService discService = MixerApi.get().discs();
            return discService.isMixerDisc(item);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка проверки Mixer диска: " + e.getMessage());
            return false;
        }
    }

    /**
     * Извлекает URL/источник аудио из Mixer диска.
     * @return URL источника или null, если это не Mixer диск
     */
    public String getMixerDiscSource(ItemStack item) {
        if (!available || item == null) return null;
        try {
            MixerDiscService discService = MixerApi.get().discs();
            Optional<MixerDisc> disc = discService.readDisc(item);
            return disc.map(MixerDisc::source).orElse(null);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка чтения Mixer диска: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получает название трека из Mixer диска (если доступно).
     * @return Название трека или null
     */
    public String getMixerDiscTitle(ItemStack item) {
        if (!available || item == null) return null;
        try {
            MixerDiscService discService = MixerApi.get().discs();
            Optional<MixerDisc> disc = discService.readDisc(item);
            return disc.flatMap(MixerDisc::track)
                    .map(track -> track.title() + " — " + track.author())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Получает MixerApi.
     */
    public MixerApi getApi() {
        if (!available) return null;
        try {
            return MixerApi.get();
        } catch (Exception e) {
            return null;
        }
    }
}
