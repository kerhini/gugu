package me.andromedov.mixer.api.disc;
import org.bukkit.inventory.ItemStack;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
public interface MixerDiscService {
    CompletionStage<MixerDisc> probe(String source);
    ItemStack createDisc(ItemStack template, MixerDisc disc);
    Optional<MixerDisc> readDisc(ItemStack item);
    boolean isMixerDisc(ItemStack item);
}
