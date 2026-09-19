package com.example.swifttagger.api;

import com.example.swifttagger.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client HTTP tolérant aux différentes formes de réponse de l'API SwiftPvP.
 * Les anciennes versions ne lisaient que quelques champs très précis :
 * résultat : tiers/rang/points restaient vides dès que l'API les plaçait dans
 * un objet "player", "profile", "stats" ou "data".
 */
public class TierApiClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Cache court (10s) de la liste /api/players : /classement et chaque
    // ouverture de /tiers (pour le calcul du rang) tapaient sur la même route
    // en quelques secondes d'intervalle, ce qui doublait inutilement le temps
    // de chargement. Un TTL court garde les données fraîches sans refaire un
    // aller-retour HTTP + parsing complet à chaque fois.
    private volatile List<JsonElement> cachedPlayerList = null;
    private volatile long cachedPlayerListAtMillis = 0L;
    private static final long PLAYER_LIST_TTL_MILLIS = 10_000L;

    public CompletableFuture<PlayerProfile> fetchByName(String playerName) {
        String baseUrl = ConfigManager.get().apiBaseUrl;
        if (baseUrl == null || baseUrl.isBlank() || playerName == null || playerName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        String base = baseUrl.replaceAll("/$", "");
        String encoded = URLEncoder.encode(playerName.trim(), StandardCharsets.UTF_8);

        // API SwiftTagger : route principale exacte. Le second chemin reste
        // uniquement comme compatibilité avec une ancienne installation.
        String[] paths = {"/api/player/" + encoded, "/api/players/" + encoded};
        return fetchProfile(base, paths, 0);
    }

    /**
     * Recherche par UUID plutôt que par pseudo, via /api/link/{uuid}.
     * Utile quand seul l'UUID est connu de façon fiable (le pseudo peut avoir
     * changé, ou être absent de la TAB list au moment de l'appel).
     */
    public CompletableFuture<PlayerProfile> fetchByUuid(UUID uuid) {
        String baseUrl = ConfigManager.get().apiBaseUrl;
        if (baseUrl == null || baseUrl.isBlank() || uuid == null) {
            return CompletableFuture.completedFuture(null);
        }
        String base = baseUrl.replaceAll("/$", "");
        String[] paths = {"/api/link/" + uuid};
        return fetchProfile(base, paths, 0);
    }

    private CompletableFuture<PlayerProfile> fetchProfile(String base, String[] paths, int index) {
        if (index >= paths.length) return CompletableFuture.completedFuture(null);
        return get(base + paths[index]).thenCompose(response -> {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                PlayerProfile profile = parseProfile(response.body());
                if (profile != null) return CompletableFuture.completedFuture(profile);
            }
            return fetchProfile(base, paths, index + 1);
        }).exceptionallyCompose(ex -> fetchProfile(base, paths, index + 1));
    }


    /** Liste canonique de tous les joueurs ayant au moins un tier. Utilisée par classement et autocomplétion. */
    public CompletableFuture<List<PlayerProfile>> fetchAllProfiles() {
        String baseUrl = ConfigManager.get().apiBaseUrl;
        if (baseUrl == null || baseUrl.isBlank()) return CompletableFuture.completedFuture(List.<PlayerProfile>of());
        String base = baseUrl.replaceAll("/$", "");
        return fetchPlayerList(base, 0).thenApply(players -> {
            if (players == null) return List.<PlayerProfile>of();
            List<PlayerProfile> result = new ArrayList<>();
            for (JsonElement element : players) {
                if (!element.isJsonObject()) continue;
                PlayerProfile p = parseProfile(element.getAsJsonObject());
                if (p != null && p.name != null && !p.name.isBlank() && !p.tiers.isEmpty()) result.add(p);
            }
            result.sort(Comparator.comparingInt(PlayerProfile::getPointsOrCalculated).reversed()
                    .thenComparing(p -> p.name.toLowerCase(Locale.ROOT)));
            return result;
        }).exceptionally(ex -> List.<PlayerProfile>of());
    }

    public CompletableFuture<RankingResult> fetchOverallRanking(String playerName, PlayerProfile currentProfile) {
        String baseUrl = ConfigManager.get().apiBaseUrl;
        if (baseUrl == null || baseUrl.isBlank()) return CompletableFuture.completedFuture(fallbackRanking(currentProfile));

        String base = baseUrl.replaceAll("/$", "");
        return fetchPlayerList(base, 0).thenApply(players -> {
            if (players == null || players.isEmpty()) return fallbackRanking(currentProfile);

            List<ScoreRow> scores = new ArrayList<>();
            String wanted = normalizeName(playerName);
            boolean found = false;

            for (JsonElement element : players) {
                if (!element.isJsonObject()) continue;
                PlayerProfile p = parseProfile(element.getAsJsonObject());
                if (p == null || p.name == null || p.name.isBlank()) continue;
                int points = p.getPointsOrCalculated();
                scores.add(new ScoreRow(p.name, points));
                if (normalizeName(p.name).equals(wanted)) found = true;
            }

            if (!found && currentProfile != null) {
                scores.add(new ScoreRow(playerName, currentProfile.getPointsOrCalculated()));
            }
            if (scores.isEmpty()) return fallbackRanking(currentProfile);

            scores.sort(Comparator.comparingInt(ScoreRow::points).reversed()
                    .thenComparing(r -> r.name().toLowerCase(Locale.ROOT)));

            int wantedPoints = currentProfile != null ? currentProfile.getPointsOrCalculated()
                    : scores.stream().filter(r -> normalizeName(r.name()).equals(wanted))
                    .mapToInt(ScoreRow::points).findFirst().orElse(0);

            int rank = 1;
            for (ScoreRow row : scores) if (row.points() > wantedPoints) rank++;
            return new RankingResult(rank, scores.size(), wantedPoints);
        }).exceptionally(ex -> fallbackRanking(currentProfile));
    }

    private static RankingResult fallbackRanking(PlayerProfile profile) {
        if (profile == null) return null;
        return profile.rank == null ? null
                : new RankingResult(profile.rank, 0, profile.getPointsOrCalculated());
    }

    private CompletableFuture<List<JsonElement>> fetchPlayerList(String base, int index) {
        long age = System.currentTimeMillis() - cachedPlayerListAtMillis;
        if (cachedPlayerList != null && age < PLAYER_LIST_TTL_MILLIS) {
            return CompletableFuture.completedFuture(cachedPlayerList);
        }

        // L'API SwiftTagger expose la liste canonique ici. Les anciennes routes
        // n'existent pas sur ton serveur et provoquaient des attentes inutiles.
        String[] paths = {"/api/players"};
        if (index >= paths.length) return CompletableFuture.completedFuture(null);

        return get(base + paths[index])
                .orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .handle((response, error) -> {
                    if (error != null || response == null) return null;
                    if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                    List<JsonElement> parsed = parsePlayerList(response.body());
                    if (parsed != null) {
                        cachedPlayerList = parsed;
                        cachedPlayerListAtMillis = System.currentTimeMillis();
                    }
                    return parsed;
                });
    }

    private CompletableFuture<HttpResponse<String>> get(String url) {
        try {
            return httpClient.sendAsync(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private PlayerProfile parseProfile(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            return parseProfile(root.getAsJsonObject());
        } catch (Exception ignored) {
            return null;
        }
    }

    private PlayerProfile parseProfile(JsonObject raw) {
        try {
            JsonObject data = unwrapProfile(raw);
            String uuidString = firstStringDeep(data, "uuid", "id");
            UUID uuid = null;
            if (uuidString != null) {
                try { uuid = UUID.fromString(uuidString); } catch (IllegalArgumentException ignored) { }
            }

            String name = firstStringDeep(data, "name", "pseudo", "username", "playerName", "player");
            if (name == null && raw != data) name = firstStringDeep(raw, "name", "pseudo", "username", "playerName");

            PlayerProfile profile = new PlayerProfile(uuid, name);
            profile.fetchedAtMillis = System.currentTimeMillis();

            readMapsDeep(data, profile.tiers, "tiers", "currentTiers", "current_tiers", "ranks");
            readMapsDeep(data, profile.peaks, "peaks", "peakTiers", "peak_tiers", "retiredTiers", "retired_tiers");
            readArraysDeep(data, profile.tiers, "tiers", "currentTiers", "current_tiers", "ranks");
            readArraysDeep(data, profile.peaks, "peaks", "peakTiers", "peak_tiers", "retiredTiers", "retired_tiers");

            profile.rank = firstIntDeep(data, "rank", "ranking", "position", "leaderboardRank", "overallRank", "top");
            profile.points = firstIntDeep(data, "points", "score", "overallPoints", "totalPoints", "profilePoints", "total");

            // Certaines APIs mettent les stats dans un sous-objet séparé.
            JsonObject stats = object(data, "stats", "statistics", "profile");
            if (stats != null) {
                if (profile.rank == null) profile.rank = firstIntDeep(stats, "rank", "ranking", "position", "top");
                if (profile.points == null) profile.points = firstIntDeep(stats, "points", "score", "totalPoints", "overallPoints");
                readMapsDeep(stats, profile.tiers, "tiers", "currentTiers", "ranks");
                readMapsDeep(stats, profile.peaks, "peaks", "peakTiers", "retiredTiers");
            }

            profile.sanction = parseSanction(data);
            if (profile.sanction == null && raw != data) {
                profile.sanction = parseSanction(raw);
            }
            return profile;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Sanction active éventuelle (RESTREINT/CHEATER/BLACKLIST). Tolérant à
     * plusieurs formes de réponse, comme le reste du parsing :
     *  - un sous-objet "sanction"/"restriction" avec un champ "type"/"status" ;
     *  - directement un champ texte "sanction"/"sanctionType"/"banType" à la racine.
     * Toute valeur qui ne correspond à aucun {@link SanctionType} connu est
     * ignorée (considérée comme "pas de sanction"), jamais une erreur.
     */
    private static SanctionType parseSanction(JsonObject data) {
        JsonObject sanctionObj = object(data, "sanction", "restriction", "moderation", "punishment");
        if (sanctionObj != null) {
            SanctionType fromObject = SanctionType.fromString(
                    firstStringDeep(sanctionObj, "type", "status", "label", "name"));
            if (fromObject != null) return fromObject;
        }
        return SanctionType.fromString(firstStringDeep(data,
                "sanction", "sanctionType", "sanction_type", "banType", "ban_type", "restriction"));
    }

    private static JsonObject unwrapProfile(JsonObject root) {
        JsonObject current = root;
        for (int i = 0; i < 3; i++) {
            JsonObject next = object(current, "data", "player", "profile", "result");
            if (next == null) break;
            // Ne pas entrer dans "profile" si l'objet courant contient déjà les données utiles.
            if (hasAny(current, "tiers", "name", "pseudo", "username", "points", "rank")) break;
            current = next;
        }
        return current;
    }

    private static List<JsonElement> parsePlayerList(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonArray()) return elements(parsed.getAsJsonArray());
            if (!parsed.isJsonObject()) return null;
            JsonObject root = parsed.getAsJsonObject();
            for (String key : new String[]{"players", "profiles", "rankings", "leaderboard", "data", "results"}) {
                JsonElement value = root.get(key);
                if (value != null && value.isJsonArray()) return elements(value.getAsJsonArray());
                if (value != null && value.isJsonObject()) {
                    JsonObject nested = value.getAsJsonObject();
                    for (String inner : new String[]{"players", "profiles", "entries", "items", "data"}) {
                        JsonElement a = nested.get(inner);
                        if (a != null && a.isJsonArray()) return elements(a.getAsJsonArray());
                    }
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<JsonElement> elements(JsonArray array) {
        List<JsonElement> out = new ArrayList<>();
        for (JsonElement e : array) out.add(e);
        return out;
    }

    private static void readMapsDeep(JsonObject root, Map<String,String> target, String... fields) {
        for (String field : fields) {
            JsonElement e = root.get(field);
            if (e == null || !e.isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> row : e.getAsJsonObject().entrySet()) {
                if (row.getValue() != null && !row.getValue().isJsonNull()) {
                    String tier = scalarString(row.getValue());
                    if (tier != null) target.put(normalizeMode(row.getKey()), tier);
                }
            }
        }
    }

    private static void readArraysDeep(JsonObject root, Map<String,String> target, String... fields) {
        for (String field : fields) {
            JsonElement e = root.get(field);
            if (e == null || !e.isJsonArray()) continue;
            for (JsonElement rowElement : e.getAsJsonArray()) {
                if (!rowElement.isJsonObject()) continue;
                JsonObject row = rowElement.getAsJsonObject();
                String mode = firstStringDeep(row, "mode", "method", "gamemode", "kit", "name", "category");
                String tier = firstStringDeep(row, "tier", "rank", "value", "currentTier");
                if (mode != null && tier != null) target.put(normalizeMode(mode), tier);
            }
        }
    }

    private static JsonObject object(JsonObject root, String... keys) {
        for (String key : keys) {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonObject()) return e.getAsJsonObject();
        }
        return null;
    }

    private static boolean hasAny(JsonObject root, String... keys) {
        for (String key : keys) if (root.has(key)) return true;
        return false;
    }

    private static String firstStringDeep(JsonObject root, String... keys) {
        for (String key : keys) {
            JsonElement e = root.get(key);
            String value = scalarString(e);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static Integer firstIntDeep(JsonObject root, String... keys) {
        for (String key : keys) {
            JsonElement e = root.get(key);
            if (e == null || e.isJsonNull()) continue;
            try { return e.getAsInt(); } catch (Exception ignored) { }
            try { return Integer.parseInt(e.getAsString().trim()); } catch (Exception ignored) { }
        }
        return null;
    }

    private static String scalarString(JsonElement e) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return null;
        try { return e.getAsString(); } catch (Exception ignored) { return null; }
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return "";
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "diamond pot", "diamondpot" -> "diamond_pot";
            case "netherite pot", "netheritepot", "neth pot", "netherite potion" -> "netherite_pot";
            default -> mode.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        };
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public record RankingResult(int rank, int totalPlayers, int points) {}
    private record ScoreRow(String name, int points) {}
}
