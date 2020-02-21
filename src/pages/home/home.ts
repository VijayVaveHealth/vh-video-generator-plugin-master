import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';

declare var cordova;

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  public imageData: ImageData;
  public logg: string;
  

  constructor(public navCtrl: NavController) {
    var self = this;
    window.onload = function() {
      self.drawOnCanvas("testImage01");
    }
  }

  drawOnCanvas(imageId) {
    var c=<HTMLCanvasElement>document.getElementById("canvas");
    var ctx=c.getContext("2d");
    var img=<HTMLImageElement>document.getElementById(imageId);
    let size = 256;
    c.width = size; c.height = size;
    ctx.drawImage(img,0,0,size,size);
    this.imageData = ctx.getImageData(0,0,size,size);
  }

  classify(event){
    var emtId = event.target.id;
    this.drawOnCanvas(emtId);
    cordova.plugins.FrameToVideoPlugin.start({"timestamp": 0, "width": this.imageData.width, "height": this.imageData.height})
    this.run(1000);
  }

  run(times) {
    var self = this;
    console.log(this.imageData.data.buffer)
    cordova.plugins.FrameToVideoPlugin.addFrame(this.imageData.data.buffer, {"type": 1, "timestamp": times},
      function(msg) {
        if (times == 100000) {
          cordova.plugins.FrameToVideoPlugin.end( 
            function (url) {
              console.log(url["fileURL"])
              var path = url["fileURL"];
              alert("Video saved to camera roll")
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

}
