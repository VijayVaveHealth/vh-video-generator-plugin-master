package com.xmartlabs.cordova.frame2video;

import android.support.annotation.NonNull;
import android.util.Log;

enum Action {
    ADD_FRAME("addFrame") {
        @NonNull
        @Override
        public ActionRunner newActionRunner() {
            return new AddFrameActionRunner(ADD_FRAME);
        }
    },
    END("end") {
        @NonNull
        @Override
        public ActionRunner newActionRunner() {
            return new EndActionRunner(END);
        }
    },
    START("start") {
        @NonNull
        @Override
        public ActionRunner newActionRunner() {
            return new StartActionRunner(START);
        }
    },
    ;

    @NonNull
    private final String value;

    Action(@NonNull String value) {
        this.value = value;
    }

    @NonNull
    public abstract ActionRunner newActionRunner();

    @NonNull
    static Action fromValue(@NonNull String value) {
        for (Action action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }

        throw new IllegalArgumentException(String.format("Action '%s' does not exist", value));
    }
}