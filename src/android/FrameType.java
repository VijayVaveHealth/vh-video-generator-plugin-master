package com.xmartlabs.cordova.frame2video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

enum FrameType {
    PNG_JPEG(0) {
        @Override
        int[] getImageBytesFromBase64EncodedString(String base64) {
            byte[] decodedByte = Base64.decode(base64, 0);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

            // 1 byte for each color channel (r, g, b, a)
            int[] bytes = new int[bitmap.getWidth() * bitmap.getHeight()];
            for (int j = 0; j < bitmap.getHeight(); j++) {
                for (int i = 0; i < bitmap.getWidth(); i++) {
                    int argb = bitmap.getPixel(i, j);
                    bytes[j * bitmap.getWidth() + i] = argb;
                }
            }
            return bytes;
        }
    },
    RGB(1) {
        @Override
        int[] getImageBytesFromBase64EncodedString(String base64) {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            IntBuffer intBuf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            int[] colors = new int[intBuf.remaining()];
            intBuf.get(colors);

            for (int i = 0; i < colors.length; i++) {
                int abgr = colors[i];
                // Source is in format: 0xAABBGGRR
                int argb =
                        ((abgr & 0xFF000000) <<  0) | // AA______
                        ((abgr & 0x000000FF) << 16) | // __RR____
                        ((abgr & 0x0000FF00) <<  0) | // ____GG__
                        ((abgr & 0x00FF0000) >> 16);  // ______BB
                colors[i] = argb;
            }

            return colors;
        }
    },
    ;

    private final int value;

    FrameType(int value) {
        this.value = value;
    }

    @NonNull
    static FrameType fromValue(int value) {
        for (FrameType type : values()) {
            if (type.value == value) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Image type '%d' does not exist", value));
    }

    abstract int[] getImageBytesFromBase64EncodedString(String base64);
}