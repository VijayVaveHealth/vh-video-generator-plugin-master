//
//  Created by Rodrigo Arsuaga on 2/11/20.
//  Copyright © 2020 Rodrigo Arsuaga. All rights reserved.
//

import Foundation
import UIKit
import Accelerate
import Photos

// Helper functions from https://github.com/hollance/CoreMLHelpers/blob/master/CoreMLHelpers/UIImage%2BCVPixelBuffer.swift
extension UIImage {
    /**
     Resizes the image to width x height and converts it to an RGB CVPixelBuffer.
     */
    func pixelBuffer(width: Int, height: Int) -> CVPixelBuffer? {
        return pixelBuffer(width: width, height: height,
                           pixelFormatType: Constants.pixelFormat,
                           colorSpace: CGColorSpaceCreateDeviceRGB(),
                           alphaInfo: .noneSkipFirst)
    }

    func pixelBuffer(width: Int, height: Int, pixelFormatType: OSType,
                     colorSpace: CGColorSpace, alphaInfo: CGImageAlphaInfo) -> CVPixelBuffer? {
        var maybePixelBuffer: CVPixelBuffer?
        let attrs = [kCVPixelBufferCGImageCompatibilityKey: kCFBooleanTrue,
                     kCVPixelBufferCGBitmapContextCompatibilityKey: kCFBooleanTrue]
        let status = CVPixelBufferCreate(kCFAllocatorDefault,
                                         width,
                                         height,
                                         pixelFormatType,
                                         attrs as CFDictionary,
                                         &maybePixelBuffer)

        guard status == kCVReturnSuccess, let pixelBuffer = maybePixelBuffer else {
            return nil
        }

        CVPixelBufferLockBaseAddress(pixelBuffer, CVPixelBufferLockFlags(rawValue: 0))
        let pixelData = CVPixelBufferGetBaseAddress(pixelBuffer)

        guard let context = CGContext(data: pixelData,
                                      width: width,
                                      height: height,
                                      bitsPerComponent: 8,
                                      bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
                                      space: colorSpace,
                                      bitmapInfo: alphaInfo.rawValue)
            else {
                return nil
        }

        UIGraphicsPushContext(context)
        context.translateBy(x: 0, y: CGFloat(height))
        context.scaleBy(x: 1, y: -1)
        self.draw(in: CGRect(x: 0, y: 0, width: width, height: height))
        UIGraphicsPopContext()

        CVPixelBufferUnlockBaseAddress(pixelBuffer, CVPixelBufferLockFlags(rawValue: 0))
        return pixelBuffer
    }
}


func resizePixelBuffer(_ srcPixelBuffer: CVPixelBuffer,
                              cropX: Int,
                              cropY: Int,
                              cropWidth: Int,
                              cropHeight: Int,
                              scaleWidth: Int,
                              scaleHeight: Int) -> CVPixelBuffer? {

    CVPixelBufferLockBaseAddress(srcPixelBuffer, CVPixelBufferLockFlags(rawValue: 0))
    guard let srcData = CVPixelBufferGetBaseAddress(srcPixelBuffer) else {
        print("Error: could not get pixel buffer base address")
        return nil
    }
    let srcBytesPerRow = CVPixelBufferGetBytesPerRow(srcPixelBuffer)
    let offset = cropY*srcBytesPerRow + cropX*4
    var srcBuffer = vImage_Buffer(data: srcData.advanced(by: offset),
                                  height: vImagePixelCount(cropHeight),
                                  width: vImagePixelCount(cropWidth),
                                  rowBytes: srcBytesPerRow)

    let destBytesPerRow = scaleWidth*4
    guard let destData = malloc(scaleHeight*destBytesPerRow) else {
        print("Error: out of memory")
        return nil
    }
    var destBuffer = vImage_Buffer(data: destData,
                                   height: vImagePixelCount(scaleHeight),
                                   width: vImagePixelCount(scaleWidth),
                                   rowBytes: destBytesPerRow)

    let error = vImageScale_ARGB8888(&srcBuffer, &destBuffer, nil, vImage_Flags(0))
    CVPixelBufferUnlockBaseAddress(srcPixelBuffer, CVPixelBufferLockFlags(rawValue: 0))
    if error != kvImageNoError {
        print("Error:", error)
        free(destData)
        return nil
    }

    let releaseCallback: CVPixelBufferReleaseBytesCallback = { _, ptr in
        if let ptr = ptr {
            free(UnsafeMutableRawPointer(mutating: ptr))
        }
    }

    let pixelFormat = CVPixelBufferGetPixelFormatType(srcPixelBuffer)
    var dstPixelBuffer: CVPixelBuffer?
    let status = CVPixelBufferCreateWithBytes(nil, scaleWidth, scaleHeight,
                                              pixelFormat, destData,
                                              destBytesPerRow, releaseCallback,
                                              nil, nil, &dstPixelBuffer)
    if status != kCVReturnSuccess {
        print("Error: could not create new pixel buffer")
        free(destData)
        return nil
    }
    return dstPixelBuffer
}

