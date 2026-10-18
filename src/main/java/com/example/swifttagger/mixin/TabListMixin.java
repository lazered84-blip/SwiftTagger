package com.example.swifttagger.mixin;

import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.TierChatDisplay;
import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.kit.KitResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class TabListMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void swifttagger$appendTier(CallbackInfoReturnable<Text> cir) {
        ModConfig config = ConfigManager.get();
        if (!config.enabled || !config.showTier || !config.showOnTab) return;

        PlayerListEntry self = (PlayerListEntry) (Object) this;
        String name = self.getProfile().name();
        Text original = cir.getReturnValue();
        if (name == null || original == null) return;

        PlayerProfile profile = SwiftTaggerClient.PROFILE_CACHE.get(name);

        if (profile == null || (profile.tiers.isEmpty() && profile.sanction == null)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        KitResult kitResult = KitResult.UNKNOWN;
        if (client.player != null && client.player.getUuid().equals(self.getProfile().id())) {
            kitResult = SwiftTaggerClient.KIT_DETECTION.detectLocal(client.player);
        } else if (client.world != null) {
            PlayerEntity worldPlayer = client.world.getPlayerByUuid(self.getProfile().id());
            if (worldPlayer != null) {
                kitResult = SwiftTaggerClient.KIT_DETECTION.detectRemote(worldPlayer);
            }
        }
        KitId detectedKit = kitResult.kitId();

        cir.setReturnValue(TierChatDisplay.buildTabDisplay(config, profile, detectedKit, original));
    }
}
