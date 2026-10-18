package com.example.swifttagger.kit;

import java.util.List;
import java.util.Set;

public final class KitDetector {

    private double minCoverage = 0.30;

    private int minRawWeight = 3;

    private List<KitDefinition> kits = KitDefinitions.ALL;

    private List<KitId> priorityOrder = List.of(
            KitId.MACE, KitId.SMP, KitId.NETHERITE_POT, KitId.AXE,
            KitId.UHC, KitId.DIAMOND_POT, KitId.SWORD, KitId.VANILLA
    );

    public KitDetector() {
    }

    public KitDetector withKits(List<KitDefinition> kits) {
        this.kits = kits;
        return this;
    }

    public KitDetector withThresholds(double minCoverage, int minRawWeight) {
        this.minCoverage = minCoverage;
        this.minRawWeight = minRawWeight;
        return this;
    }

    public KitDetector withPriorityOrder(List<KitId> order) {
        this.priorityOrder = order;
        return this;
    }

    public double minCoverage() {
        return minCoverage;
    }

    public int minRawWeight() {
        return minRawWeight;
    }

    public List<KitId> priorityOrder() {
        return priorityOrder;
    }

    public KitResult detect(Set<String> presentKeys) {
        if (presentKeys == null || presentKeys.isEmpty()) {
            return KitResult.UNKNOWN;
        }

        KitResult best = KitResult.UNKNOWN;
        KitDefinition bestDef = null;

        for (KitDefinition def : kits) {
            KitResult result = score(def, presentKeys);

            if (isBetter(result, def, best, bestDef)) {
                best = result;
                bestDef = def;
            }
        }

        if (best.isUnknown()
                || best.coverage() < minCoverage
                || best.rawWeight() < minRawWeight) {
            return KitResult.UNKNOWN;
        }

        return best;
    }

    private boolean isBetter(KitResult candidate, KitDefinition candidateDef,
                              KitResult current, KitDefinition currentDef) {
        if (currentDef == null) {
            return candidate.rawWeight() > 0;
        }

        if (candidate.rawWeight() != current.rawWeight()) {
            return candidate.rawWeight() > current.rawWeight();
        }

        if (Math.abs(candidate.coverage() - current.coverage()) > 1e-9) {
            return candidate.coverage() > current.coverage();
        }

        int candidatePriority = priorityIndexOf(candidateDef.id());
        int currentPriority = priorityIndexOf(currentDef.id());
        return candidatePriority < currentPriority;
    }

    private int priorityIndexOf(KitId id) {
        int idx = priorityOrder.indexOf(id);
        if (idx >= 0) {
            return idx;
        }

        KitDefinition def = KitDefinitions.byId(id);
        return def != null ? 100 + def.tiePriority() : 999;
    }

    private KitResult score(KitDefinition def, Set<String> presentKeys) {
        int matchedWeight = 0;

        for (ItemWeight iw : def.characteristicItems()) {
            if (presentKeys.contains(iw.key())) {
                matchedWeight += iw.weight();
            }
        }

        for (ItemWeight iw : def.optionalItems()) {
            if (presentKeys.contains(iw.key())) {
                matchedWeight += iw.weight();
            }
        }

        int denominator = def.maxCharacteristicWeight();
        double coverage = denominator == 0 ? 0.0 : Math.min(1.0, (double) matchedWeight / denominator);

        return new KitResult(def.id(), coverage, matchedWeight);
    }
}
