
# FrameToVideoPlugin

FrameToVideoPlugin is a Cordova plugin to run create a video out of a set of frames. This repository includes the plugin in the `FrameToVideoPlugin` folder. It also includes an example app.

## Example app

To run the Example app you can follow these steps:

* Download the repository
* Open the terminal in the root folder and run `npm install`
* Run `ionic cordova prepare ios`
* Run `ionic cordova plugin add FrameToVideoPlugin`
* Run `ionic cordova plugin add cordova-plugin-add-swift-support --save`
* Open Xcode and set the correct Provisioning profile. (in my case I had just to select a development team). Possibly you want to change the bundle identifier as well.
* Run `ionic cordova build ios`
* Go to Xcode and run!

There might be a better way or some unnecessary steps but this works for me.

## Plugin

### Installation

Copy the plugin's files into a folder `FrameToVideoPlugin` inside your project and run:
`ionic cordova plugin add FrameToVideoPlugin`

### API

There are three functions to be called from a TypeScript file:

* cordova.plugins.FrameToVideoPlugin.start(options, success, error);
  - This function sets up the video writer and sets the width and height of the frames and initial time inside of options as.
  ```
  cordova.plugins.FrameToVideoPlugin.start({"timestamp": 1582054790799, "width": 256, "height":256})
  ```
  The parameters also include a `success` and an `error` callback.
    - `options`: dictionary containing other parameters. With `timestamp` as milliseconds, `height` and `width`.
    - `success`: callback which returns the output if the fram was able to be added in a dictionary.
    - `error`: error callback
    
* cordova.plugins.FrameToVideoPlugin.addFrame(image, options, success, error);
  - Adds a frame to the video. The parameters are:
    - `image`: Image passed as base64 String or ArrayBuffer (recommended).
    - `options`: dictionary containing other parameters. Must include `type` 0 if the image is a PNG/JPG or 1 if it is just binary RGB.
      . Also its `timestamp` is required to be passed in millisecond
    - `success`: callback which returns the output if the fram was able to be added in a dictionary.
    - `error`: error callback
    
* cordova.plugins.FrameToVideoPlugin.end(success, error);
  - Writes the video and finishes up the process. Also adds video to camera roll and gives back URL The parameters are:
    - `saveToRoll`: Boolean parameter to add to camera roll in case of True.
    - `success`: callback which returns the output if the video was added correctly.
    - `error`: error callback
    
    
    ```
     run(times) {
    var self = this;
    cordova.plugins.FrameToVideoPlugin.addFrame(this.imageData.data.buffer, {"type": 1, "timestamp": times},
      function(msg) {
        if (times >= SomeNumber) {
          cordova.plugins.FrameToVideoPlugin.end( true, 
            function (url) {
              console.log(url["fileURL"])
            }  
            );

        } else {
          self.run(times + 1000);
        }
      },
      function(err) {
        alert(err);
      }
    );
  }
    ```
  ### Error Codes
  * Error 1: There was an error with the parameters you passed when creating a dictionary out of the argument
  * Error 2: There was an error with one of the parameters
  * Error 3: There was an error with the parameters you passed. Please pass first the base64 encoded data, then then the type of the data (0: PNG, 1: byte array) and last the timestamp
  * Error 4: The added frame was out of order and could not be added
  * Error 5: Frame wasnt saved
  * Error 6: Error when creating writer
  
  ### Notes
  * Frames need to be sent in ascending order of timestamp
  * Over 100 frames supported per video
  * Need to remove data:image/png;base64, from a Base64 String in order to work
