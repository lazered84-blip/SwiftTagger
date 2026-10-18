package com.example.swifttagger.kit;

public record ItemWeight(String key, int weight, boolean optional) {
    public static ItemWeight required(String key, int weight) {
        return new ItemWeight(key, weight, false);
    }
    public static ItemWeight optional(String key, int weight) {
        return new ItemWeight(key, weight, true);
    }

    public static final class Weight {
        public static final int HIGH = 3;
        public static final int MEDIUM = 2;
        public static final int LOW = 1;
        private Weight() {
        }
    }
}
