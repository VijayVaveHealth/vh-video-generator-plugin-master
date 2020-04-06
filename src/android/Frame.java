package com.xmartlabs.cordova.frame2video;

import android.support.annotation.NonNull;

class Frame {
    @NonNull
    private final int[] data;
    @NonNull
    private final FrameType type;
    private final long timestamp;

    Frame(@NonNull int[] data, @NonNull FrameType type, long timestamp) {
        this.data = data;
        this.type = type;
        this.timestamp = timestamp;
    }

    @NonNull
    int[] getData() {
        return data;
    }

    @NonNull
    FrameType getType() {
        return type;
    }

    long getTimestamp() {
        return timestamp;
    }
}