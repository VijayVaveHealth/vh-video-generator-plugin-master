var exec = require('cordova/exec');

exports.start = function (options, success, error) {
    exec(success, error, 'FrameToVideoPlugin', 'start', [options]);
};

exports.addFrame = function (imageData, options, success, error) {
    exec(success, error, 'FrameToVideoPlugin', 'addFrame', [imageData, options]);
};

exports.end = function (success, error) {
    exec(success, error, 'FrameToVideoPlugin', 'end', []);
};
