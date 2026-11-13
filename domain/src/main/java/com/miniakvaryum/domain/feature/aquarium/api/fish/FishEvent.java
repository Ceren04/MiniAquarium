package com.miniakvaryum.domain.feature.aquarium.api.fish;

/** What {@link FishBrain#update} reports back to the world after one frame. */
public enum FishEvent {

    /** Nothing the world has to react to. */
    NONE,

    /** The EAT action ended in this frame; the world must now complete the eating. */
    FINISHED_EATING
}
