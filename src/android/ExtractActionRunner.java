package com.xmartlabs.cordova.frame2video;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static android.media.MediaMetadataRetriever.OPTION_CLOSEST;
import static android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC;

public class ExtractActionRunner implements ActionRunner  {
  private static final int OPTIONS_ARG_INDEX = 0;

  private static final String VIDEO_FILE_NAME_ARG_NAME = "videoFileName";
  private static final String WIDTH_ARG_NAME = "width";
  private static final String HEIGHT_ARG_NAME = "height";

  @NonNull
  private final Action action;

  ExtractActionRunner(@NonNull Action action) {
    this.action = action;
  }

  @Nullable
  @Override
  public void run(@NonNull CordovaPlugin plugin, @NonNull Context appContext, @NonNull CallbackContext callbackContext, @Nullable ActionContext context, @NonNull JSONArray args, @NonNull ActionRunnerCallback callback) throws JSONException, IOException {

    JSONObject options = args.getJSONObject(OPTIONS_ARG_INDEX);
    String videoPath = options.getString(VIDEO_FILE_NAME_ARG_NAME);
    Integer height = options.getInt(WIDTH_ARG_NAME);
    Integer width = options.getInt(HEIGHT_ARG_NAME);

    ArrayList<String> frameList;
    ArrayList<Long> frameTimes;
    /* MediaMetadataRetriever class is used to retrieve meta data from methods. */
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      //path of the video of which you want frames
      retriever.setDataSource(videoPath);
    }catch (Exception e) {
      System.out.println("Exception= "+e);
    }
    // created an arraylist of bitmap that will store your frames
    frameList = new ArrayList<>();
    frameTimes = new ArrayList<>();
    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    String frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

    int duration_millisec = Integer.parseInt(duration); //duration in millisec
    int duration_second = duration_millisec / 1000;  //millisec to sec.
    int frames_per_second = 20;  //no. of frames want to retrieve per second
    int numeroFrameCaptured = frames_per_second * duration_second;
    int time_microseconds = duration_millisec * 1000;
    //int step = time_microseconds / numeroFrameCaptured;
//    let sampleCounts = 120
//    let totalTimeLength = Int(videoDuration.seconds * Double(videoDuration.timescale))
//    let step = totalTimeLength / sampleCounts
//
//    for i in 0 ..< sampleCounts {
//      let cmTime = CMTimeMake(value: Int64(i * step), timescale: Int32(videoDuration.timescale))
//      frameForTimes.append(NSValue(time: cmTime))
//    }
//    for(int i=0;i<numeroFrameCaptured;i+=1)
//    {
//      frameTimes.add((long) (step * i));
//    }
//    for (int i = 0; i < numeroFrameCaptured; i++)
//    {
//      //setting time position at which you want to retrieve frames
//      Bitmap bitmap= retriever.getFrameAtTime(frameTimes.get(i));
//      String img = encodeToBase64(bitmap);
//      frameList.add(img);
//    }
    int step = 1000000 / 20;
    for(int i=0;i<duration_millisec*1000;i+=step){
      Bitmap bitmap= retriever.getFrameAtTime(i,OPTION_CLOSEST);
      String img = encodeToBase64(bitmap);
      frameList.add(img);
    }


    JSONArray jsArray = new JSONArray(frameList);

    callbackContext.success(jsArray);
    callback.call(null);
  }

  public static String encodeToBase64(Bitmap image)
  {
    Bitmap immagex=image;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
    byte[] b = baos.toByteArray();
    String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

    Log.e("LOOK", imageEncoded);
    return imageEncoded;
  }
}
