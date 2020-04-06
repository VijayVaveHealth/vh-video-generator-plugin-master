package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class AddFrameActionRunner implements ActionRunner {

    private static final int DATA_ARG_INDEX = 0;
    private static final int OPTIONS_ARG_INDEX = 1;

    private static final String TIMESTAMP_ARG_NAME = "timestamp";
    private static final String TYPE_ARG_NAME = "type";

    @NonNull
    private final Action action;

    AddFrameActionRunner(@NonNull Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Encodes data for a new frame in the action context. This action must be invoked after {@link Action#START}
     * was invoked and before invoking {@link Action#END}. Therefore {@code context} can't be {@code null}.</p>
     */
    @Nullable
    @Override
    public void run(@NonNull CordovaPlugin plugin, @NonNull Context appContext, @NonNull CallbackContext callbackContext,
                    @Nullable ActionContext context, @NonNull JSONArray args, @NonNull ActionRunnerCallback callback) throws JSONException, IOException {
        if (context == null) {
            callbackContext.error(Error.CONTEXT_NOT_INITIALIZED
                .getErrorMessage("Context must be created before calling '%s' action", action));
            callback.call(null);
            return;
        }
        if (args.length() < 2) {
            callbackContext.error(
                Error.INVALID_NUMBER_OF_ARGUMENTS.getErrorMessage("Expected 2, but got %d.", args.length()));
            callback.call(context);
            return;
        }
        if (!(args.get(DATA_ARG_INDEX) instanceof String)) {
            callbackContext.error(Error.INVALID_ARGUMENT_TYPE.getErrorMessage(
                "Type '%s' not supported for image's data.", args.get(DATA_ARG_INDEX).getClass().getCanonicalName()));
            callback.call(context);
            return;
        }

        context.addFrame(newFrameFromJson(args));
        callbackContext.success();
    }

    @NonNull
    private Frame newFrameFromJson(@NonNull JSONArray args) throws JSONException {
        String data = args.getString(DATA_ARG_INDEX);
        FrameType imageType = FrameType.fromValue(((JSONObject) args.get(OPTIONS_ARG_INDEX)).getInt(TYPE_ARG_NAME));
        int[] imageData = imageType.getImageBytesFromBase64EncodedString(data);
        long timestamp = ((JSONObject) args.get(OPTIONS_ARG_INDEX)).getLong(TIMESTAMP_ARG_NAME);
        return new Frame(imageData, imageType, timestamp);
    }
}
