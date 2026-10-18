package com.example.swifttagger.kit;

import java.util.List;

public record KitDefinition(
        KitId id,
        String name,
        List<String> aliases,
        List<ItemWeight> characteristicItems,
        List<ItemWeight> optionalItems,
        int tiePriority
) {

    public int maxCharacteristicWeight() {
        int sum = 0;
        for (ItemWeight iw : characteristicItems) {
            sum += iw.weight();
        }
        return sum;
    }
}
