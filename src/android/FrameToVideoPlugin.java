package com.xmartlabs.cordova.frame2video;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.UUID;

public class FrameToVideoPlugin extends CordovaPlugin {

    static final String LOG_TAG = FrameToVideoPlugin.class.getCanonicalName();

    private static final int F2V_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    private long initTimestamp;
    private FramesToVideoConverter framesToVideoConverter;
    private CallbackContext endCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                switch (action) {
                    case "start":
                        start(args, callbackContext);
                        break;
                    case "addFrame":
                        addFrame(args, callbackContext);
                        break;
                    case "end":
                        end(args, callbackContext);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error(e.getMessage());
            }
        });
        return true;
    }

    private void start(JSONArray args, CallbackContext callbackContext) throws JSONException, IOException {
        JSONObject options = args.getJSONObject(0);
//        int height = 800;
//        int width = 600;
        int height = options.getInt("height");
        int width = options.getInt("width");
        initTimestamp = options.getLong("timestamp");

        String outputPath = cordova.getActivity().getFilesDir().getAbsolutePath() + "/" + UUID.randomUUID().toString() + ".mp4";
        framesToVideoConverter = new FramesToVideoConverter(outputPath, width, height);
        framesToVideoConverter.init();
        callbackContext.success();
    }

    private void addFrame(JSONArray args, CallbackContext callbackContext) throws JSONException, IOException {
        long timestamp = ((JSONObject) args.get(1)).getLong("timestamp");
        int type = ((JSONObject) args.get(1)).getInt("type");
        String data = args.getString(0);
        byte[] bgra = getImageBytesFromBase64EncodedString(data, type);
        framesToVideoConverter.addFrame(timestamp - initTimestamp, bgra);
        callbackContext.success();
    }

    private void end(JSONArray args, CallbackContext callbackContext) throws IOException {
        framesToVideoConverter.end();

        Activity context = cordova.getActivity();
        int status = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (status != PackageManager.PERMISSION_GRANTED) {
            cordova.requestPermission(this, F2V_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            endCallbackContext = callbackContext;
            return;
        }

        // App has access to write storage so we can save the video to camera roll
        saveToCameraRoll(callbackContext);
    }

    private void copy(File src, File dst) throws IOException {
        byte[] buffer = new byte[1024];
        try (InputStream is = new FileInputStream(src)) {
            try (OutputStream os = new FileOutputStream(dst)) {
                int len;
                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
            }
        }
    }

    private void saveToCameraRoll(CallbackContext callbackContext) throws IOException {
        Activity context = cordova.getActivity();
        String appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
        File cameraFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), appName);
        cameraFolder.mkdirs();
        File src = new File(framesToVideoConverter.getOutputPath());
        File dst = new File(cameraFolder, src.getName());
        copy(src, dst);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dst)));
        Log.i(FrameToVideoPlugin.LOG_TAG, String.format("Video added to camera roll with path '%s'.", dst.getAbsolutePath()));

        callbackContext.success(new JSONObject(
                Collections.singletonMap("fileURL", framesToVideoConverter.getOutputPath())));
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);

        if (requestCode != F2V_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Permission not requested by this plugin
            return;
        }

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Log.w(FrameToVideoPlugin.LOG_TAG, "Permission to write external storage was not granted");
            return;
        }

        try {
            saveToCameraRoll(endCallbackContext);
        } catch (IOException e) {
            Log.e(FrameToVideoPlugin.LOG_TAG, "Failed to write video to camera roll.", e);
            endCallbackContext.error(e.getMessage());
        }
    }

    private byte[] getImageBytesFromBase64EncodedString(String base64, int type) {
        if (type == 0) {
            byte[] decodedByte = Base64.decode(base64, 0);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

            int bytes = bitmap.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            bitmap.copyPixelsToBuffer(buffer);

            return buffer.array();
        } else {
            return Base64.decode(base64, Base64.DEFAULT);
        }
    }
}
