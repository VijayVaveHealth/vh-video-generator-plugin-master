package com.xmartlabs.cordova.frame2video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class FramesToVideoConverter {

    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int INFLAME_INTERVAL = 1;
    private static long CODEC_DEQUEUE_TIMEOUT_USEC = 5000;

    private final int height;
    private final int width;
    private final String outputPath;

    private MediaCodec mediaCodec;
    private int mediaCodecColorFormat;
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex;

    FramesToVideoConverter(String outputPath, int width, int height) {
        this.height = height;
        this.width = width;
        this.outputPath = outputPath;
    }

    void init() throws IOException {
        prepareEncoder();
    }

    void  addFrame(long presentationTimeMs, String frameData) throws IOException {
        byte[] fbytes = Base64.decode(frameData, Base64.DEFAULT);

        IntBuffer intBuf = ByteBuffer.wrap(fbytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        int[] colors = new int[intBuf.remaining()];
        intBuf.get(colors);
        byte[] yuvData = convertToYUV(width, height, colors);

        encodeFrame(yuvData, presentationTimeMs * 1_000);
    }

    void end() {
        release();
    }

    private void prepareEncoder() throws IOException {
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            throw new RuntimeException("Unable to find an appropriate codec for '" + MIME_TYPE + "' video type.");
        }

        mediaCodecColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
        mediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mediaCodecColorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, INFLAME_INTERVAL);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private void encodeFrame(byte[] frameData, long presentationTimeUs) {
        boolean processed = false;
        while (!processed) {
            int inputBufIndex = mediaCodec.dequeueInputBuffer(CODEC_DEQUEUE_TIMEOUT_USEC);
            if (inputBufIndex >= 0) {
                final ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufIndex);
                inputBuffer.clear();
                inputBuffer.put(frameData);
                mediaCodec.queueInputBuffer(inputBufIndex, 0, frameData.length, presentationTimeUs, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_DEQUEUE_TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.w("FramesToVideoConverter", "No output from encoder available yet.");
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                videoTrackIndex = mediaMuxer.addTrack(newFormat);
                mediaMuxer.start();
                Log.d("FramesToVideoConverter", "Started media muxer");
            } else if (encoderStatus < 0) {
                Log.w("FramesToVideoConverter", "Unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else if (bufferInfo.size != 0) {
                ByteBuffer encodedData = mediaCodec.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    Log.e("FramesToVideoConverter", "Null data got from encoder with status " + encoderStatus);
                } else {
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                    mediaCodec.releaseOutputBuffer(encoderStatus, false);
                    processed = true;
                }
            }
        }
        Log.d("FramesToVideoConverter", "Finished encoding frames.");
    }

    private void release() {
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        MediaCodecInfo[] codecInfos = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }

        throw new RuntimeException(
            String.format("Color format not found for codec '%s' and '%s' video type", codecInfo.getName(), mimeType));
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private byte[] convertToYUV(int inputWidth, int inputHeight, int[] argb) {
        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];

        if (isSemiPlanarYUV(mediaCodecColorFormat)) {
            encodeYUV420SemiPlanar(yuv, argb, inputWidth, inputHeight);
        } else {
            encodeYUV420Planar(yuv, argb, inputWidth, inputHeight);
        }

        return yuv;
    }

    private boolean isSemiPlanarYUV(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                return false;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                throw new RuntimeException("unknown format " + colorFormat);
        }
    }

    private void encodeYUV420SemiPlanar(byte[] yuv420sp, int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;

        int[] yuv = new int[3];
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                yuvComponentsFromArgbInteger(argb[index], yuv);
                int Y = yuv[0], U = yuv[1], V = yuv[2];

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));

                }

                index++;
            }
        }
    }

    private void encodeYUV420Planar(byte[] yuv420p, int[] argb, int width, int height) {
        int frameSize = width * height;
        int chromasize = frameSize / 4;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + chromasize;

        int index = 0;
        int[] yuv = new int[3];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                yuvComponentsFromArgbInteger(argb[index], yuv);
                int Y = yuv[0], U = yuv[1], V = yuv[2];

                yuv420p[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420p[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420p[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }

                index ++;
            }
        }
    }

    private void yuvComponentsFromArgbInteger(int argb, int[] yuv) {
        int R, G, B, Y, U, V;

        R = (argb & 0xff0000) >> 16;
        G = (argb & 0xff00) >> 8;
        B = (argb & 0xff);

        Y = ((66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
        U = (( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
        V = (( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

        yuv[0] = Y;
        yuv[1] = U;
        yuv[2] = V;
    }

    public String getOutputPath() {
        return outputPath;
    }
}
