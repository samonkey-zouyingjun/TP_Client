package com.zouyingjun.inzone.tp_client;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

/**
 * Created by zouyingjun on 2017/6/27.
 */

public class DeviceDetailFragment extends Fragment implements
        WifiP2pManager.ConnectionInfoListener {//连接成功的回调
    private View mContentView;
    private WifiP2pInfo info;
    private WifiP2pDevice device;
    ProgressDialog progressDialog;
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;//相册请求码
    protected static final int CHOOSE_RECODER_RESULT_CODE = 30;//录制视频请求码
    private Button mButton;//点击录制视频
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecorder mRecorder;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail,null);

        //获取mMediaProjectionManager
        mMediaProjectionManager = (MediaProjectionManager) getActivity()
                .getSystemService(MEDIA_PROJECTION_SERVICE);

                //开始连接获取连接的信息（如端口）
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiP2pConfig config = new WifiP2pConfig();
                //wifiP2pConfig.groupOwnerIntent 建议设置设置group owner
                config.deviceAddress = device.deviceAddress;//设置连接地址
                config.wps.setup = WpsInfo.PBC;
                //显示进度条
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(),"按返回按键取消","搜索可用设备..."+
                        device.deviceAddress, true, true);

                //连接回调
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //取消连接
                ((MainActivity)getActivity()).disconnect();
            }
        });
                //开始选择相册
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
            }
        });
                //点击开始录制视屏到本地
        mButton = mContentView.findViewById(R.id.btn_start_recoder);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开始录制视屏
                if (mRecorder != null) {
                    mRecorder.quit();
                    mRecorder = null;
                    mButton.setText("Restart recorder");
                } else {
                    //开启录制屏幕
                    Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, CHOOSE_RECODER_RESULT_CODE);
                }
            }
        });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {//承接相册点击事件

        if(CHOOSE_FILE_RESULT_CODE == requestCode) {//加载相册

            //打开相册后，选取照片，返回文件地址
            Uri uri = data.getData();
            TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
            statusText.setText("Sending: " + uri);
            Log.e("zouyingjun", "照片地址：" + uri);
            //把文件写入流中
            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            getActivity().startService(serviceIntent);

        }else if(CHOOSE_RECODER_RESULT_CODE == requestCode){//录屏

            //传递data构建mediaprojection交给ScreenRecorder录制视屏
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null");
                return;
            }

//            Display defaultDisplay = getActivity().getWindowManager().getDefaultDisplay();
//            int width1 = defaultDisplay.getWidth();
//            int height1 = defaultDisplay.getHeight();
//
//
//
//            Log.e("zouyingjun", "width1 :"+width1+"    height1:"+height1);

            // video size
            final int width = 1280;
            final int height = 720;
            File file = new File(Environment.getExternalStorageDirectory(),
                    "record-" + width + "x" + height + "-" + System.currentTimeMillis() + ".mp4");
            final int bitrate = 6000000;
            mRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());//直接传递路径生成文件
//            mRecorder = new ScreenRecorder(mediaProjection);//默认路径：sdcard/test.mp4

            mRecorder.start();
            mButton.setText("Stop Recorder");
            Toast.makeText(getActivity(), "Screen recorder is running...", Toast.LENGTH_SHORT).show();
//            getActivity().moveTaskToBack(true);//类似于home按键（但不是finish 只是在后台运行），false时候必须处于栈顶才能实现

        }
    }

    //重置界面
    public void resetViews() {
        //显示连接按钮
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        //隐藏相册功能
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        //隐藏视频录制功能
        mContentView.findViewById(R.id.btn_start_recoder).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * 流写入
     */
    public static boolean copyFile(InputStream inputStream, OutputStream out) {

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e("zouyingjun", "流写入失败！  "+e.toString());
            return false;
        }
        return true;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //连接成功的回调,根据是推送端还接收端分辨处理
        //更新UI
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        //至此系统已经自动分配好group owner ，IP可知
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // the group owner作服务端，接收流，复制文件到文件夹展示
        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(getActivity(), (TextView) mContentView.findViewById(R.id.status_text))
                    .execute();
        //the group client 做客户端，获取文件路径后，将文件写入流，开启任务栈推送流
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            //相册按钮可见,逻辑在点击回调中
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }
        //显示录制视频按钮
        mContentView.findViewById(R.id.btn_start_recoder).setVisibility(View.VISIBLE);

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    //点击设备后展开设备信息
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }


    class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        public FileServerAsyncTask(Context context, TextView statusText) {
            this.context = context;
            this.statusText = statusText;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                //接收流拷贝文件到文件夹 结束后返回文件路径
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.e("zouyingjun", "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.e("zouyingjun", "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.e("zouyingjun", "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("zouyingjun", "server error!  " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {//取得的文件路径，用相册预览
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }else{
                statusText.setText("传输失败,服务器错误,请查看日志");
            }

        }
        @Override
        protected void onPreExecute() {
            statusText.setText("socket 已经开启，请耐心等待文件传输完毕");
        }
    }
}
