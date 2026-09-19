package com.example.swifttagger.screen;

import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.api.TierApiClient;
import com.example.swifttagger.config.ConfigManager;
import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.kit.KitDefinition;
import com.example.swifttagger.kit.KitDefinitions;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import com.example.swifttagger.skin.PlayerHeadCache;
import com.example.swifttagger.text.TierTextRenderer;
import com.example.swifttagger.text.Messages;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carte joueur graphique : ouverte par /tiers, /swift tier et
 * la touche "joueur le plus proche". Affiche le skin du joueur, son pseudo,
 * son classement/points, ses tiers (avec peak en dessous si meilleur), et
 * des boutons "Recharger le profil" / "NameMC".
 */
public class PlayerCardScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("swifttagger-skin");
    private static final int PANEL_COLOR = 0xE0101010;
    // DrawContext attend des couleurs ARGB. Sans le FF alpha, le texte est
    // entièrement transparent (cause du panneau vide observé en jeu).
    private static final int BORDER_COLOR = 0x60FFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int GOLD = 0xFFFFD700;
    private static final int ERROR_RED = 0xFFFF5555;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 12;
    private static final int HEAD_SIZE = 48;
    private static final int PANEL_WIDTH = 260;

    /** Skin par défaut de Minecraft (Steve), utilisé si le skin réel du joueur n'est pas résolvable. */
    private static final SkinTextures FALLBACK_SKIN = DefaultSkinHelper.getSteve();
    private static final Identifier LOGO = Identifier.of("swifttagger", "textures/gui/logo.png");

    private final String targetName;
    private String displayName;
    private PlayerProfile profile;
    private boolean loadFailed = false;
    private String loadError = null;
    private TierApiClient.RankingResult ranking = null;
    private ButtonWidget reloadButton;

    public PlayerCardScreen(String targetName) {
        super(Text.literal(Messages.get("screen.tiers_of", "player", targetName)));
        this.targetName = targetName;
        this.displayName = targetName;
    }

    @Override
    protected void init() {
        PlayerHeadCache.request(targetName);
        requestProfile();

        int left = (this.width - PANEL_WIDTH) / 2;
        int buttonY = 30 + estimatePanelHeight();

        this.reloadButton = ButtonWidget.builder(Text.literal(Messages.get("button.reload")), b -> {
                    SwiftTaggerClient.PROFILE_CACHE.invalidate(targetName);
                    this.profile = null;
                    this.loadFailed = false;
                    this.loadError = null;
                    this.ranking = null;
                    requestProfile();
                })
                .dimensions(left, buttonY, 125, 20)
                .build();
        this.addDrawableChild(this.reloadButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(Messages.get("button.namemc")), b -> {
                    try {
                        Util.getOperatingSystem().open(new URI("https://namemc.com/profile/" + displayName));
                    } catch (Exception ignored) {
                        // Pas de navigateur/accès réseau disponible : simple raccourci de confort, on ignore.
                    }
                })
                .dimensions(left + 135, buttonY, 125, 20)
                .build());
    }

    private void requestProfile() {
        SwiftTaggerClient.PROFILE_CACHE.getAsync(targetName).whenComplete((result, error) -> {
            if (this.client == null) return;
            this.client.execute(() -> {
                if (error != null || result == null) {
                    this.loadFailed = true;
                    this.loadError = Messages.get("profile.not_found");
                    this.ranking = null;
                } else {
                    this.profile = result;
                    // L'API est la source de vérité pour la casse du pseudo.
                    if (result.name != null && !result.name.isBlank()) this.displayName = result.name;
                    PlayerHeadCache.request(this.displayName);
                    this.loadFailed = false;
                    this.loadError = null;
                    // /api/player/<pseudo> fournit les tiers/peaks.
                    // /api/players fournit la liste nécessaire au classement.
                    SwiftTaggerClient.API_CLIENT.fetchOverallRanking(targetName, result)
                            .whenComplete((rank, rankError) -> {
                                if (this.client == null) return;
                                this.client.execute(() -> {
                                    if (rankError == null) this.ranking = rank;
                                    repositionButtons();
                                });
                            });
                }
                repositionButtons();
            });
        });
    }

    /** Replace les boutons sous le panneau une fois sa vraie hauteur connue (profil chargé). */
    private void repositionButtons() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int buttonY = 30 + estimatePanelHeight();
        if (this.reloadButton != null) {
            this.reloadButton.setPosition(left, buttonY);
        }
        this.children().forEach(child -> {
            if (child instanceof ButtonWidget button && button != this.reloadButton
                    && button.getMessage().getString().startsWith("NameMC")) {
                button.setPosition(left + 135, buttonY);
            }
        });
    }

    private int estimatePanelHeight() {
        int base = PADDING * 2 + Math.max(HEAD_SIZE, LINE_HEIGHT * 3);
        // Un joueur sanctionné n'affiche plus la liste des tiers (remplacée
        // par le libellé de sanction, déjà compté dans le bloc rang/points).
        if (profile != null && profile.sanction == null && !profile.tiers.isEmpty()) {
            base += (buildTierRows(ConfigManager.get(), profile).size() + 1) * LINE_HEIGHT + 6;
        }
        return base;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        ModConfig config = ConfigManager.get();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = 30;
        int panelHeight = estimatePanelHeight();

        // Panneau de fond.
        context.fill(left, top, left + PANEL_WIDTH, top + panelHeight, PANEL_COLOR);
        drawBorder(context, left, top, PANEL_WIDTH, panelHeight, BORDER_COLOR);

        // Logo du serveur, centré au-dessus du panneau.
        int logoSize = 20;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO,
                this.width / 2 - logoSize / 2, top - logoSize - 4, 0, 0, logoSize, logoSize, logoSize, logoSize);

        int y = top + PADDING;
        drawCrafatarHead(context, displayName, left + PADDING, y, HEAD_SIZE);

        int textX = left + PADDING + HEAD_SIZE + PADDING;
        int textY = y;

        context.drawText(this.textRenderer, Text.literal(displayName).setStyle(bold(0xFFFFFF)), textX, textY, 0xFFFFFFFF, true);
        textY += LINE_HEIGHT + 4;

        if (profile == null && !loadFailed) {
            context.drawText(this.textRenderer, Text.literal(Messages.get("profile.searching")).setStyle(color(GREY)), textX, textY, GREY, true);
        } else if (loadFailed) {
            context.drawText(this.textRenderer, Text.literal(Messages.get("profile.not_found")).setStyle(color(GREY)), textX, textY, GREY, true);
        } else if (profile.sanction != null) {
            // Sanction active : remplace complètement rang/points/tiers par
            // le libellé de sanction, dans sa couleur fixe.
            Text sanctionLine = Text.literal(profile.sanction.label)
                    .setStyle(bold(profile.sanction.resolveColorRgb(config)));
            context.drawText(this.textRenderer, sanctionLine, textX, textY, GREY, true);
        } else {
            int points = profile.getPointsOrCalculated();
            String rankText;
            if (ranking != null) {
                // Affiche uniquement la position : "#1", jamais "#1 / 6".
                rankText = "#" + ranking.rank();
            } else {
                rankText = profile.rank != null ? ("#" + profile.rank) : "...";
            }
            int rankColor = rankingColor(ranking != null ? ranking.rank() : (profile.rank != null ? profile.rank : 100));
            Text rankLine = Text.literal(Messages.get("profile.rank_label"))
                    .append(Text.literal(rankText).setStyle(color(rankColor)));
            context.drawText(this.textRenderer, rankLine, textX, textY, GREY, true);
            textY += LINE_HEIGHT;
            Text pointsLine = Text.literal(Messages.get("profile.points_label"))
                    .append(Text.literal(String.valueOf(points)).setStyle(color(rankColor)));
            context.drawText(this.textRenderer, pointsLine, textX, textY, GREY, true);
        }

        int rowsY = Math.max(y + HEAD_SIZE, textY + LINE_HEIGHT) + 6;
        if (profile != null && profile.sanction == null && !profile.tiers.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(Messages.get("profile.tiers")).setStyle(bold(0xFFFFFF)), left + PADDING, rowsY, 0xFFFFFFFF, true);
            rowsY += LINE_HEIGHT;
            for (MutableText row : buildTierRows(config, profile)) {
                context.drawText(this.textRenderer, row, left + PADDING, rowsY, 0xFFFFFFFF, true);
                rowsY += LINE_HEIGHT;
            }
        }
    }

    private List<MutableText> buildTierRows(ModConfig config, PlayerProfile profile) {
        List<MutableText> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : ordered(profile.tiers).entrySet()) {
            String modeKey = entry.getKey();
            String tier = entry.getValue();
            if (tier == null || tier.isBlank()) continue;

            KitId kitId = KitId.fromModeKey(modeKey);
            Text icon = KitIcons.iconText(config, kitId);
            int tierColor = TierTextRenderer.resolveTierColor(config, tier);

            // Dans /swift tier, on n'affiche plus le nom du mode (ex. "Sword :").
            // On affiche uniquement l'icône et le tier, dans le même sens que
            // le réglage "Position icône + tier" de la configuration.
            MutableText row = Text.empty();
            boolean iconLeft = "GAUCHE".equalsIgnoreCase(config.badgePosition);
            if (iconLeft && !icon.getString().isEmpty()) {
                row.append(TierTextRenderer.tintIconForDisplay(config, icon, tierColor))
                   .append(Text.literal(" "));
            }
            row.append(Text.literal(tier).setStyle(color(tierColor)));
            if (!iconLeft && !icon.getString().isEmpty()) {
                row.append(Text.literal(" "))
                   .append(TierTextRenderer.tintIconForDisplay(config, icon, tierColor));
            }
            rows.add(row);

            String peak = profile.peaks.get(modeKey);
            if (peak != null && !peak.isBlank() && PlayerProfile.tierScorePublic(peak) > PlayerProfile.tierScorePublic(tier)) {
                int peakColor = TierTextRenderer.resolveTierColor(config, peak);
                MutableText peakRow = Text.literal(Messages.get("profile.peak", "tier", peak)).setStyle(color(GREY));
                rows.add(peakRow);
            }
        }
        return rows;
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

    /**
     * Dessine la tête d'un joueur (visage + calque du chapeau) via mc-heads.net,
     * partagé par la carte de tier et /classement (voir LeaderboardScreen).
     * Fonctionne à partir du pseudo, contrairement à l'ancien essai avec
     * Crafatar par UUID (les UUID stockés en database sont des UUID
     * offline/cracked, pas de vrais UUID Mojang, donc invalides pour Crafatar).
     * Retombe sur Steve tant que la tête n'est pas encore téléchargée/en cache.
     */
    static void drawCrafatarHead(DrawContext context, String name, int x, int y, int size) {
        Identifier texture = name != null ? PlayerHeadCache.get(name) : null;
        if (name != null && !name.isBlank()) PlayerHeadCache.request(name);
        if (texture != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size);
        } else {
            PlayerSkinDrawer.draw(context, FALLBACK_SKIN, x, y, size);
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    /** Couleurs globales du classement, également utilisées pour les points. */
    public static int rankingColor(int rank) {
        if (rank == 1) return 0xFFFFD700; // GOLD
        if (rank == 2) return 0xFFC0C0C0; // SILVER
        if (rank == 3) return 0xFFCD7F32; // BRONZE
        if (rank >= 4 && rank <= 99) return 0xFFAA55FF; // VIOLET #4+
        return 0xFFAAAAAA; // couleur de base #100+
    }

    private static Style color(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static Style bold(int rgb) {
        return color(rgb).withBold(true);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
