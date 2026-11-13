package com.miniakvaryum.domain.feature.tasks.api;

/** Outcome of approving a task. */
public enum ApprovalResult {

    /** The task was pending; it is approved now and the reward was paid. */
    APPROVED,

    /** The task had been approved before; nothing changed and nothing was paid again. */
    ALREADY_APPROVED,

    /** Reserved for callers that prefer a result over an exception; see {@code TaskService.approve}. */
    NOT_PENDING
}
