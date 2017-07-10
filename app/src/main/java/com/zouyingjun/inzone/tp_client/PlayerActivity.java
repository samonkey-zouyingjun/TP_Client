package com.zouyingjun.inzone.tp_client;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.vonchenchen.android_video_demos.codec.FrameRender;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PlayerActivity extends Activity {

    private SurfaceView sv;
    private ServerSocket serverSocket;
    private SurfaceHolder holder;
    private FrameRender frameRender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//屏幕常亮
        setContentView(R.layout.activity_player);
        sv = findViewById(R.id.sv);
        holder = sv.getHolder();
        frameRender = new FrameRender(holder.getSurface());
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                //开启解码线程
                new Thread(read).start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });


    }


    private void test_decode(FrameRender frameRender, InputStream inputStream){
        byte[] buffer = new byte[1024*8];

        //FrameRender frameRender = new FrameRender(mHolder.getSurface());

        int len;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
//                long start = System.currentTimeMillis();
                frameRender.decodeStream(buffer, len);
//                long end = System.currentTimeMillis();
//                Log.i("mCodecWrapper", "spend "+(end-start));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
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


                test_decode(frameRender,fs);


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
