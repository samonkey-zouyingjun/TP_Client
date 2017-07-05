/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zouyingjun.inzone.tp_client;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 录制视屏的线程 获取到帧数据流编码通过socket发送
 */
public class ScreenRecorder extends Thread {
    private static final String TAG = "ScreenRecorder";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private String mDstPath;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int TIMEOUT_US = 10000;

    private MediaCodec mEncoder;
    private Surface mSurface;
    private int mVideoTrackIndex = -1;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;

    public ScreenRecorder(int width, int height, int bitrate, int dpi, MediaProjection mp, String dstPath) {
        super(TAG);
        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        mDpi = dpi;
        mMediaProjection = mp;
        mDstPath = dstPath;
    }


    public ScreenRecorder(MediaProjection mp) {
        // 480p 2Mbps
        this(640, 480, 2000000, 1, mp, "/sdcard/test.mp4");
    }

    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @Override
    public void run() {
        try {
            try {
                prepareEncoder();//编码前格式设置,并生成渲染的容器
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //根据指定参数和容器（mSurface）生成屏幕映射(mVirtualDisplay)
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mSurface, null, null);
            Log.d(TAG, "created virtual display: " + mVirtualDisplay);
            //同步处理数据  不用理会编解码器是否已经准备好接收数据（有风险）
            recordVirtualDisplay();



        } finally {
            release();//释放资源
        }
    }


    String path = Environment.getExternalStorageDirectory() + "/zyj.h264";

    private void recordVirtualDisplay() {//http://blog.csdn.net/jinzhuojun/article/details/32163149
        //设置循环去遍历输出缓冲区域
        ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();

        while (!mQuit.get()) {
            //返回一个inputbuffer的索引用来填充数据，返回-1表示暂无可用buffer
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);

            if(index >= 0){
                ByteBuffer outputBuffer = outputBuffers[index];
                byte[] outData = new byte[mBufferInfo.size];
                outputBuffer.get(outData);

                //记录pps和sps
                byte[] mPpsSps = new byte[0];
                if (outData[0] == 0 && outData[1] == 0 && outData[2] == 0 && outData[3] == 1 && outData[4] == 103) {
                    mPpsSps = outData;
                } else if (outData[0] == 0 && outData[1] == 0 && outData[2] == 0 && outData[3] == 1 && outData[4] == 101) {
                    //在关键帧前面加上pps和sps数据
                    byte[] iframeData = new byte[mPpsSps.length + outData.length];
                    System.arraycopy(mPpsSps, 0, iframeData, 0, mPpsSps.length);
                    System.arraycopy(outData, 0, iframeData, mPpsSps.length, outData.length);
                    outData = iframeData;
                }
                Util.save(outData, 0, outData.length, path, true);
                mEncoder.releaseOutputBuffer(index, false);

            }
        }
    }


    private void encodeToVideoTrack(int index) {

        ByteBuffer encodedData = mEncoder.getOutputBuffer(index);//就是实时视频数据

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodedData != null) {
            encodedData.position(mBufferInfo.offset);
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
            Log.i(TAG, "sent " + mBufferInfo.size + " bytes to muxer...");
        }
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once

        MediaFormat newFormat = mEncoder.getOutputFormat();//获取的视频流

        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());

        Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        Log.d(TAG, "created video format: " + format);
        //初始化H246编码器
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        //设置编码信息
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //生成等待VirtualDisplay渲染的容器
        mSurface = mEncoder.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
        //等候编码
        mEncoder.start();
    }

    private void release() {
        //编码器
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        //屏幕映射
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        //媒体管理器
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }
}