func resizePixelBuffer(_ pixelBuffer: CVPixelBuffer,
                              width: Int, height: Int) -> CVPixelBuffer? {
    return resizePixelBuffer(pixelBuffer, cropX: 0, cropY: 0,
                             cropWidth: CVPixelBufferGetWidth(pixelBuffer),
                             cropHeight: CVPixelBufferGetHeight(pixelBuffer),
                             scaleWidth: width, scaleHeight: height)
}



func newFileUrl() -> URL {
    let tempPath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(String.randomString(length: 6) + ".mp4").absoluteString
    if FileManager.default.fileExists(atPath: tempPath) {
        do {
            try FileManager.default.removeItem(atPath: tempPath)
        } catch {
            print("Error deleting the existing temporary file")
        }
    }
    return URL(string: tempPath)!
}

func newAudioFileUrl() -> URL {
    let tempPath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(String.randomString(length: 6) + ".m4a").absoluteString
    if FileManager.default.fileExists(atPath: tempPath) {
        do {
            try FileManager.default.removeItem(atPath: tempPath)
        } catch {
            print("Error deleting the existing temporary file")
        }
    }
    return URL(string: tempPath)!
}

func deleteTemporaryFiles() {
    let tmpDirectory = try? FileManager.default.contentsOfDirectory(atPath: NSTemporaryDirectory())
    tmpDirectory?.forEach { file in
        let path = String.init(format: "%@%@", NSTemporaryDirectory(), file)
        let pathExtension = (path as NSString).pathExtension
        if pathExtension == "mp4" {
            try? FileManager.default.removeItem(atPath: path)
        }
    }
}

func imageFromAsset(asset: PHAsset, size: CGSize? = nil, completion: @escaping (UIImage?) -> Void) {
    let options = PHImageRequestOptions()
    options.isSynchronous = false
    options.isNetworkAccessAllowed = true

    let manager = PHImageManager.default()
    DispatchQueue.global(qos: .background).async {
        manager.requestImage(for: asset, targetSize: size ?? PHImageManagerMaximumSize, contentMode: .aspectFit, options: options) { image, info in
            DispatchQueue.main.async {
                completion(image)
            }
        }
    }
}

extension String: Error {

    static func randomString(length: Int) -> String {

        let letters : NSString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let len = UInt32(letters.length)

        var randomString = ""

        for _ in 0 ..< length {
            let rand = arc4random_uniform(len)
            var nextChar = letters.character(at: Int(rand))
            randomString += NSString(characters: &nextChar, length: 1) as String
        }

        return randomString
    }

}


struct Constants {

    static let FloatSize = MemoryLayout<Float>.size
    static let HalfSize = MemoryLayout<Float>.size / 2
    static let outputImageWidth = 256
    static let outputImageHeight = outputImageWidth
    static let pixelFormat = kCVPixelFormatType_32ARGB
    static let baseKernelCount = 16
}

struct DSError {
    static let addOutputFailed = "Add output failed"
    static let addInputFailed = "Add input failed"
    static let noTrackInVideo = "no track in video"
    static let couldNotCreateTexture = "Could not create texture from pixel buffer"
}
