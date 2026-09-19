package com.example.swifttagger;

import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.SwiftTaggerConfigScreen;
import com.example.swifttagger.kit.GamemodeCycle;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.text.SwiftMessage;
import com.example.swifttagger.text.Messages;
import com.example.swifttagger.screen.LeaderboardScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/**
 * /swift tier <pseudo>  et /swift tiers <pseudo> : carte tiers d'un joueur
 *  - /swift toggle / config / kit restent disponibles.
 *  - /swift toggle                                      : active/désactive le mod
 *  - /swift config                                      : ouvre l'écran de config
 *  - /swift kit <mode>     ou  /kit <mode>              : choisit le mode affiché
 */
public final class SwiftCommand {
    private SwiftCommand() {}

    // Suggestions affichées (première lettre en majuscule, UHC/SMP inchangés) ;
    // la valeur réellement stockée est de toute façon remise en majuscules
    // par selectKit(), donc la casse affichée ici est purement cosmétique.
    private static final String[] KIT_ARG_VALUES = buildKitArgValues();

    private static String[] buildKitArgValues() {
        String[] values = new String[GamemodeCycle.ORDER.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = GamemodeCycle.displayName(GamemodeCycle.ORDER[i]);
        }
        return values;
    }

    private static final SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS =
            (context, builder) -> SwiftTaggerClient.API_CLIENT.fetchAllProfiles().thenApply(players -> {
                String remaining = builder.getRemainingLowerCase();
                for (var profile : players) {
                    if (profile.name != null && profile.name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                        builder.suggest(profile.name);
                    }
                }
                return builder.build();
            });

    private static final SuggestionProvider<FabricClientCommandSource> KIT_SUGGESTIONS =
            (context, builder) -> {
                for (String value : KIT_ARG_VALUES) {
                    builder.suggest(value);
                }
                return builder.buildFuture();
            };

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommandManager.literal("swift")
                    .then(ClientCommandManager.literal("tier")
                            .then(ClientCommandManager.argument(Messages.get("command.player_argument"), StringArgumentType.word())
                                    .suggests(PLAYER_SUGGESTIONS)
                                    .executes(ctx -> showPlayer(StringArgumentType.getString(ctx, Messages.get("command.player_argument"))))))
                    .then(ClientCommandManager.literal("tiers")
                            .then(ClientCommandManager.argument(Messages.get("command.player_argument"), StringArgumentType.word())
                                    .suggests(PLAYER_SUGGESTIONS)
                                    .executes(ctx -> showPlayer(StringArgumentType.getString(ctx, Messages.get("command.player_argument"))))))
                    .then(ClientCommandManager.literal("toggle").executes(ctx -> toggle()))
                    .then(ClientCommandManager.literal("config").executes(ctx -> openConfig()))
                    .then(ClientCommandManager.literal("classement").executes(ctx -> openLeaderboard()))
                    .then(ClientCommandManager.literal("kit")
                            .then(ClientCommandManager.argument(Messages.get("command.mode_argument"), StringArgumentType.word())
                                    .suggests(KIT_SUGGESTIONS)
                                    .executes(ctx -> selectKit(StringArgumentType.getString(ctx, Messages.get("command.mode_argument")))))));

            dispatcher.register(ClientCommandManager.literal("kit")
                    .then(ClientCommandManager.argument(Messages.get("command.mode_argument"), StringArgumentType.word())
                            .suggests(KIT_SUGGESTIONS)
                            .executes(ctx -> selectKit(StringArgumentType.getString(ctx, Messages.get("command.mode_argument"))))));
        });
    }

    private static int openLeaderboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new LeaderboardScreen(client.currentScreen)));
        return 1;
    }

    private static int showPlayer(String name) {
        // "me" n'est plus un alias spécial : les commandes attendent un vrai pseudo.
        if (name == null || name.isBlank() || name.equalsIgnoreCase("me")) return 0;
        // Pas de message de chat à l'ouverture : la carte s'ouvre directement.
        TierChatDisplay.show(name);
        return 1;
    }

    private static int showSelf() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        String name = client.player.getGameProfile().name();
        feedbackInfo(Messages.get("command.open_own_profile", "player", name));
        TierChatDisplay.show(name);
        return 1;
    }

    private static int reload() {
        ConfigManager.load();
        SwiftTaggerClient.applyDetectionConfig();
        SwiftTaggerClient.PROFILE_CACHE.clear();
        feedbackSuccess(Messages.get("command.config_reloaded"));
        return 1;
    }

    private static int toggle() {
        ModConfig config = ConfigManager.get();
        config.enabled = !config.enabled;
        ConfigManager.save();
        if (config.enabled) {
            feedbackSuccess(Messages.get("command.enabled"));
        } else {
            feedbackError(Messages.get("command.disabled"));
        }
        return 1;
    }

    private static int openConfig() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(SwiftTaggerConfigScreen.create(client.currentScreen)));
        return 1;
    }

    private static int selectKit(String modeArg) {
        String normalized = GamemodeCycle.canonicalize(modeArg);
        boolean valid = normalized.equals("AUCUN") || normalized.equals("DETECTION");
        if (!valid) {
            try {
                valid = KitId.valueOf(normalized) != KitId.UNKNOWN;
            } catch (IllegalArgumentException ignored) {
                valid = false;
            }
        }
        if (!valid) {
            feedbackError(Messages.get("command.mode_unknown", "mode", modeArg, "values", String.join(", ", KIT_ARG_VALUES)));
            return 0;
        }

        ModConfig config = ConfigManager.get();
        config.selectedGamemode = normalized;
        config.displayedTierMode = "SELECTED";
        ConfigManager.save();
        // Même message que la touche "changer de mode" (cycleMode) : juste le
        // libellé du mode avec son icône, sans préfixe "Mode sélectionné :".
        feedbackMode(config, normalized, GamemodeCycle.displayName(normalized));
        return 1;
    }

    private static void feedbackSuccess(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(SwiftMessage.success(message), true);
        }
    }

    private static void feedbackError(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(SwiftMessage.error(message), true);
        }
    }

    private static void feedbackInfo(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(SwiftMessage.info(message), true);
        }
    }

    /** Comme feedbackSuccess(), mais avec l'icône du mode {@code mode} à la place du ✅ générique. */
    private static void feedbackMode(ModConfig config, String mode, String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(SwiftMessage.forMode(config, mode, message), true);
        }
    }
}