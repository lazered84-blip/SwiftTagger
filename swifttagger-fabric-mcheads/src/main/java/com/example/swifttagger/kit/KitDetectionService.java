package com.example.swifttagger.kit;

import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point d'entrée unique pour la détection de kit, utilisé par les mixins
 * (nametag/tab) et par SwiftTaggerClient (auto-détection locale / touche
 * dédiée).
 *
 * Évite de rescanner l'inventaire à chaque frame (section 13 de la spec) :
 * un scan complet n'est relancé que si le "signature hash" de l'équipement
 * (en pratique : le Set de clés lui-même) a changé depuis le dernier appel
 * pour ce joueur, sinon on renvoie le dernier résultat en cache.
 */
public final class KitDetectionService {

    private final KitDetector detector = new KitDetector();
    private final Map<UUID, Set<String>> lastKeysByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, KitResult> lastResultByPlayer = new ConcurrentHashMap<>();

    public KitDetector detector() {
        return detector;
    }

    /** Reconfigure le détecteur depuis la config utilisateur (section 11 : sensibilité/seuil/priorité). */
    public void configure(double minCoverage, int minRawWeight, List<KitId> priorityOrder) {
        detector.withThresholds(minCoverage, minRawWeight);
        if (priorityOrder != null && !priorityOrder.isEmpty()) {
            detector.withPriorityOrder(priorityOrder);
        }
        // La config a changé la façon de scorer : on ne peut plus faire confiance au cache.
        clear();
    }

    /** Détection best-effort sur un joueur distant (armure + mains visibles seulement). */
    public KitResult detectRemote(PlayerEntity player) {
        if (player == null) {
            return KitResult.UNKNOWN;
        }
        Set<String> keys = InventoryScanner.scanVisibleEquipment(player);
        return detectCached(player.getUuid(), keys);
    }

    /** Détection complète sur le joueur local (sac entier). */
    public KitResult detectLocal(PlayerEntity player) {
        if (player == null) {
            return KitResult.UNKNOWN;
        }
        Set<String> keys = InventoryScanner.scanFull(player);
        return detectCached(player.getUuid(), keys);
    }

    private KitResult detectCached(UUID uuid, Set<String> keys) {
        Set<String> previous = lastKeysByPlayer.get(uuid);
        if (previous != null && previous.equals(keys)) {
            KitResult cached = lastResultByPlayer.get(uuid);
            if (cached != null) {
                return cached;
            }
        }
        KitResult result = detector.detect(keys);
        lastKeysByPlayer.put(uuid, keys);
        lastResultByPlayer.put(uuid, result);
        return result;
    }

    public void clear() {
        lastKeysByPlayer.clear();
        lastResultByPlayer.clear();
    }
}
