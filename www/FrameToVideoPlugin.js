const exec = require('cordova/exec');
const PLUGIN_NAME = 'FrameToVideoPlugin';
const COMMANDS = {
  START: 'start',
  ADD_FRAME: 'addFrame',
  END: 'end'
};

exports.start = function (options) {
  return new Promise((resolve, reject) => {
    const timeoutFn = window.setTimeout(() => {
      reject('Timeout');
    }, options.timeout || 1000 * 20);

    exec(
      res => {
        resolve(res);
        window.clearTimeout(timeoutFn);
      },
      err => {
        reject(err);
        window.clearTimeout(timeoutFn);
      },
      PLUGIN_NAME,
      COMMANDS.START,
      [options]
      );
  });
};

exports.addFrame = function (imageData, options) {
  return new Promise((resolve, reject) => {
    const timeoutFn = window.setTimeout(() => {
      reject('Timeout');
    }, options.timeout || 1000 * 20);

    exec(
      res => {
        if (res && res.err) {
          reject(res.err);
        } else {
          resolve(res);
        }
        window.clearTimeout(timeoutFn);
      },
      err => {
        reject(err);
        window.clearTimeout(timeoutFn);
      },
      PLUGIN_NAME,
      COMMANDS.ADD_FRAME,
      [imageData, options]
      );
  });
};

exports.end = function ({ saveToRoll, timeout }) {
  return new Promise((resolve, reject) => {
    const timeoutFn = window.setTimeout(() => {
      reject('Timeout');
    }, timeout || 1000 * 20);

    exec(
      res => {
        resolve(res);
        window.clearTimeout(timeoutFn);
      },
      err => {
        reject(err);
        window.clearTimeout(timeoutFn);
      },
      PLUGIN_NAME,
      COMMANDS.END,
      [saveToRoll]
    );
  });
};
