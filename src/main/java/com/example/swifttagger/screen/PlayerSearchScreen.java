package com.example.swifttagger.screen;

import com.example.swifttagger.text.Messages;
import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierColors;
import com.example.swifttagger.kit.KitDefinition;
import com.example.swifttagger.kit.KitDefinitions;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerSearchScreen extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int BORDER_COLOR = 0x60FFFFFF;

    private final String targetName;

    public PlayerSearchScreen(String targetName) {
        super(Text.literal(Messages.get("screen.tiers_of", "player", targetName)));
        this.targetName = targetName;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        PlayerProfile profile = SwiftTaggerClient.PROFILE_CACHE.get(targetName);
        ModConfig config = ConfigManager.get();

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(bold(Text.literal(Messages.get("search.player", "player", targetName)), 0xFFFFFF)));

        if (profile == null) {
            lines.add(new Line(Text.literal(Messages.get("search.loading")).setStyle(color(0xAAAAAA))));
            drawPanel(context, lines);
            return;
        }

        if (profile.rank != null && profile.points != null) {
            lines.add(new Line(Text.literal(Messages.get("search.top", "rank", profile.rank, "points", profile.points))
                    .setStyle(color(0xAAAAAA))));
        }

        if (profile.tiers.isEmpty() && profile.peaks.isEmpty()) {
            lines.add(Line.EMPTY);
            lines.add(new Line(Text.literal(Messages.get("profile.no_tier")).setStyle(color(0xAAAAAA))));
            drawPanel(context, lines);
            return;
        }

        if (!profile.tiers.isEmpty()) {
            lines.add(Line.EMPTY);
            lines.add(new Line(bold(Text.literal(Messages.get("search.tiers")), 0xFFFFFF)));
            appendTierRows(lines, config, profile.tiers);
        }

        if (!profile.peaks.isEmpty()) {
            lines.add(Line.EMPTY);
            lines.add(new Line(bold(Text.literal(Messages.get("search.retired_tiers")), 0xFFFFFF)));
            appendTierRows(lines, config, profile.peaks);
        }

        drawPanel(context, lines);
    }

    private void appendTierRows(List<Line> lines, ModConfig config, Map<String, String> tiersByMode) {
        for (Map.Entry<String, String> entry : tiersByMode.entrySet()) {
            String modeKey = entry.getKey();
            String tier = entry.getValue();

            KitId kitId = KitId.fromModeKey(modeKey);
            Text icon = KitIcons.iconText(config, kitId);
            KitDefinition def = KitDefinitions.byId(kitId);
            String displayName = def != null ? def.name() : titleCase(modeKey);

            int tierColorRgb = com.example.swifttagger.text.TierTextRenderer.resolveTierColor(config, tier);
            if (!config.useTierColor) tierColorRgb = 0xFFD700;

            MutableText row = Text.literal(displayName + " : ").setStyle(color(0xFFD700));
            if (!icon.getString().isEmpty()) {
                row.append(icon.copy().setStyle(icon.getStyle().withColor(TextColor.fromRgb(tierColorRgb)))).append(Text.literal(" "));
            }
            row.append(Text.literal(tier).setStyle(color(tierColorRgb)));

            lines.add(new Line(row));
        }
    }

    private static String titleCase(String modeKey) {
        if (modeKey == null || modeKey.isEmpty()) {
            return modeKey;
        }
        String[] parts = modeKey.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static Style color(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static MutableText bold(MutableText text, int rgb) {
        return text.setStyle(color(rgb).withBold(true));
    }

    private void drawPanel(DrawContext context, List<Line> lines) {
        int maxWidth = 0;
        for (Line line : lines) {
            maxWidth = Math.max(maxWidth, this.textRenderer.getWidth(line.text));
        }

        int panelWidth = maxWidth + PADDING * 2;
        int panelHeight = lines.size() * LINE_HEIGHT + PADDING * 2;
        int left = (this.width - panelWidth) / 2;
        int top = 30;

        context.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
        drawBorderManually(context, left, top, panelWidth, panelHeight, BORDER_COLOR);

        int y = top + PADDING;
        for (Line line : lines) {
            if (!line.text.getString().isEmpty()) {
                context.drawTextWithShadow(this.textRenderer, line.text, left + PADDING, y, 0xFFFFFF);
            }
            y += LINE_HEIGHT;
        }
    }

    private void drawBorderManually(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Line(Text text) {
        private static final Line EMPTY = new Line(Text.literal(""));
    }
}
