package com.jukeboxboat;

import me.andromedov.mixer.api.MixerApi;
import me.andromedov.mixer.api.MixerAudioPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управление лодками-проигрывателями:
 * - 3D-модель музыкального блока надежно закреплена как пассажир лодки и движется вместе с ней.
 * - Музыка играет НЕПРЕРЫВНО и ПОСТОЯННО от самой лодки.
 */
public class JukeboxBoatManager {

    private final JukeboxBoatPlugin plugin;

    // UUID лодки -> вставленный диск
    private final Map<UUID, ItemStack> insertedDiscs = new ConcurrentHashMap<>();

    // UUID лодки -> играющий сейчас ванильный звук
    private final Map<UUID, Sound> activeVanillaSounds = new ConcurrentHashMap<>();

    // UUID лодки -> задача анимации
    private final Map<UUID, BukkitTask> effectTasks = new ConcurrentHashMap<>();

    // UUID лодки -> Mixer audio player
    private final Map<UUID, MixerAudioPlayer> mixerPlayers = new ConcurrentHashMap<>();

    public JukeboxBoatManager(JukeboxBoatPlugin plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    // Маркировка лодки и 3D-модель Jukebox
    // =====================================================================

    public void markAsJukeboxBoat(Boat boat) {
        PersistentDataContainer pdc = boat.getPersistentDataContainer();
        pdc.set(plugin.getKeyIsJukebox(), PersistentDataType.BYTE, (byte) 1);
        boat.customName(Component.text("♪ Лодка-проигрыватель ♪", NamedTextColor.GOLD));
        boat.setCustomNameVisible(false);

        ensureJukeboxDisplay(boat);
    }

    public boolean isJukeboxBoat(Entity entity) {
        if (entity == null) return false;
        Boat boat = null;
        if (entity instanceof Boat b) {
            boat = b;
        } else if (entity.getVehicle() instanceof Boat vb) {
            boat = vb;
        }
        if (boat == null) return false;

        PersistentDataContainer pdc = boat.getPersistentDataContainer();
        Byte val = pdc.get(plugin.getKeyIsJukebox(), PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    /**
     * Создает или находит BlockDisplay музыкального блока, надежно закрепленный на лодке.
     */
    public BlockDisplay ensureJukeboxDisplay(Boat boat) {
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof BlockDisplay bd) {
                if (bd.getBlock().getMaterial() == Material.JUKEBOX) {
                    return bd;
                }
            }
        }

        BlockDisplay display = (BlockDisplay) boat.getWorld().spawnEntity(boat.getLocation(), EntityType.BLOCK_DISPLAY);
        display.setBlock(Material.JUKEBOX.createBlockData());
        display.getPersistentDataContainer().set(plugin.getKeyIsJukebox(), PersistentDataType.BYTE, (byte) 1);
        display.setBillboard(Display.Billboard.FIXED);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(0);

        // Позиционирование на корме лодки
        float scale = 0.58f;
        float offset = -scale / 2.0f;
        Transformation transform = new Transformation(
                new Vector3f(offset, 0.12f, offset - 0.28f),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
        );
        display.setTransformation(transform);

        boat.addPassenger(display);
        return display;
    }

    /**
     * Посадка игрока в лодку: игрок садится на место водителя (слот 0),
     * а музыкальный блок остается закрепленным сзади (слот 1).
     */
    public void enterBoat(Boat boat, Player player) {
        BlockDisplay display = ensureJukeboxDisplay(boat);

        boat.removePassenger(display);
        boat.addPassenger(player);
        boat.addPassenger(display);
    }

    public void handlePlayerExited(Boat boat, Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (boat.isValid() && !boat.isDead()) {
                ensureJukeboxDisplay(boat);
            }
        });
    }

    // =====================================================================
    // Создание предмета
    // =====================================================================

    public ItemStack createJukeboxBoatItem(Material boatMaterial) {
        if (boatMaterial == null || !boatMaterial.name().contains("BOAT") && !boatMaterial.name().contains("RAFT")) {
            boatMaterial = Material.OAK_BOAT;
        }

        ItemStack item = new ItemStack(boatMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("♪ Лодка-проигрыватель ♪", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Вставьте музыкальный диск (ПКМ)", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Музыкальный блок зафиксирован сзади лодки!", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("✦ Версия: 1.21.11", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("✦ Поддержка Mixer Reloaded", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(
                    plugin.getKeyIsJukebox(), PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isJukeboxBoatItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Byte val = meta.getPersistentDataContainer().get(plugin.getKeyIsJukebox(), PersistentDataType.BYTE);
        if (val != null && val == 1) return true;

        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            return name != null && name.contains("Лодка-проигрыватель");
        }
        return false;
    }

    // =====================================================================
    // Рецепты крафта
    // =====================================================================

    public void registerRecipes() {
        Material[] boats = new Material[] {
                Material.OAK_BOAT, Material.OAK_CHEST_BOAT,
                Material.SPRUCE_BOAT, Material.SPRUCE_CHEST_BOAT,
                Material.BIRCH_BOAT, Material.BIRCH_CHEST_BOAT,
                Material.JUNGLE_BOAT, Material.JUNGLE_CHEST_BOAT,
                Material.ACACIA_BOAT, Material.ACACIA_CHEST_BOAT,
                Material.DARK_OAK_BOAT, Material.DARK_OAK_CHEST_BOAT,
                Material.MANGROVE_BOAT, Material.MANGROVE_CHEST_BOAT,
                Material.CHERRY_BOAT, Material.CHERRY_CHEST_BOAT,
                Material.BAMBOO_RAFT, Material.BAMBOO_CHEST_RAFT
        };

        for (Material b : boats) {
            String keyName = "craft_" + b.name().toLowerCase() + "_jukebox";
            NamespacedKey key = new NamespacedKey(plugin, keyName);
            ItemStack result = createJukeboxBoatItem(b);

            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            recipe.addIngredient(b);
            recipe.addIngredient(Material.JUKEBOX);

            try {
                Bukkit.addRecipe(recipe);
            } catch (Exception ignored) {}
        }
    }

    // =====================================================================
    // Вставка / Извлечение пластинок
    // =====================================================================

    public boolean insertDisc(Boat boat, ItemStack disc, Player player) {
        UUID boatId = boat.getUniqueId();

        if (hasDisc(boat)) {
            ejectDisc(boat, player);
        }

        ItemStack discCopy = disc.clone();
        discCopy.setAmount(1);
        insertedDiscs.put(boatId, discCopy);
        saveDiscToPDC(boat, discCopy);

        if (player != null && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            if (disc.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                disc.setAmount(disc.getAmount() - 1);
            }
        }

        startPlaying(boat, discCopy, player);
        return true;
    }

    public ItemStack ejectDisc(Boat boat, Player player) {
        UUID boatId = boat.getUniqueId();

        stopPlaying(boat);

        ItemStack disc = insertedDiscs.remove(boatId);
        if (disc == null) {
            disc = loadDiscFromPDC(boat);
        }
        clearDiscFromPDC(boat);

        if (disc != null && player != null) {
            var remaining = player.getInventory().addItem(disc);
            for (ItemStack leftover : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        return disc;
    }

    public ItemStack getInsertedDisc(Boat boat) {
        ItemStack disc = insertedDiscs.get(boat.getUniqueId());
        if (disc == null) {
            disc = loadDiscFromPDC(boat);
            if (disc != null) {
                insertedDiscs.put(boat.getUniqueId(), disc);
            }
        }
        return disc;
    }

    public boolean hasDisc(Boat boat) {
        return getInsertedDisc(boat) != null;
    }

    // =====================================================================
    // Непрерывное воспроизведение от лодки
    // =====================================================================

    public void startPlaying(Boat boat, ItemStack disc, Player player) {
        stopPlaying(boat);

        // Mixer Reloaded
        if (plugin.getMixerHook().isAvailable()) {
            String source = plugin.getMixerHook().getMixerDiscSource(disc);
            if (source != null) {
                startMixerPlayback(boat, source, player);
                startEffects(boat);
                return;
            }
        }

        // Ванильное воспроизведение
        startVanillaPlayback(boat, disc);
        startEffects(boat);
    }

    private void startMixerPlayback(Boat boat, String source, Player player) {
        UUID boatId = boat.getUniqueId();
        try {
            MixerApi api = MixerApi.get();

            MixerAudioPlayer audioPlayer = api.createEntityPlayer(boat);
            mixerPlayers.put(boatId, audioPlayer);
            audioPlayer.load(source);

            if (player != null) {
                player.sendActionBar(Component.text("♪ Музыка играет от лодки непрерывно ♪", NamedTextColor.GREEN));
            }
            plugin.getLogger().info("Mixer: звук привязан к лодке " + boatId);
        } catch (Exception e) {
            plugin.getLogger().warning("Mixer error: " + e.getMessage());
            ItemStack disc = insertedDiscs.get(boatId);
            if (disc != null) {
                startVanillaPlayback(boat, disc);
            }
        }
    }

    public void handlePlayerEntered(Boat boat, Player player) {
    }

    private void startVanillaPlayback(Boat boat, ItemStack disc) {
        UUID boatId = boat.getUniqueId();
        Sound sound = getVanillaDiscSound(disc.getType());
        if (sound == null) return;

        activeVanillaSounds.put(boatId, sound);

        int radius = plugin.getSoundRadius();
        float volume = Math.max(1.0f, (float) radius / 16.0f);

        for (Player p : boat.getWorld().getNearbyPlayers(boat.getLocation(), radius)) {
            p.playSound(boat, sound, SoundCategory.RECORDS, volume, 1.0f);
        }
    }

    private void startEffects(Boat boat) {
        UUID boatId = boat.getUniqueId();
        if (!plugin.isParticleEffectsEnabled()) return;
        if (effectTasks.containsKey(boatId)) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!boat.isValid() || boat.isDead() || !hasDisc(boat)) {
                    cancel();
                    effectTasks.remove(boatId);
                    return;
                }
                Location loc = boat.getLocation();
                plugin.getParticleEffect().spawnFloatingSymbol(loc);
            }
        }.runTaskTimer(plugin, 5L, 16L);

        effectTasks.put(boatId, task);
    }

    public void stopPlaying(Boat boat) {
        UUID boatId = boat.getUniqueId();

        MixerAudioPlayer mixerPlayer = mixerPlayers.remove(boatId);
        if (mixerPlayer != null) {
            try { mixerPlayer.stop(); } catch (Exception ignored) {}
        }
        try {
            MixerApi.get().stopEntityPlayer(boat);
        } catch (Exception ignored) {}

        Sound sound = activeVanillaSounds.remove(boatId);
        if (boat.isValid() && boat.getWorld() != null) {
            int radius = plugin.getSoundRadius() * 2;
            for (Player p : boat.getWorld().getNearbyPlayers(boat.getLocation(), radius)) {
                if (sound != null) {
                    p.stopSound(sound, SoundCategory.RECORDS);
                }
                p.stopSound(SoundCategory.RECORDS);
            }
        }

        BukkitTask effectTask = effectTasks.remove(boatId);
        if (effectTask != null) {
            effectTask.cancel();
        }
    }

    public void updatePlayerPosition(Boat boat) {
    }

    public void stopAll() {
        for (UUID boatId : insertedDiscs.keySet()) {
            MixerAudioPlayer mixerPlayer = mixerPlayers.remove(boatId);
            if (mixerPlayer != null) {
                try { mixerPlayer.stop(); } catch (Exception ignored) {}
            }
            BukkitTask effectTask = effectTasks.remove(boatId);
            if (effectTask != null) effectTask.cancel();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(SoundCategory.RECORDS);
        }
        insertedDiscs.clear();
        activeVanillaSounds.clear();
    }

    public void handleBoatDestroyed(Boat boat) {
        UUID boatId = boat.getUniqueId();

        stopPlaying(boat);

        for (Entity p : new ArrayList<>(boat.getPassengers())) {
            p.remove();
        }

        ItemStack disc = insertedDiscs.remove(boatId);
        if (disc == null) {
            disc = loadDiscFromPDC(boat);
        }
        clearDiscFromPDC(boat);

        if (disc != null) {
            boat.getWorld().dropItemNaturally(boat.getLocation(), disc);
        }

        Material boatMat = boat.getBoatMaterial();
        ItemStack boatItem = createJukeboxBoatItem(boatMat);
        boat.getWorld().dropItemNaturally(boat.getLocation(), boatItem);
    }

    // =====================================================================
    // PDC
    // =====================================================================

    private void saveDiscToPDC(Boat boat, ItemStack disc) {
        PersistentDataContainer pdc = boat.getPersistentDataContainer();
        byte[] serialized = disc.serializeAsBytes();
        pdc.set(plugin.getKeyDiscData(), PersistentDataType.BYTE_ARRAY, serialized);

        if (plugin.getMixerHook().isAvailable()) {
            String source = plugin.getMixerHook().getMixerDiscSource(disc);
            if (source != null) {
                pdc.set(plugin.getKeyDiscSource(), PersistentDataType.STRING, source);
            }
        }
    }

    private ItemStack loadDiscFromPDC(Boat boat) {
        PersistentDataContainer pdc = boat.getPersistentDataContainer();
        byte[] data = pdc.get(plugin.getKeyDiscData(), PersistentDataType.BYTE_ARRAY);
        if (data == null) return null;
        try {
            return ItemStack.deserializeBytes(data);
        } catch (Exception e) {
            return null;
        }
    }

    private void clearDiscFromPDC(Boat boat) {
        PersistentDataContainer pdc = boat.getPersistentDataContainer();
        pdc.remove(plugin.getKeyDiscData());
        pdc.remove(plugin.getKeyDiscSource());
    }

    private Sound getVanillaDiscSound(Material material) {
        return switch (material) {
            case MUSIC_DISC_13 -> Sound.MUSIC_DISC_13;
            case MUSIC_DISC_CAT -> Sound.MUSIC_DISC_CAT;
            case MUSIC_DISC_BLOCKS -> Sound.MUSIC_DISC_BLOCKS;
            case MUSIC_DISC_CHIRP -> Sound.MUSIC_DISC_CHIRP;
            case MUSIC_DISC_CREATOR -> Sound.MUSIC_DISC_CREATOR;
            case MUSIC_DISC_CREATOR_MUSIC_BOX -> Sound.MUSIC_DISC_CREATOR_MUSIC_BOX;
            case MUSIC_DISC_FAR -> Sound.MUSIC_DISC_FAR;
            case MUSIC_DISC_MALL -> Sound.MUSIC_DISC_MALL;
            case MUSIC_DISC_MELLOHI -> Sound.MUSIC_DISC_MELLOHI;
            case MUSIC_DISC_STAL -> Sound.MUSIC_DISC_STAL;
            case MUSIC_DISC_STRAD -> Sound.MUSIC_DISC_STRAD;
            case MUSIC_DISC_WARD -> Sound.MUSIC_DISC_WARD;
            case MUSIC_DISC_11 -> Sound.MUSIC_DISC_11;
            case MUSIC_DISC_WAIT -> Sound.MUSIC_DISC_WAIT;
            case MUSIC_DISC_OTHERSIDE -> Sound.MUSIC_DISC_OTHERSIDE;
            case MUSIC_DISC_PIGSTEP -> Sound.MUSIC_DISC_PIGSTEP;
            case MUSIC_DISC_RELIC -> Sound.MUSIC_DISC_RELIC;
            case MUSIC_DISC_5 -> Sound.MUSIC_DISC_5;
            case MUSIC_DISC_PRECIPICE -> Sound.MUSIC_DISC_PRECIPICE;
            default -> null;
        };
    }

    public boolean isMusicDisc(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getType().name().startsWith("MUSIC_DISC_")) return true;
        if (plugin.getMixerHook().isAvailable()) {
            return plugin.getMixerHook().isMixerDisc(item);
        }
        return false;
    }
}
