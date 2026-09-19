package com.example.swifttagger.kit;

import java.util.List;
import java.util.Set;

/**
 * Moteur de correspondance par proximité (section 2/5 de la spec).
 *
 * Ne prend jamais 100% des items comme condition (pas de
 * "if allItemsPresent"). Calcule un score pour chaque kit et retient le
 * meilleur, avec un système de départage explicite pour éviter les faux
 * positifs entre kits qui partagent des items (section 3).
 *
 * Pourquoi deux métriques (coverage + rawWeight) plutôt qu'une seule ?
 * ------------------------------------------------------------------
 * Une simple fraction "matched/total" par kit ne suffit PAS : le kit
 * "Sword" n'a que 5 items dans sa définition (armure + épée), donc un
 * joueur qui a l'armure + l'épée ET des potions (donc plutôt un Diamond
 * Pot incomplet) obtient une fraction de 100% pour Sword, ce qui écraserait
 * à tort un Diamond Pot dont la fraction est mécaniquement plus basse
 * (plus d'items attendus). La spec elle-même donne cet exemple en test 3
 * de la section 18 et attend "Diamond Pot", pas "Sword".
 *
 * On utilise donc :
 *  1) rawWeight = somme des poids des items EFFECTIVEMENT présents pour ce
 *     kit -> favorise le kit qui explique le plus d'éléments trouvés
 *     dans l'inventaire (le signal le plus fort).
 *  2) coverage = rawWeight / poids total attendu par le kit -> sert de
 *     garde-fou (seuil minimal en dessous duquel on répond "Unknown") et
 *     de critère de départage secondaire quand deux kits ont exactement le
 *     même rawWeight.
 *  3) priorité configurable -> dernier recours en cas d'égalité parfaite.
 */
public final class KitDetector {

    /** En dessous de ce taux de couverture, le meilleur candidat est ignoré. */
    private double minCoverage = 0.30;

    /** Poids brut minimal absolu, pour ignorer le bruit (1-2 items isolés). */
    private int minRawWeight = 3;

    private List<KitDefinition> kits = KitDefinitions.ALL;

    /** Ordre de priorité en cas d'égalité totale (premier = gagne). Modifiable. */
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

    /**
     * @param presentKeys clés canoniques des items présents (voir ItemWeight),
     *                    typiquement produites par ItemKeyMapper +
     *                    InventoryScanner côté Minecraft. Un Set (pas de
     *                    doublons, quantité déjà ignorée en amont).
     */
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

        // 1) le kit qui explique le plus de poids "gagne" en priorité.
        if (candidate.rawWeight() != current.rawWeight()) {
            return candidate.rawWeight() > current.rawWeight();
        }

        // 2) à égalité de poids brut, on préfère le kit le plus "complet"
        //    (plus proche de sa propre définition).
        if (Math.abs(candidate.coverage() - current.coverage()) > 1e-9) {
            return candidate.coverage() > current.coverage();
        }

        // 3) égalité totale -> priorité configurable (section 5 "Égalités").
        int candidatePriority = priorityIndexOf(candidateDef.id());
        int currentPriority = priorityIndexOf(currentDef.id());
        return candidatePriority < currentPriority;
    }

    private int priorityIndexOf(KitId id) {
        int idx = priorityOrder.indexOf(id);
        if (idx >= 0) {
            return idx;
        }
        // Fallback sur la priorité statique de la définition si le kit
        // n'apparaît pas dans la liste configurée (ex: nouveau kit ajouté
        // sans mettre à jour la priorité).
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
