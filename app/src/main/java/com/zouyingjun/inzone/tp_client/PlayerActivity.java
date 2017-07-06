package com.zouyingjun.inzone.tp_client;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class PlayerActivity extends Activity {

    // Video Constants
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 1280;
    private final static int VIDEO_HEIGHT = 720;
    private final static int TIME_INTERNAL = 30;
    private final static int HEAD_OFFSET = 512;

    private MediaCodec mCodec;
    private SurfaceView sv;
    private ServerSocket serverSocket;
    private boolean readFlag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.activity_player);
        sv = findViewById(R.id.sv);

        //开启解码线程
        new Thread(read).start();
    }


    private Runnable read = new Runnable() {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8988);

                Log.e("zouyingjun", "server opening"+"");
                Socket client = serverSocket.accept();
                Log.e("zouyingjun", "server ok"+"");
                InputStream fs = client.getInputStream();//获取流
                Log.e("zouyingjun", "server getstream"+"");

                //对输入流解码
                int frameOffset = 0;
                byte[] buffer = new byte[100000];
                byte[] framebuffer = new byte[200000];
                readFlag = true;
                BufferedInputStream is = new BufferedInputStream(fs);
                while (!Thread.interrupted() && readFlag) {
                    try {
                        int length = is.available();
                        if (length > 0) {
                            // Read file and fill buffer
                            int count = is.read(buffer);
                            Log.e("zouyingjun", "读取到字符数量："+count);
                            // Fill frameBuffer
                            if (frameOffset + count < 200000) {
                                System.arraycopy(buffer, 0, framebuffer,
                                        frameOffset, count);
                                frameOffset += count;
                            } else {
                                frameOffset = 0;
                                System.arraycopy(buffer, 0, framebuffer,
                                        frameOffset, count);
                                frameOffset += count;
                            }

                            // Find H264 head
                            int offset = findHead(framebuffer, frameOffset);
                            Log.e("zouyingjun", "找到的h264头文件角标："+offset);
                            while (offset > 0) {
                                if (checkHead(framebuffer, 0)) {
                                    // Fill decoder
                                    boolean flag = onFrame(framebuffer, 0, offset);
                                    if (flag) {
                                        byte[] temp = framebuffer;
                                        framebuffer = new byte[200000];
                                        System.arraycopy(temp, offset, framebuffer,
                                                0, frameOffset - offset);
                                        frameOffset -= offset;
                                        Log.e("zouyingjun", "is Head:" + offset);
                                        // Continue finding head
                                        offset = findHead(framebuffer, frameOffset);
                                    }
                                } else {

                                    offset = 0;
                                }

                            }
                            Log.d("zouyingjun", "end loop");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(TIME_INTERNAL);
                    } catch (InterruptedException e) {

                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    int mCount = 0;

    public boolean onFrame(byte[] buf, int offset, int length) {

        if(mCodec == null){

            try {
                mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
                mCodec.configure(mediaFormat, sv.getHolder().getSurface(), null, 0);
                mCodec.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        Log.e("Media", "onFrame start");
        Log.e("Media", "onFrame Thread:" + Thread.currentThread().getId());
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);

        Log.e("Media", "onFrame index:" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        Log.e("Media", "onFrame end");
        return true;
    }

    /**
     * Find H264 frame head
     *
     * @param buffer
     * @param len
     * @return the offset of frame head, return 0 if can not find one
     */
    private int findHead(byte[] buffer, int len) {
        int i;
        for (i = HEAD_OFFSET; i < len; i++) {
            if (checkHead(buffer, i))
                break;
        }
        if (i == len)
            return 0;
        if (i == HEAD_OFFSET)
            return 0;
        return i;
    }

    /**
     * Check if is H264 frame head
     *
     * @param buffer
     * @param offset
     * @return whether the src buffer is frame head
     */
    private boolean checkHead(byte[] buffer, int offset) {
        // 00 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 0 && buffer[3] == 1)
            return true;
        // 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 1)
            return true;
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        readFlag = false;
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //编码器
        if (mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }
}
