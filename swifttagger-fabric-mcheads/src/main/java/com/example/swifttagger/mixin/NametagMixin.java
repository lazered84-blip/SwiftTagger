package com.example.swifttagger.mixin;

import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierResolver;
import com.example.swifttagger.kit.KitDefinition;
import com.example.swifttagger.kit.KitDefinitions;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.kit.KitResult;
import com.example.swifttagger.text.TierTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ajoute le tier au nom affiché au-dessus du joueur (section 10 "Nametag").
 *
 * Corrigé par rapport à la version buguée :
 *  - respecte le coupe-circuit général ModConfig#enabled ;
 *  - utilise le vrai pipeline kit -> tier (TierResolver) au lieu d'un
 *    simple "meilleur tier toutes catégories" hardcodé ;
 *  - utilise TierTextRenderer pour respecter le format/les couleurs
 *    configurables (sections 6/7/12) au lieu d'un préfixe doré codé en dur.
 */
@Mixin(PlayerEntity.class)
public class NametagMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void swifttagger$appendTier(CallbackInfoReturnable<Text> cir) {
        ModConfig config = ConfigManager.get();
        if (!config.enabled || !config.showTier || !config.showOnNametag) return;
        if (cir.getReturnValue() == null) return;

        PlayerEntity self = (PlayerEntity) (Object) this;
        String name = self.getGameProfile().name();

        PlayerProfile profile = SwiftTaggerClient.PROFILE_CACHE.get(name);
        if (profile == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        boolean isLocalPlayer = client.player != null && client.player.getUuid().equals(self.getUuid());

        KitResult kitResult = isLocalPlayer
                ? SwiftTaggerClient.KIT_DETECTION.detectLocal(self)
                : SwiftTaggerClient.KIT_DETECTION.detectRemote(self);

        KitId detectedKit = kitResult.kitId();
        String tier = TierResolver.resolve(config, profile, detectedKit);
        if (tier == null) return;

        KitDefinition def = KitDefinitions.byId(detectedKit);
        String kitDisplayName = def != null ? def.name() : "";

        String resolvedMode = TierResolver.resolveMode(config, profile, detectedKit);
        Text icon = KitIcons.iconText(config, resolvedMode);

        cir.setReturnValue(TierTextRenderer.build(config, tier, kitDisplayName, icon, cir.getReturnValue()));
    }
}
