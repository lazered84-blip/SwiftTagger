package com.example.swifttagger.kit;

import java.util.List;

/**
 * Définition statique d'un kit (section 14 de la spec : "structure
 * facilement modifiable"). C'était la classe manquante du lot fourni :
 * {@link KitDefinitions} et {@link KitDetector} la référencent mais elle
 * n'existait dans aucun fichier livré, ce qui empêchait toute compilation.
 *
 * @param id                   identifiant interne unique du kit
 * @param name                 nom affiché (utilisé pour {kit} dans le format, section 12)
 * @param aliases              alias purement cosmétiques (section 15) — ne créent jamais de kit séparé
 * @param characteristicItems  items "caractéristiques" (obligatoires dans le calcul du score max, mais
 *                             jamais requis à 100% pour la détection, cf. section 2)
 * @param optionalItems        items qui contribuent au score sans jamais faire partie du dénominateur
 * @param tiePriority          priorité de secours en cas d'égalité totale (plus petit = prioritaire),
 *                             utilisée seulement si le kit n'apparaît pas dans la liste configurable
 *                             de KitDetector#withPriorityOrder (section 5 "Égalités")
 */
public record KitDefinition(
        KitId id,
        String name,
        List<String> aliases,
        List<ItemWeight> characteristicItems,
        List<ItemWeight> optionalItems,
        int tiePriority
) {
    /** Somme des poids des items caractéristiques : dénominateur du coverage (voir KitDetector). */
    public int maxCharacteristicWeight() {
        int sum = 0;
        for (ItemWeight iw : characteristicItems) {
            sum += iw.weight();
        }
        return sum;
    }
}
