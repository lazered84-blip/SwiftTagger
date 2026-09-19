package com.example.swifttagger.cache;

import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.api.TierApiClient;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache client des profils par nom de joueur.
 * Les appels API sont asynchrones et dédupliqués.
 */
public class ProfileCache {

    private static final long CACHE_DURATION_MILLIS = 5 * 60 * 1000;

    private final TierApiClient apiClient;

    public ProfileCache() {
        this(new TierApiClient());
    }

    public ProfileCache(TierApiClient apiClient) {
        this.apiClient = apiClient;
    }
    private final Map<String, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final Map<String, CompletableFuture<PlayerProfile>> pendingFutures = new ConcurrentHashMap<>();

    /**
     * Renvoie immédiatement le profil en cache et déclenche un fetch si nécessaire.
     * Compatible avec les anciens appelants de rendu.
     */
    public PlayerProfile get(String playerName) {
        String key = key(playerName);
        PlayerProfile cached = cache.get(key);
        boolean valid = cached != null
                && (System.currentTimeMillis() - cached.fetchedAtMillis) <= CACHE_DURATION_MILLIS;
        if (!valid) {
            getAsync(playerName);
        }
        return cached;
    }

    /**
     * Récupère un profil et prévient l'appelant quand l'API a répondu.
     * Indispensable pour /tiers : on ne doit plus afficher une carte vide
     * simplement parce que la requête HTTP n'était pas encore terminée.
     */
    public CompletableFuture<PlayerProfile> getAsync(String playerName) {
        String key = key(playerName);
        PlayerProfile cached = cache.get(key);
        if (cached != null
                && (System.currentTimeMillis() - cached.fetchedAtMillis) <= CACHE_DURATION_MILLIS) {
            return CompletableFuture.completedFuture(cached);
        }

        return pendingFutures.computeIfAbsent(key, name -> {
            CompletableFuture<PlayerProfile> future = apiClient.fetchByName(playerName);
            future.whenComplete((profile, error) -> {
                if (profile != null) {
                    // La clé demandée peut être en minuscules, alors que l'API
                    // renvoie le pseudo officiel. On garde les deux alias pour
                    // éviter une seconde requête et conserver UUID + skin.
                    cache.put(name, profile);
                    if (profile.name != null && !profile.name.isBlank()) {
                        cache.put(key(profile.name), profile);
                    }
                }
                pending.remove(name);
                pendingFutures.remove(name);
            });
            pending.add(name);
            return future;
        });
    }

    public void clear() {
        cache.clear();
    }


    private static String key(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    /** Force le prochain getAsync() à refaire un appel API (bouton "recharger le profil"). */
    public void invalidate(String playerName) {
        cache.remove(key(playerName));
        pendingFutures.remove(key(playerName));
        pending.remove(key(playerName));
    }
}
