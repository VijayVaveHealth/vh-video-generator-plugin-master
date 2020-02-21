import Foundation
import AVFoundation
import UIKit
import CoreMedia
import Photos


enum RecordingResult {

    case success(url: URL)
    case error
    case aborted

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

    @objc(start:)
    func start(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run() { [unowned self] in
         guard let options = command.argument(at: 0) as? [String: Int] else {
             let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: "There was an error with the parameters you passed. Please pass first the base64 encoded data" +
                                                                  "and then the type of the data (0: PNG, 1: byte array)")
                self.commandDelegate!.send(pluginResult,
                                           callbackId: command.callbackId)
                return
         }
            self.lock.lock()
            defer { self.lock.unlock() }
                do {
                    self.dataWidth = options["width"] ?? 256
                    self.dataHeight = options["height"] ?? 256
                    self.initialTime = options["timestamp"] ?? 0
                    self.tempFilePath = newFileUrl()
                    try self.setupProcessedVideoWriter(path: self.tempFilePath)
                    self.processedVideoWriter?.startWriting()
                    self.processedVideoWriter?.startSession(atSourceTime: kCMTimeZero)
                     let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                     self.commandDelegate!.send(pluginResult,
                                       callbackId: command.callbackId)
                } catch {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                                       messageAs: "There was an error with the parameters you passed. Please pass first the base64 encoded data" +
                                                                  "and then the type of the data (0: PNG, 1: byte array)")
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
                                                       messageAs: "There was an error with the parameters you passed. Please pass first the base64 encoded data" +
                                                                  "and then the type of the data (0: PNG, 1: byte array)")
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

                //TODO check color or non color by changing pixel format type
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
                                                       messageAs: "Frame wasnt saved")
                    }
                 self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
                }
            }
        }
    }

    // objc(saveFrame:)
    private func saveFrame(buffer: CVPixelBuffer, relativeTime interval: TimeInterval, completionHandler handler: @escaping (Bool) -> Void) {
          let time = floor(interval * 1000)
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
            PHPhotoLibrary.shared().performChanges({
             PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: (self?.tempFilePath!)!)
            }) { saved, error in
            debugPrint("Tried to camera roll")
                if saved {
                    debugPrint("Saved to camera roll")
                }
             }   
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK,
                                               messageAs: result)
            self?.commandDelegate!.send(pluginResult,
                    callbackId: command.callbackId)
        })
    }

    // objc(abortRecording:)
  private func abortRecording() {
        processedWriterInput?.markAsFinished()
        debugPrint("aborted recording")
        onFinishWritingVideo?(.aborted)
        cleanup()
    }

    // objc(cleanup:)
private func cleanup() {
        readerOutput = nil
        videoReader = nil
        processedReaderOutput = nil
        processedVideoReader = nil
        cleanUpWriter()
    }

    // objc(cleanUpWriter:)
   private func cleanUpWriter() {
        processedInputAdaptor = nil
        processedWriterInput = nil
        processedVideoWriter = nil
    }

    // objc(setupProcessedVideoWriter:)
    private func setupProcessedVideoWriter(path: URL) throws {
        processedVideoWriter = try AVAssetWriter(outputURL: path, fileType: AVFileType.mov)
        setVideoInputAdaptor()
        if processedVideoWriter!.canAdd(processedWriterInput!) {
            processedVideoWriter!.add(processedWriterInput!)
        } else {
            let error = DSError.addInputFailed as Error
            throw error
        }
    }

    // objc(setVideoInputAdaptor:)
    private func setVideoInputAdaptor() {
        let videoSettings = [AVVideoCodecKey: AVVideoCodecType.jpeg, AVVideoWidthKey: dataWidth, AVVideoHeightKey: dataHeight
                             ,AVVideoCompressionPropertiesKey: [AVVideoQualityKey: NSNumber(floatLiteral: 1.0),
                                                                AVVideoMaxKeyFrameIntervalKey: NSNumber(integerLiteral: 1)]
            ] as [String : Any]
        processedWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings)
        processedWriterInput?.expectsMediaDataInRealTime = true
        let bufferAttributes = [kCVPixelBufferPixelFormatTypeKey as String:
            NSNumber(value: Int32(Constants.pixelFormat))] as [String: Any]
        processedInputAdaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: processedWriterInput!,
                                                                     sourcePixelBufferAttributes: bufferAttributes)
    }

}
