package com.example.swifttagger.skin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerHeadCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("swifttagger-skin");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String URL_TEMPLATE = "https://mc-heads.net/avatar/%s/64";

    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> LAST_FAILURE_AT = new ConcurrentHashMap<>();

    private static final long RETRY_COOLDOWN_MILLIS = 30_000L;

    private PlayerHeadCache() {}

    private static String key(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    public static Identifier get(String name) {
        String key = key(name);
        return key == null || key.isEmpty() ? null : TEXTURES.get(key);
    }

    public static void request(String name) {
        String key = key(name);
        if (key == null || key.isEmpty() || TEXTURES.containsKey(key) || IN_FLIGHT.contains(key)) return;
        Long lastFailure = LAST_FAILURE_AT.get(key);
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < RETRY_COOLDOWN_MILLIS) return;
        if (!IN_FLIGHT.add(key)) return;

        long startedAt = System.currentTimeMillis();
        String encoded = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(URL_TEMPLATE, encoded)))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "SwiftTagger-Fabric-Mod")
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, error) -> {
            IN_FLIGHT.remove(key);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            if (error != null) {
                LOGGER.warn("[SwiftTagger] Tête : échec pour {} après {}ms", name, elapsedMs, error);
                LAST_FAILURE_AT.put(key, System.currentTimeMillis());
                return;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("[SwiftTagger] Tête : HTTP {} pour {} après {}ms", response.statusCode(), name, elapsedMs);
                LAST_FAILURE_AT.put(key, System.currentTimeMillis());
                return;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                LOGGER.warn("[SwiftTagger] Tête : réponse vide pour {} après {}ms", name, elapsedMs);
                LAST_FAILURE_AT.put(key, System.currentTimeMillis());
                return;
            }
            registerTexture(key, name, body, elapsedMs);
        });
    }

    private static void registerTexture(String key, String originalName, byte[] pngBytes, long elapsedMs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(pngBytes));
            client.execute(() -> {
                try {
                    Identifier id = Identifier.of("swifttagger", "head/" + key.replaceAll("[^a-z0-9_.-]", "_"));
                    client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "swifttagger/head/" + key, image));
                    TEXTURES.put(key, id);
                    LAST_FAILURE_AT.remove(key);
                    LOGGER.info("[SwiftTagger] Tête chargée pour {} en {}ms", originalName, elapsedMs);
                } catch (Exception e) {
                    LOGGER.warn("[SwiftTagger] Enregistrement de texture échoué pour {}", originalName, e);
                    image.close();
                }
            });
        } catch (Exception e) {
            LOGGER.warn("[SwiftTagger] Décodage PNG échoué pour {}", originalName, e);
            LAST_FAILURE_AT.put(key, System.currentTimeMillis());
        }
    }
}
