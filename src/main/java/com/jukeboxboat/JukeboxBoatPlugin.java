package com.jukeboxboat;

import com.jukeboxboat.command.JukeboxBoatCommand;
import com.jukeboxboat.listener.BoatDestroyListener;
import com.jukeboxboat.listener.BoatInteractListener;
import com.jukeboxboat.listener.BoatMoveListener;
import com.jukeboxboat.listener.BoatPassengerListener;
import com.jukeboxboat.listener.BoatPlaceListener;
import com.jukeboxboat.mixer.MixerReloadedHook;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class JukeboxBoatPlugin extends JavaPlugin {

    private static JukeboxBoatPlugin instance;

    // NamespacedKey для PersistentDataContainer
    public static final String NAMESPACE = "jukeboxboat";
    private NamespacedKey keyIsJukebox;
    private NamespacedKey keyDiscData;
    private NamespacedKey keyDiscSource; // Для Mixer дисков — хранит URL источника

    private JukeboxBoatManager manager;
    private MixerReloadedHook mixerHook;
    private com.jukeboxboat.util.CustomParticleEffect particleEffect;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Инициализация ключей
        keyIsJukebox = new NamespacedKey(this, "is_jukebox");
        keyDiscData = new NamespacedKey(this, "disc_data");
        keyDiscSource = new NamespacedKey(this, "disc_source");

        // Инициализация менеджера
        manager = new JukeboxBoatManager(this);
        manager.registerRecipes();

        // Инициализация кастомного визуального эффекта (логотипа)
        particleEffect = new com.jukeboxboat.util.CustomParticleEffect(this);

        // Попытка подключения к Mixer Reloaded
        mixerHook = new MixerReloadedHook(this);
        if (mixerHook.isAvailable()) {
            getLogger().info("Mixer Reloaded обнаружен! Интеграция активирована.");
        } else {
            getLogger().info("Mixer Reloaded не найден. Работаем с ванильными дисками.");
        }

        // Регистрация слушателей
        var pm = getServer().getPluginManager();
        pm.registerEvents(new BoatInteractListener(this), this);
        pm.registerEvents(new BoatDestroyListener(this), this);
        pm.registerEvents(new BoatMoveListener(this), this);
        pm.registerEvents(new BoatPlaceListener(this), this);
        pm.registerEvents(new BoatPassengerListener(this), this);

        // Регистрация команд
        var cmd = getCommand("jbboat");
        if (cmd != null) {
            var executor = new JukeboxBoatCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("JukeboxBoat загружен! Версия " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Остановить все активные воспроизведения
        if (manager != null) {
            manager.stopAll();
        }
        getLogger().info("JukeboxBoat выключен.");
    }

    // --- Геттеры ---

    public static JukeboxBoatPlugin getInstance() {
        return instance;
    }

    public JukeboxBoatManager getManager() {
        return manager;
    }

    public MixerReloadedHook getMixerHook() {
        return mixerHook;
    }

    public com.jukeboxboat.util.CustomParticleEffect getParticleEffect() {
        return particleEffect;
    }

    public NamespacedKey getKeyIsJukebox() {
        return keyIsJukebox;
    }

    public NamespacedKey getKeyDiscData() {
        return keyDiscData;
    }

    public NamespacedKey getKeyDiscSource() {
        return keyDiscSource;
    }

    // --- Конфигурация ---

    public int getSoundRadius() {
        return getConfig().getInt("sound-radius", 64);
    }

    public boolean isParticleEffectsEnabled() {
        return getConfig().getBoolean("particle-effects", true);
    }

    public boolean isMovingPlaybackAllowed() {
        return getConfig().getBoolean("allow-moving-playback", true);
    }

    public int getSoundUpdateInterval() {
        return getConfig().getInt("sound-update-interval", 60);
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "&7[JukeboxBoat] Сообщение не найдено: " + key);
    }
}
