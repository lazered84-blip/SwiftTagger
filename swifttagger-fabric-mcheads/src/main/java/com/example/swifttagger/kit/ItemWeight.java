package com.example.swifttagger.kit;
/**
 * Poids d'un item "caractéristique" pour un kit donné.
 *
 * key       : clé canonique de l'item (ex: "diamond_sword", "potion:speed",
 *             "netherite_helmet"). Ne dépend jamais des enchantements, de la
 *             quantité, du niveau/durée de potion ou de la durabilité.
 * weight    : poids de correspondance (voir {@link Weight}).
 * optional  : si vrai, l'item ne compte pas dans le score "maximum possible"
 *             du kit (donc son absence ne pénalise jamais le score), mais
 *             sa présence ajoute un petit bonus au score.
 */
public record ItemWeight(String key, int weight, boolean optional) {
    public static ItemWeight required(String key, int weight) {
        return new ItemWeight(key, weight, false);
    }
    public static ItemWeight optional(String key, int weight) {
        return new ItemWeight(key, weight, true);
    }
    /** Poids "sémantiques" utilisés dans la spec (élevé / moyen / faible). */
    public static final class Weight {
        public static final int HIGH = 3;
        public static final int MEDIUM = 2;
        public static final int LOW = 1;
        private Weight() {
        }
    }
}
