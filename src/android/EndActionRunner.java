package com.xmartlabs.cordova.frame2video;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

class EndActionRunner implements ActionRunner {

    private static final int EXPORT_TO_CAMERA_ROLL_ARG_INDEX = 0;

    @NonNull
    private final Action action;

    EndActionRunner(@NonNull Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a MP4 video file from the frames previously passed in, and it's stored in the app's files folder. The
     * video's file path is returned in the {@code callbackContext} execution context.
     * </p>
     * <p>If {@code true} is received as argument, then the video is exported to the camera roll. To achieve this,
     * the video file is copied to the device's external storage and then included in the media library. Notice that
     * to being able to write the video file to the device's external storage, the permission {@link
     * Manifest.permission#WRITE_EXTERNAL_STORAGE} is needed, in case it has not yet been granted,
     * it is requested as part of this command's execution.</p>
     */
    @Nullable
    @Override
    public void run(@NonNull CordovaPlugin plugin, @NonNull Context appContext,
                    @NonNull CallbackContext callbackContext, @Nullable ActionContext context,
                    @NonNull JSONArray args, @NonNull ActionRunnerCallback callback) throws JSONException, IOException {
        if (context == null) {
            callbackContext.error(Error.CONTEXT_NOT_INITIALIZED
                .getErrorMessage("Context must be created before calling '%s' action", action));
            callback.call(null);
            return;
        }

        String videoFilePath = context.finishEncodingVideo();
        boolean saveToCameraRoll = args.length() > 0 && args.getBoolean(EXPORT_TO_CAMERA_ROLL_ARG_INDEX);
        if (saveToCameraRoll) {
            saveVideoToCameraFolder(plugin, context, appContext, callbackContext, videoFilePath, callback);
        } else {
            callbackContext.success(new JSONObject(Collections.singletonMap("fileURL", videoFilePath)));
            callback.call(null);
        }
    }

    private void saveVideoToCameraFolder(@NonNull CordovaPlugin plugin, @NonNull ActionContext context,
                                         @NonNull Context appContext, @NonNull CallbackContext callbackContext,
                                         @NonNull String videoFilePath, @NonNull ActionRunnerCallback callback) throws IOException {
        String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        context.requestPermissionIfRequired(plugin, appContext, writePermission, granted -> {
            if (granted) {
                String appName = appContext.getPackageManager().getApplicationLabel(appContext.getApplicationInfo()).toString();
                File cameraFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), appName);
                cameraFolder.mkdirs();
                File src = new File(videoFilePath);
                File dst = new File(cameraFolder, src.getName());
                FileUtils.copy(src, dst);
                appContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dst)));
                Log.i(FrameToVideoPlugin.LOG_TAG, String.format("Video added to camera roll with path '%s'.", dst.getAbsolutePath()));
            } else {
                Log.w(FrameToVideoPlugin.LOG_TAG, "Asked to save video to camera roll but '" +
                        writePermission + "' permission is no granted.");
            }

            callbackContext.success(new JSONObject(Collections.singletonMap("fileURL", videoFilePath)));
            callback.call(null);
        });
    }
}
