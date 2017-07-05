package com.zouyingjun.inzone.tp_client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;

/**
 * WiFiDirectActivity
 */
public class MainActivity extends Activity implements DeviceActionListener{
    private boolean isWifiP2pEnabled = false;//收到广播后记录p2p可用状态
    private WiFiDirectBroadcastReceiver receiver;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter = new IntentFilter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //设置广播过滤器
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //初始化
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

    }

    public void setWifiP2pEnabled(boolean wifiP2pEnabled) {
        isWifiP2pEnabled = wifiP2pEnabled;
    }

    //重置
    public void resetData() {
        //重置list
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().
                findFragmentById(R.id.frag_list);
        if(fragmentList != null){
            fragmentList.clearPeers();
        }
        //重置info
        DeviceDetailFragment fragmentDetail = (DeviceDetailFragment) getFragmentManager().
                findFragmentById(R.id.frag_detail);
        if(fragmentList != null){
            fragmentDetail.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.action_items,menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.atn_direct_enable://设置wifi
                if(manager !=null && channel !=null){
                    //跳转设置界面
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }else{
                    //不可用
                    Log.e("zouyingjun", "manager or channel is null！"+"");
                }
                return true;
            case R.id.atn_direct_discover://开始搜索
                searchPears();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean searchPears() {
        //开启服务前提是P2p可用
        if(!isWifiP2pEnabled){
            Toast.makeText(MainActivity.this, "wifiP2p不可用",
                    Toast.LENGTH_SHORT).show();
            return true;
        }


        DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().
                findFragmentById(R.id.frag_list);
        //启用进度条
        fragment.onInitiateDiscovery();
        //开始搜索设备
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
//                        Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
                // 剩下逻辑在广播中处理
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(MainActivity.this, "搜索失败 错误码："+i, Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册广播
        receiver = new WiFiDirectBroadcastReceiver(manager,channel,this);
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onPause() {
       super.onPause();
        //取消广播
        unregisterReceiver(receiver);
    }

    //--------------DeviceActionListener start----------------
    @Override
    public void showDetails(WifiP2pDevice device) {//点击设备后回调的
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    @Override
    public void cancelDisconnect() {

    }

    @Override
    public void connect(WifiP2pConfig config) {//点击设备的连接按钮后
        //设置参数后，开启连接
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver会收到通知，这里暂时不处理
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(MainActivity.this, "连接失败! 失败码："+i, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void disconnect() {//点击断开按钮后段开连接
        //清空显示设备
        final DeviceDetailFragment fragmentDetail = (DeviceDetailFragment) getFragmentManager().
                findFragmentById(R.id.frag_detail);

        fragmentDetail.resetViews();

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                fragmentDetail.getView().setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int i) {
                Log.e("zouyingjun", "断开连接异常，错误代码："+i);
            }
        });
    }
    //--------------DeviceActionListener end----------------


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Socket socket = ((App) getApplication()).socket;
        if(socket.isConnected()){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
