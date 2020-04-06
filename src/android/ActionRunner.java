package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

interface ActionRunner {

    interface ActionRunnerCallback {
        void call(@Nullable ActionContext resultContext);
    }

    /**
     * <p>Execute the intended action using the given {@code context}. It may be updated in case of a change in the
     * state is required for future calls.</p>
     *
     * @param plugin an instance of the plugin invoking this method.
     * @param callbackContext the javascript callback, {@link CallbackContext#success} or
     *      {@link CallbackContext#error} will be properly called when the action finishes.
     * @param context current execution context, it will be edited if needed to keep state between action calls.
     *
     * @param args arguments passed in from Cordova call.
     * @param callback a completion callback, it will be invoked when the command ends with a new
     *                 {@link ActionContext} instance to replace current one.
     * @throws IllegalStateException if provided {@code context} argument is in an invalid state.
     */
    @Nullable
    void run(@NonNull CordovaPlugin plugin, @NonNull Context appContext,
                      @NonNull CallbackContext callbackContext, @Nullable ActionContext context,
                      @NonNull JSONArray args, @NonNull ActionRunnerCallback callback) throws JSONException, IOException;
}
