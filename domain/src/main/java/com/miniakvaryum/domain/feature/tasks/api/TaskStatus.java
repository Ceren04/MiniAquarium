package com.miniakvaryum.domain.feature.tasks.api;

/**
 * Life cycle of a task:
 * <pre>
 *   ACTIVE --reportDone--> PENDING --approve--> APPROVED
 *                             |
 *                             +-----reject----> ACTIVE
 * </pre>
 */
public enum TaskStatus {

    /** Created by a parent, waiting for the child to do it. */
    ACTIVE,

    /** The child reported it as done; waiting for a parent to check. */
    PENDING,

    /** A parent confirmed it; the reward has been paid. Final state. */
    APPROVED
}
