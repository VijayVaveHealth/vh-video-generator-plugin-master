interface VideoOptions {
  videoFileName: string;
  width: number;
  height: number;
  timestamp: number;
  bit_rate?: number;
  frame_rate?: number;
  quality: number;
  external: boolean;
}

interface VideoEndOptions {
  saveToRoll: boolean;
}

type FrameDataTypes = 0 | 1;

interface FrameOptions {
  type: FrameDataTypes;
  timestamp: number;
}

interface IVideoGenerator {
  start(options: VideoOptions): Promise<string>;

  addFrame(imageData: string | Uint8Array, options: FrameOptions): Promise<string>;

  end(options: VideoEndOptions): Promise<string>;
}

declare const VideoGenerator: IVideoGenerator;
