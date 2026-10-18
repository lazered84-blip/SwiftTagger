package com.example.swifttagger.kit;

import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitDetectionService {

    private final KitDetector detector = new KitDetector();
    private final Map<UUID, Set<String>> lastKeysByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, KitResult> lastResultByPlayer = new ConcurrentHashMap<>();

    public KitDetector detector() {
        return detector;
    }

    public void configure(double minCoverage, int minRawWeight, List<KitId> priorityOrder) {
        detector.withThresholds(minCoverage, minRawWeight);
        if (priorityOrder != null && !priorityOrder.isEmpty()) {
            detector.withPriorityOrder(priorityOrder);
        }

        clear();
    }

    public KitResult detectRemote(PlayerEntity player) {
        if (player == null) {
            return KitResult.UNKNOWN;
        }
        Set<String> keys = InventoryScanner.scanVisibleEquipment(player);
        return detectCached(player.getUuid(), keys);
    }

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
