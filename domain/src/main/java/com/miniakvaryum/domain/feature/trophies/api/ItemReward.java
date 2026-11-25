package com.miniakvaryum.domain.feature.trophies.api;

import java.util.Objects;

/**
 * A thing: decoration, accessory, fish part or whole fish. Until the inventory exists (V11) it is
 * only remembered as earned ({@link TrophyService#earnedItems()}).
 *
 * @param itemId id in the shared item catalog: {@code deco.*}, {@code acc.*},
 *               {@code part.<set>.<head|body|fin|tail>} or {@code fish.*} (draft ids, to be
 *               agreed with V11)
 */
public record ItemReward(String itemId, ItemKind kind) implements Reward {

    public ItemReward {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(kind, "kind");
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is blank");
        }
    }
}
