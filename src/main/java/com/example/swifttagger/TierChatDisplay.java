package com.example.swifttagger;

import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierResolver;
import com.example.swifttagger.kit.KitDefinition;
import com.example.swifttagger.kit.KitDefinitions;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.text.TierTextRenderer;
import com.example.swifttagger.text.Messages;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TierChatDisplay {
    private static final int NAME_COLOR = 0x06B7FE;
    private static final int GREY = 0xAAAAAA;
    private static final int DARK_GREY = 0x666666;
    private static final int ERROR_RED = 0xFF5555;

    private TierChatDisplay() {}

    public static void show(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || playerName == null || playerName.isBlank()) return;

        client.execute(() -> {
            if (client.player != null) {
                client.setScreen(new com.example.swifttagger.screen.PlayerCardScreen(playerName));
            }
        });
    }

    public static void printProfileFallback(MinecraftClient client, String playerName, PlayerProfile profile) {
        ModConfig config = ConfigManager.get();

        send(client, Text.literal(playerName).setStyle(color(NAME_COLOR)));
        send(client, separator());

        if (profile.sanction != null) {
            send(client, Text.literal(profile.sanction.label).setStyle(boldColor(profile.sanction.resolveColorRgb(config))));
            return;
        }

        send(client, Text.literal(Messages.get("profile.tiers")).setStyle(boldColor(GREY)));
        if (profile.tiers.isEmpty()) {
            send(client, Text.literal(Messages.get("chat.unranked")).setStyle(color(ERROR_RED)));
        } else {
            for (Map.Entry<String, String> entry : ordered(profile.tiers).entrySet()) {
                String tier = entry.getValue();
                if (tier == null || tier.isBlank()) continue;
                send(client, tierRow(config, entry.getKey(), tier, profile.peaks.get(entry.getKey())));
            }
        }

        send(client, separator());
        send(client, Text.literal(Messages.get("chat.rank")).setStyle(boldColor(GREY)));

        int points = profile.getPointsOrCalculated();
        send(client, Text.literal(Messages.get("chat.points", "points", points)).setStyle(color(GREY)));

        SwiftTaggerClient.API_CLIENT.fetchOverallRanking(playerName, profile).whenComplete((ranking, rankError) ->
                client.execute(() -> {
                    if (client.player == null) return;
                    if (rankError == null && ranking != null) {
                        send(client, Text.literal(Messages.get("chat.ranking", "rank", ranking.rank())).setStyle(color(GREY)));
                    } else if (profile.rank != null) {
                        send(client, Text.literal(Messages.get("chat.ranking", "rank", profile.rank)).setStyle(color(GREY)));
                    } else {
                        send(client, Text.literal(Messages.get("chat.rank_na")).setStyle(color(GREY)));
                    }
                }));
    }

    public static MutableText buildTabDisplay(ModConfig config, PlayerProfile profile, KitId detectedKit, Text playerText) {
        String tier = TierResolver.resolve(config, profile, detectedKit);
        if (tier == null) return playerText.copy();

        String resolvedMode = TierResolver.resolveMode(config, profile, detectedKit);
        KitId resolvedKitId = KitId.fromModeKey(resolvedMode);
        KitDefinition def = KitDefinitions.byId(resolvedKitId);
        String kitDisplayName = def != null ? def.name() : "";
        Text icon = KitIcons.iconText(config, resolvedMode);

        return TierTextRenderer.build(config, tier, kitDisplayName, icon, playerText);
    }

    private static MutableText tierRow(ModConfig config, String modeKey, String tier, String peakTier) {
        KitId kitId = KitId.fromModeKey(modeKey);
        KitDefinition def = KitDefinitions.byId(kitId);
        String displayName = def != null ? def.name() : titleCase(modeKey);

        int tierColor = TierTextRenderer.resolveTierColor(config, tier);

        MutableText label = Text.literal(displayName + " : ").setStyle(color(GREY));
        Text icon = KitIcons.iconText(config, kitId);
        if (!icon.getString().isEmpty()) label.append(com.example.swifttagger.text.TierTextRenderer.tintIconForDisplay(config, icon, tierColor)).append(Text.literal(" "));
        label.append(Text.literal(tier).setStyle(color(tierColor)));

        if (peakTier != null && !peakTier.isBlank() && !peakTier.equalsIgnoreCase(tier)) {
            int peakColor = TierTextRenderer.resolveTierColor(config, peakTier);
            label.append(Text.literal(Messages.get("chat.peak_open")).setStyle(color(GREY)));
            label.append(Text.literal(peakTier).setStyle(color(peakColor)));
            label.append(Text.literal(Messages.get("chat.peak_close")).setStyle(color(GREY)));
        }
        return label;
    }

    private static Text separator() {
        return Text.literal(Messages.get("chat.separator")).setStyle(color(DARK_GREY));
    }

    private static Map<String, String> ordered(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        for (KitDefinition def : KitDefinitions.ALL) {
            String key = def.id().name().toLowerCase();
            if (source.containsKey(key)) result.put(key, source.get(key));
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (!result.containsKey(entry.getKey())) result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String titleCase(String modeKey) {
        if (modeKey == null || modeKey.isEmpty()) return modeKey;
        String[] parts = modeKey.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static Style color(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static Style boldColor(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true);
    }

    private static void send(MinecraftClient client, Text text) {
        client.player.sendMessage(text, false);
    }
}
