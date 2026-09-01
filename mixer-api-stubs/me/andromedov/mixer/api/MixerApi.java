package me.andromedov.mixer.api;

import me.andromedov.mixer.api.disc.MixerDiscService;
import me.andromedov.mixer.api.playback.MixerPlaybackPolicyRegistry;
import me.andromedov.mixer.api.source.MixerAudioSourceRegistry;
import me.andromedov.mixer.api.addon.MixerAddonManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public interface MixerApi {
    static MixerApi get() {
        return getIfAvailable().orElseThrow(() -> new IllegalStateException(
                "Mixer API is unavailable. Add Mixer to depend or softdepend in plugin.yml."));
    }

    static Optional<MixerApi> getIfAvailable() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(MixerApi.class));
    }

    String version();
    MixerAudioPlayer createPlayer(Location location);
    MixerAudioPlayer getOrCreatePlayer(Location location);
    Optional<MixerAudioPlayer> findPlayer(Location location);
    Collection<MixerAudioPlayer> players();
    boolean stopPlayer(Location location);
    MixerAudioPlayer createPortablePlayer(Player owner);
    Optional<MixerAudioPlayer> findPortablePlayer(Player owner);
    boolean stopPortablePlayer(Player owner);
    MixerAudioPlayer createEntityPlayer(org.bukkit.entity.Entity entity);
    Optional<MixerAudioPlayer> findEntityPlayer(org.bukkit.entity.Entity entity);
    boolean stopEntityPlayer(org.bukkit.entity.Entity entity);
    MixerAddonManager addons();
    MixerAudioSourceRegistry sources();
    MixerDiscService discs();
    MixerPlaybackPolicyRegistry playbackPolicies();
}
