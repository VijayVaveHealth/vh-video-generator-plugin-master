package com.xmartlabs.cordova.frame2video;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class FileUtils {

    private FileUtils() {}

    static void copy(@NonNull File src, @NonNull File dst) throws IOException {
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
}
