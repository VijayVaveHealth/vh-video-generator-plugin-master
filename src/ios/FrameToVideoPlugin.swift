import Foundation
import AVFoundation
import UIKit
import CoreMedia
import Photos
import Foundation


enum RecordingResult {

    case success(url: URL)
    case error
    case aborted

}

struct Frame {

    let interval: TimeInterval
    let buffer: CVPixelBuffer

}

@objc(FrameToVideoPlugin) class FrameToVideoPlugin : CDVPlugin{

    static let instance = FrameToVideoPlugin()
    // MARK: - URLs
    var tempFilePath: URL!
    var processedVideoUrl: URL!


    // MARK: - Recording variables
    let lock = NSLock()
    var onFinishWritingVideo: ((RecordingResult) -> Void)?


    var processedVideoWriter: AVAssetWriter?
    var processedWriterInput: AVAssetWriterInput?
    var processedInputAdaptor: AVAssetWriterInputPixelBufferAdaptor?

    var videoReader: AVAssetReader?
    var readerOutput: AVAssetReaderOutput?

    var processedVideoReader: AVAssetReader?
    var processedReaderOutput: AVAssetReaderOutput?

    var dataWidth: Int = 256
    var dataHeight: Int = 256
    var initialTime: Int = 0

    var frames: [Frame] = []

    var videoUrl:URL! // use your own url
    var frameRate:Int = 20
    var framesExtract:[String] = []
    private var generator:AVAssetImageGenerator!
    var frameStartIndex: Int = 0

    @objc(start:)
    func start(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run() { [unowned self] in
         guard let options = command.argument(at: 0) as? [String: Any] else {
             let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.initParametersFormatFailed)
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                return
         }
            self.lock.lock()
            defer { self.lock.unlock() }
                do {
                    self.dataWidth = (options["width"] as? Int) ?? 256
                    self.dataHeight = (options["height"] as? Int) ?? 256
                    self.initialTime = (options["timestamp"] as? Int) ?? 0
                    let quality = (options["quality"] as? NSNumber) ?? 0.8
                    let external = options["external"] as? Bool ?? false
                    if (external) {
                        self.tempFilePath = newFileUrlExternal(fileName: options["videoFileName"] as? String)
                    } else {
                        self.tempFilePath = newFileUrl(fileName: options["videoFileName"] as? String)
                    }

                    self.frames = []
                    try self.setupProcessedVideoWriter(path: self.tempFilePath, quality: quality)
                    self.processedVideoWriter?.startWriting()
                    self.processedVideoWriter?.startSession(atSourceTime: CMTime.zero)
                     let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                     self.commandDelegate!.send(pluginResult,
                                       callbackId: command.callbackId)
                } catch {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.initParametersFailed)
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                }
        }
    }

    @objc(addFrame:)
    func addFrame(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run() { [unowned self] in
            var stringData: Data?
            if let dataStr = command.argument(at: 0) as? String {
                stringData = Data(base64Encoded: dataStr)
            }
            guard var data = stringData ?? command.argument(at: 0) as? Data, let options = command.argument(at: 1) as? [String: Int],
                let type = options["type"], let frameTime = options["timestamp"]  else {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.initParametersAddFrameFailed)
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                return
            }

            var pixelBuffer: CVPixelBuffer?
            if type == 0 {
                // Data is coming as PNG
                let image = UIImage(data: data)
                pixelBuffer = image?.pixelBuffer(width: self.dataWidth, height: self.dataHeight)
            } else {
                // Data is coming as array buffer
                let _ = data.withUnsafeMutableBytes { bytes in
                    CVPixelBufferCreateWithBytes(kCFAllocatorDefault, self.dataWidth, self.dataHeight, kCVPixelFormatType_32BGRA, bytes, self.dataWidth * 4, nil, nil, nil, &pixelBuffer)
                }
            }
            if let pixelBuffer = pixelBuffer {
                let result = ["addFrame": "frameAdded"] as [String : Any]
                self.saveFrame(buffer: pixelBuffer, relativeTime: TimeInterval(frameTime - self.initialTime)) { saved in
                    var pluginResult: CDVPluginResult
                    if saved {
                        pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)

                    } else {
                        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.frameWasntSavedError)
                    }
                 self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
                }
            }
        }
    }

    private func saveFrame(buffer: CVPixelBuffer, relativeTime interval: TimeInterval, completionHandler handler: @escaping (Bool) -> Void) {
          let time = floor(interval)
          debugPrint("Trying to save frame at time: \(time)")
          while processedInputAdaptor?.assetWriterInput.isReadyForMoreMediaData == false {
              //TODO: log message
              debugPrint("inputAdaptor was not ready. Sleeping 5ms")
              usleep(5000)
          }
          if processedInputAdaptor?.append(buffer, withPresentationTime: CMTime(value: CMTimeValue(interval), timescale: 1000)) == false {
            debugPrint(processedVideoWriter!.status.rawValue)
            debugPrint(processedVideoWriter?.error as Any)
            abortRecording()
            handler(false)
          } else {
            debugPrint("Frame saved at:  \(time)")
            handler(true)
          }
      }

    @objc(end:)
    func end(_ command: CDVInvokedUrlCommand) {
        lock.lock()
        defer { lock.unlock() }
        processedWriterInput?.markAsFinished()
        self.processedVideoWriter?.finishWriting(completionHandler: { [weak self] in
            debugPrint(self?.processedVideoWriter!.status.rawValue)
            let result = ["fileURL": self?.tempFilePath.absoluteString] as [String : Any]
            if let saveToRoll = command.argument(at: 0) as? Bool, saveToRoll {
                PHPhotoLibrary.shared().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: (self?.tempFilePath!)!)
                }) { saved, error in
                    debugPrint("Tried to camera roll")
                    if saved {
                        debugPrint("Saved to camera roll")
                    }
                }
            }
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK,
                                               messageAs: result)
            self?.commandDelegate!.send(pluginResult,
                                        callbackId: command.callbackId)
        })
    }

    @objc(extract:)
    func extract(_ command: CDVInvokedUrlCommand) {
           self.commandDelegate.run() { [unowned self] in
         guard let options = command.argument(at: 0) as? [String: Any] else {
             let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.initParametersFormatFailed)
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                return
         }
            self.lock.lock()
            defer { self.lock.unlock() }
                do {
                    let dataWidth = (options["width"] as? Int) ?? 600
                    let dataHeight = (options["height"] as? Int) ?? 800
                    let videoFileName = (options["videoFileName"] as? String) ?? ""
                    self.videoUrl = URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent("Library/NoCloud").appendingPathComponent(videoFileName);
                    self.frameRate = (options["frameRate"] as? Int) ?? 20

                    debugPrint("Filename is : " )
                    try getAllFrames()
                    let pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_OK,
                            messageAs: self.framesExtract
                    )
                     self.commandDelegate!.send(pluginResult,
                                       callbackId: command.callbackId)
                } catch {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: DSError.initParametersFailed)
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                }
        }

    }

  private func abortRecording() {
        processedWriterInput?.markAsFinished()
        debugPrint("aborted recording")
        onFinishWritingVideo?(.aborted)
        cleanup()
    }

