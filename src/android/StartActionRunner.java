package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

class StartActionRunner implements ActionRunner {

    private static final int OPTIONS_ARG_INDEX = 0;

    private static final String VIDEO_FILE_NAME_ARG_NAME = "videoFileName";
    private static final String WIDTH_ARG_NAME = "width";
    private static final String HEIGHT_ARG_NAME = "height";
    private static final String TIMESTAMP_ARG_NAME = "timestamp";
    private static final String BIT_RATE_ARG_NAME = "bit_rate";
    private static final String FRAME_RATE_ARG_NAME = "frame_rate";

    @NonNull
    private final Action action;

    StartActionRunner(@NonNull Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This action will create a new context based on initial parameters -{@code args}- received from Cordova.
     * This action must be the first one invoked or right after {@link Action#END} has been called. Therefore,
     * no context is expected at this point, a {@code null} value must be passed in for it.</p>
     */
    @Nullable
    @Override
    public void run(@NonNull CordovaPlugin plugin, @NonNull Context appContext, @NonNull CallbackContext callbackContext,
                    @Nullable ActionContext context, @NonNull JSONArray args, @NonNull ActionRunnerCallback callback) throws JSONException, IOException {
        if (context != null) {
            callbackContext.error(
                Error.CONTEXT_ALREADY_INITIALIZED.getErrorMessage("Can't call '%s' action again.", action));
            callback.call(context);
            return;
        }
        if (args.length() == 0) {
            callbackContext.error(
                Error.INVALID_NUMBER_OF_ARGUMENTS.getErrorMessage("Expected 1, but got %d.", args.length()));
            callback.call(null);
            return;
        }

        JSONObject options = args.getJSONObject(OPTIONS_ARG_INDEX);
        String videoFileName = options.getString(VIDEO_FILE_NAME_ARG_NAME);
        ActionContext actionContext = new ActionContext(
                appContext.getFilesDir().getAbsolutePath() + File.separator + videoFileName,
                options.getInt(WIDTH_ARG_NAME),
                options.getInt(HEIGHT_ARG_NAME),
                options.getInt(TIMESTAMP_ARG_NAME));

        if (options.has(BIT_RATE_ARG_NAME)) {
            actionContext.setBitRate(options.getInt(BIT_RATE_ARG_NAME));
        }

        if (options.has(FRAME_RATE_ARG_NAME)) {
            actionContext.setFrameRate(options.getInt(FRAME_RATE_ARG_NAME));
        }

        actionContext.start();

        callbackContext.success();
        callback.call(actionContext);
    }
}
