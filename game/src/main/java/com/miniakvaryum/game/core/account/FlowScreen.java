package com.miniakvaryum.game.core.account;

/** Base of the screens before sign-in (Welcome, Sign up, Sign in, Forgot password). */
abstract class FlowScreen extends AccountScreen {

    protected final AccountFlow flow;

    FlowScreen(AccountFlow flow) {
        super(flow);
        this.flow = flow;
    }
}
