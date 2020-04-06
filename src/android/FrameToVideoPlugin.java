package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;

public class FrameToVideoPlugin extends CordovaPlugin {

    static final String LOG_TAG = FrameToVideoPlugin.class.getCanonicalName();

    @Nullable
    private static ActionContext context = null;

    private static void setContext(ActionContext newContext) {
        context = newContext;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Context applicationContext = cordova.getActivity().getApplicationContext();

        cordova.getThreadPool().execute(() -> {
            try {
                ActionRunner actionRunner = ActionRunnerFactory.newActionRunner(action);
                actionRunner.run(this, applicationContext, callbackContext, context, args,
                        FrameToVideoPlugin::setContext);
            } catch (IllegalStateException e) {
                callbackContext.error(Error.ILLEGAL_STATE.getErrorMessage(
                    "Error got when running '%s' action: %s.", action, e.getMessage()));
            } catch (JSONException e) {
                callbackContext.error(Error.INVALID_JSON_ARGUMENT.getErrorMessage(
                    "Error got when running '%s' action: %s.", action, e.getMessage()));
            } catch (IOException e) {
                callbackContext.error(Error.GENERIC_IO_ERROR.getErrorMessage(
                    "Error got when running '%s' action: %s.", action, e.getMessage()));
            } catch (Exception e) {
                Log.w(LOG_TAG, String.format("Action '%s' thrown an unexpected error", action), e);
                callbackContext.error(Error.UNKNOWN_ERROR.getErrorMessage(
                    "Error got when running '%s' action: %s", action, e.getMessage()));
            }
        });
        return true;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);

        if (context == null) {
            Log.w(LOG_TAG, String.format(
                    "Got request permission result for request %d and permissions '%s' with results '%s' but no action context is available",
                    requestCode, Arrays.toString(permissions), Arrays.toString(grantResults)));
            return;
        }

        try {
            context.onRequestPermissionResult(requestCode, permissions, grantResults);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process request permission result", e);
        }
    }
}
