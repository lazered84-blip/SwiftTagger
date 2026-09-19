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

/**
 * Préfixe les messages de chat avec le tier de l'expéditeur.
 *
 * Le point important ici est que la récupération du profil est asynchrone :
 * le premier message d'un joueur arrivait avant que ProfileCache#get() ait
 * terminé sa requête et était donc affiché sans tier. On bloque désormais
 * l'affichage par défaut pendant cette première requête, puis on ajoute le
 * message une fois le profil reçu. En cas d'erreur API, le message original
 * est quand même affiché.
 */
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

            // Première occurrence : get() a déclenché le fetch. On annule
            // l'affichage vanilla et on rejoue le message dès que le profil
            // est disponible, sans demander au joueur de renvoyer son message.
            //
            // BUGFIX : `signedMessage.getSignedContent()` ne renvoie QUE le texte
            // brut tapé par le joueur, sans le préfixe "Pseudo: " ajouté par le
            // serveur — contrairement à `message`, qui est la ligne déjà décorée.
            // En l'utilisant ici, le message affiché (repli en cas d'erreur, ou
            // repassé tel quel dans buildTaggedMessage si aucun ": " n'y est
            // trouvé) perdait le nom du joueur : le message apparaissait dans le
            // tchat sans qu'on sache qui l'avait envoyé. On garde donc toujours
            // la version décorée du serveur.
            final Text original = message.copy();

            SwiftTaggerClient.PROFILE_CACHE.getAsync(name).whenComplete((loadedProfile, error) ->
                    client.execute(() -> {
                        if (client.inGameHud == null) return;

                        if (error != null || loadedProfile == null) {
                            // L'API est indisponible : ne pas perdre le message.
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

        // IMPORTANT : le serveur envoie déjà la ligne complète du chat,
        // notamment le préfixe LuckPerms + pseudo. Ne reconstruisons donc
        // jamais "pseudo + tier" devant le message : cela créait le doublon
        // visible en jeu. On insère uniquement le badge SwiftTagger dans le
        // préfixe existant, juste avant le ":" du message.
        String raw = message.getString();
        int separator = raw.indexOf(": ");
        if (separator < 0) {
            // Fallback : si le serveur n'utilise pas ": ", on conserve
            // intégralement sa ligne et on ajoute seulement le tier à la fin
            // du préfixe impossible à détecter plutôt que de dupliquer le pseudo.
            return message.copy();
        }

        String serverPrefix = raw.substring(0, separator).stripTrailing();
        String content = raw.substring(separator + 1); // conserve le ':'

        MutableText result = Text.literal(serverPrefix);
        result.append(Text.literal(" "));
        result.append(TierTextRenderer.buildBadge(config, tier, icon, tierColor));
        result.append(Text.literal(" "));
        result.append(Text.literal(content));
        return result;
    }
}