private func cleanup() {
        readerOutput = nil
        videoReader = nil
        processedReaderOutput = nil
        processedVideoReader = nil
        cleanUpWriter()
    }

   private func cleanUpWriter() {
        processedInputAdaptor = nil
        processedWriterInput = nil
        processedVideoWriter = nil
    }

    private func setupProcessedVideoWriter(path: URL, quality: NSNumber) throws {
        processedVideoWriter = try AVAssetWriter(outputURL: path, fileType: AVFileType.mov)
        setVideoInputAdaptor(quality: quality)
        if processedVideoWriter!.canAdd(processedWriterInput!) {
            processedVideoWriter!.add(processedWriterInput!)
        } else {
            let error = DSError.writerWasntAbleToSetupError as Error
            throw error
        }
    }

    private func setVideoInputAdaptor(quality: NSNumber) {
        let videoSettings = [AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: dataWidth, AVVideoHeightKey: dataHeight
                             ,AVVideoCompressionPropertiesKey: [AVVideoQualityKey: quality, AVVideoMaxKeyFrameIntervalKey: NSNumber(integerLiteral: 1)]
            ] as [String : Any]
        processedWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings)
        processedWriterInput?.expectsMediaDataInRealTime = true
        let bufferAttributes = [kCVPixelBufferPixelFormatTypeKey as String:
            NSNumber(value: Int32(Constants.pixelFormat))] as [String: Any]
        processedInputAdaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: processedWriterInput!,
                                                                     sourcePixelBufferAttributes: bufferAttributes)
    }

    private func getAllFrames() throws {
       let asset:AVAsset = AVAsset(url:self.videoUrl)
       let videoDuration = asset.duration
       self.generator = AVAssetImageGenerator(asset:asset)
       self.generator.appliesPreferredTrackTransform = true
       self.framesExtract = []
       self.frameStartIndex = 0
       var frameForTimes = [NSValue]()
       let totalTimeLength = Int(videoDuration.value);
       let timeInSeconds = CMTimeGetSeconds(videoDuration)
       let sampleCounts =  Int(timeInSeconds * Double(self.frameRate))
       if (sampleCounts == 0) {
        throw "Wrong cine duration"
       }
       let step = totalTimeLength / sampleCounts

       for i in 0 ..< sampleCounts {
            let cmTime = CMTimeMake(value: Int64(i * step), timescale: Int32(videoDuration.timescale))
            frameForTimes.append(NSValue(time: cmTime))
       }
        self.generator.requestedTimeToleranceAfter = .zero
        self.generator.requestedTimeToleranceBefore = .zero
        // Make `generateCGImagesAsynchronously` synchronous within the current operation.
        let block = DispatchGroup()
        block.enter()
        // Can be safely modified from the generator's callbacks' threads as they are
        // strictly sequential.
        var countProcessed = 0

       self.generator.generateCGImagesAsynchronously(forTimes: frameForTimes, completionHandler: {requestedTime, image, actualTime, result, error in
               DispatchQueue.main.async {
                   if let image = image {
                       print(requestedTime.value, requestedTime.seconds, actualTime.value)
                    self.framesExtract.append(self.convertImageToBase64String(img: UIImage(cgImage: image)))
                   }

                countProcessed += 1

                if countProcessed == frameForTimes.count {
                    block.leave()
                }
               }
           })
        block.wait()

       self.generator = nil
    }

    func convertImageToBase64String (img: UIImage) -> String {
        return img.pngData()?.base64EncodedString() ?? ""
    }

}
