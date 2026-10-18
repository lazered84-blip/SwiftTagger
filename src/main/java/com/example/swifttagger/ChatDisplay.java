package com.example.swifttagger;

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
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class ChatDisplay {

    private ChatDisplay() {
    }

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            ModConfig config = ConfigManager.get();
            if (!config.enabled || !config.showTier || !config.showOnChat || sender == null) {
                return true;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            String name = sender.name();
            PlayerProfile profile = SwiftTaggerClient.PROFILE_CACHE.get(name);

            if (profile != null) {
                Text tagged = buildTaggedMessage(client, config, name, sender.id(), message, signedMessage, profile);
                if (tagged == null) {
                    return true;
                }
                client.inGameHud.getChatHud().addMessage(tagged);
                return false;
            }

            final Text original = message.copy();

            SwiftTaggerClient.PROFILE_CACHE.getAsync(name).whenComplete((loadedProfile, error) ->
                    client.execute(() -> {
                        if (client.inGameHud == null) return;

                        if (error != null || loadedProfile == null) {

                            client.inGameHud.getChatHud().addMessage(original);
                            return;
                        }

                        Text tagged = buildTaggedMessage(client, config, name, sender.id(), original, null, loadedProfile);
                        client.inGameHud.getChatHud().addMessage(tagged != null ? tagged : original);
                    }));

            return false;
        });
    }

    private static Text buildTaggedMessage(MinecraftClient client, ModConfig config, String name,
                                           java.util.UUID senderUuid, Text message, Object signedMessage,
                                           PlayerProfile profile) {
        if (profile == null) return null;

        KitResult kitResult = KitResult.UNKNOWN;
        if (client.player != null && client.player.getUuid().equals(senderUuid)) {
            kitResult = SwiftTaggerClient.KIT_DETECTION.detectLocal(client.player);
        } else if (client.world != null) {
            PlayerEntity worldPlayer = client.world.getPlayerByUuid(senderUuid);
            if (worldPlayer != null) {
                kitResult = SwiftTaggerClient.KIT_DETECTION.detectRemote(worldPlayer);
            }
        }

        KitId detectedKit = kitResult.kitId();
        String tier = TierResolver.resolve(config, profile, detectedKit);
        if (tier == null) {
            return message.copy();
        }

        String resolvedMode = TierResolver.resolveMode(config, profile, detectedKit);
        KitId resolvedKitId = KitId.fromModeKey(resolvedMode);
        Text icon = KitIcons.iconText(config, resolvedMode);
        int tierColor = TierTextRenderer.resolveTierColor(config, tier);

        String raw = message.getString();
        int separator = raw.indexOf(": ");
        if (separator < 0) {

            return message.copy();
        }

        String serverPrefix = raw.substring(0, separator).stripTrailing();
        String content = raw.substring(separator + 1);

        MutableText result = Text.literal(serverPrefix);
        result.append(Text.literal(" "));
        result.append(TierTextRenderer.buildBadge(config, tier, icon, tierColor));
        result.append(Text.literal(" "));
        result.append(Text.literal(content));
        return result;
    }
}
