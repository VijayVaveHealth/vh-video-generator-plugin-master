package com.xmartlabs.cordova.frame2video;

import android.support.annotation.NonNull;

class ActionRunnerFactory {

    @NonNull
    static ActionRunner newActionRunner(@NonNull String action) {
        return Action.fromValue(action).newActionRunner();
    }

    private ActionRunnerFactory() {}
}
