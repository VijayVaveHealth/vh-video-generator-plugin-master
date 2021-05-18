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


func newFileUrlExternal(fileName: String?) -> URL {
    let videoFileName = fileName ?? (String.randomString(length: 6) + ".mp4")
    let path = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(videoFileName)
    do {
        try FileManager.default.removeItem(at: path)
    } catch {
        print("Error deleting the existing temporary file")
    }
    return path
}

func newFileUrl(fileName: String?) -> URL {
    let videoFileName = fileName ?? (String.randomString(length: 6) + ".mp4")
    let path = URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent("Library/NoCloud").appendingPathComponent(videoFileName);
    do {
        try FileManager.default.removeItem(at: path)
    } catch {
        print("Error deleting the existing temporary file")
    }
    return path
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
    static let initParametersFormatFailed = "Error 1: There was an error with the parameters you passed when creating a dictionary out of the argument"
    static let initParametersFailed = "Error 2: There was an error with one of the parameters"
    static let initParametersAddFrameFailed = "Error 3: There was an error with the parameters you passed. Please pass first the base64 encoded data, then then the type of the data (0: PNG, 1: byte array) and last the timestamp"
    static let frameOutOfOrderError = "Error 4: The added frame was out of order and could not be added"
    static let frameWasntSavedError = "Error 5: Frame wasnt saved"
    static let writerWasntAbleToSetupError = "Error 6: Error when creating writer"
}
