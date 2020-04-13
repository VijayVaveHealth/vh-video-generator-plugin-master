package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.xmartlabs.cordova.frame2video.FrameToVideoPlugin.LOG_TAG;

class ActionContext {

    private static final int DEFAULT_BIT_RATE = 2_000_000; // (2 Mbs)
    private static final int DEFAULT_FRAME_RATE = 30; // 30 FPS

    interface GrantPermissionRequestCallback {
        void call(boolean granted) throws IOException;
    }

    private int bitRate = DEFAULT_BIT_RATE;
    private int frameRate = DEFAULT_FRAME_RATE;
    @Nullable
    private FramesToVideoConverter videoConverter;

    private final int height;
    private final long initialTimestamp;
    @NonNull
    private final Object lock = new Object();
    private final int width;
    @NonNull
    private final UUID uuid = UUID.randomUUID();
    @NonNull
    private final String videoFilePath;

    private Map<Integer, GrantPermissionRequestCallback> grantRequests = new HashMap<>();

    ActionContext(@NonNull String videoFilePath, int width, int height, long initialTimestamp) {
        this.height = height;
        this.initialTimestamp = initialTimestamp;
        this.width = width;
        this.videoFilePath = videoFilePath;
    }

    void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    void start() throws IOException {
        synchronized (lock) {
            getVideoConverter().init();
        }
    }

    void addFrame(@NonNull Frame frame) {
        long presentationTimeMillis = frame.getTimestamp() - initialTimestamp;
        if (presentationTimeMillis < 0) {
            throw new IllegalStateException("New frame's timestamp can't be before video's start time.");
        }

        FramesToVideoConverter videoConverter = getVideoConverter();
        synchronized (lock) {
            videoConverter.addFrame(presentationTimeMillis, frame.getData());
        }
    }

    @NonNull
    String finishEncodingVideo() {
        FramesToVideoConverter videoConverter = getVideoConverter();
        synchronized (lock) {
            videoConverter.finish();
        }
        return getVideoFilePath();
    }

    void cancel() {
        File file = new File(finishEncodingVideo());
        if (file.exists()) {
            file.delete();
        }
    }

    void requestPermissionIfRequired(CordovaPlugin plugin, Context context, String permission,
                                     GrantPermissionRequestCallback callback) throws IOException {
         int status = ActivityCompat.checkSelfPermission(context, permission);
         if (status != PackageManager.PERMISSION_GRANTED) {
             int requestCode = new Random().nextInt();
             grantRequests.put(requestCode, callback);
             plugin.cordova.requestPermission(plugin, requestCode, permission);
             return;
         }

         callback.call(true);
    }

    void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws IOException {
        GrantPermissionRequestCallback callback = grantRequests.get(requestCode);
        if (callback == null) {
            // Permission not requested by this plugin
            Log.d(LOG_TAG, String.format(
                    "Request %d and permissions %s not started by FrameToVideoPlugin",
                    requestCode, Arrays.toString(permissions)));
            return;
        }

        grantRequests.remove(requestCode);
        boolean granted = grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED;
        callback.call(granted);
    }

    @NonNull
    private String getVideoFilePath() {
        return videoFilePath;
    }

    @NonNull
    private FramesToVideoConverter getVideoConverter() {
        synchronized (lock) {
            if (videoConverter == null) {
                videoConverter = new FramesToVideoConverter(getVideoFilePath(), width, height)
                    .withBitRate(bitRate)
                    .withFrameRate(frameRate);
            }
        }
        return videoConverter;
    }
}
