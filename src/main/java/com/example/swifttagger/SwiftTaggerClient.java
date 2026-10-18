package com.example.swifttagger;

import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.api.TierApiClient;
import com.example.swifttagger.cache.ProfileCache;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierResolver;
import com.example.swifttagger.kit.GamemodeCycle;
import com.example.swifttagger.kit.KitDefinition;
import com.example.swifttagger.kit.KitDefinitions;
import com.example.swifttagger.kit.KitDetectionService;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.kit.KitResult;
import com.example.swifttagger.text.SwiftMessage;
import com.example.swifttagger.text.Messages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SwiftTaggerClient implements ClientModInitializer {

    public static final TierApiClient API_CLIENT = new TierApiClient();
    public static final ProfileCache PROFILE_CACHE = new ProfileCache(API_CLIENT);
    public static final KitDetectionService KIT_DETECTION = new KitDetectionService();

    private static KeyBinding searchNearestKey;
    private static KeyBinding autoDetectKey;
    private static KeyBinding cycleModeKey;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("swifttagger", "general"));

    private int autoDetectTickCounter = 0;
    private static final int AUTO_DETECT_INTERVAL_TICKS = 10;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        applyDetectionConfig();

        ModConfig config = ConfigManager.get();

        searchNearestKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.swifttagger.search_nearest",
                InputUtil.Type.KEYSYM,
                config.searchKeyCode,
                CATEGORY,
                0
        ));

        autoDetectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.swifttagger.auto_detect",
                InputUtil.Type.KEYSYM,
                config.autoDetectKeyCode,
                CATEGORY,
                0
        ));

        cycleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.swifttagger.cycle_mode",
                InputUtil.Type.KEYSYM,
                config.cycleModeKeyCode,
                CATEGORY,
                0
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ChatDisplay.register();
        SwiftCommand.register();
    }

    public static void applyDetectionConfig() {
        ModConfig config = ConfigManager.get();
        KIT_DETECTION.configure(
                config.minMatchCoverage,
                config.minMatchRawWeight,
                ConfigManager.kitPriorityAsIds()
        );
    }

    private void onTick(MinecraftClient client) {
        ModConfig config = ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        while (searchNearestKey.wasPressed()) {
            PlayerEntity nearest = findNearestPlayer(client);
            if (nearest != null) {
                TierChatDisplay.show(nearest.getGameProfile().name());
            } else if (client.player != null) {
                client.player.sendMessage(SwiftMessage.error(Messages.get("action.no_nearby")), true);
            }
        }

        while (autoDetectKey.wasPressed()) {
            runManualDetection(client);
        }

        while (cycleModeKey.wasPressed()) {
            cycleMode(client);
        }

        if (config.autoDetectEnabled && client.player != null) {
            autoDetectTickCounter++;
            if (autoDetectTickCounter >= AUTO_DETECT_INTERVAL_TICKS) {
                autoDetectTickCounter = 0;

                KIT_DETECTION.detectLocal(client.player);
            }
        }
    }

    private void runManualDetection(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        KitResult result = KIT_DETECTION.detectLocal(client.player);

        if (result.isUnknown()) {
            client.player.sendMessage(SwiftMessage.error(Messages.get("action.gamemode_unknown")), true);
            return;
        }

        KitId kitId = result.kitId();
        KitDefinition def = KitDefinitions.byId(kitId);
        String name = def != null ? def.name() : kitId.name();

        ModConfig config = ConfigManager.get();
        if (!config.enabled) {
            return;
        }
        String playerName = client.player.getGameProfile().name();
        PlayerProfile profile = PROFILE_CACHE.get(playerName);

        String tier = null;
        if (profile != null) {
            String modeKey = TierResolver.modeKeyOf(kitId);
            tier = modeKey != null ? profile.tiers.get(modeKey) : null;
        }

        Text actionBarMessage = tier != null
                ? SwiftMessage.forMode(config, kitId, Messages.get("action.gamemode_tier", "mode", name, "tier", tier))
                : SwiftMessage.forMode(config, kitId, Messages.get("action.gamemode_unranked", "mode", name));

        client.player.sendMessage(actionBarMessage, true);
    }

    private void cycleMode(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        ModConfig config = ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        String next = GamemodeCycle.next(config.selectedGamemode);
        config.selectedGamemode = next;
        config.displayedTierMode = "SELECTED";
        ConfigManager.save();

        String label = GamemodeCycle.displayName(next);
        Text actionBarMessage = SwiftMessage.forMode(config, next, label);

        client.player.sendMessage(actionBarMessage, true);
    }

    private PlayerEntity findNearestPlayer(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;
        Vec3d selfPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        PlayerEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            Vec3d otherPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            double distSq = otherPos.squaredDistanceTo(selfPos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }

        final double MAX_DISTANCE = 25.0D;
        if (nearest == null || nearestDistSq > MAX_DISTANCE * MAX_DISTANCE) {
            return null;
        }
        return nearest;
    }
}
