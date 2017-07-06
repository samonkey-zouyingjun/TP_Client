package com.zouyingjun.inzone.tp_client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by zouyingjun on 2017/6/27.
 *
 *
 *
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity ;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        switch (action){
            //表示Wi-Fi对等网络状态发生了改变
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean b = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                if(b){
                    //记录Wifi 是否可状态
                    activity.setWifiP2pEnabled(true);
                }else{
                    //刷新主界面
                    activity.setWifiP2pEnabled(false);
                    activity.resetData();
                }
            Log.e("zouyingjun", ""+"Wi-Fi对等网络状态发生了改变"+"  P2p是否可用？"+b);
                break;
            //表示可用的对等点的列表发生了改变_点击搜素成功获取到设备列表后收到
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:

                if(manager !=null){
                    manager.requestPeers(channel, (WifiP2pManager.PeerListListener) activity.getFragmentManager().findFragmentById(R.id.frag_list));
                }

                Log.e("zouyingjun", "可用的对等点的列表发生了改变"+"");

                break;
            //表示Wi-Fi对等网络的连接状态发生了改变_点击connect成功联接后收到此通知
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:

                if(manager == null){
                    return;
                }
                NetworkInfo networkInfo = intent.
                        getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);//获取网络信息

                if(networkInfo.isConnected()){
                    DeviceDetailFragment fragment = (DeviceDetailFragment) activity.
                            getFragmentManager().findFragmentById(R.id.frag_detail);
                    manager.requestConnectionInfo(channel, fragment);//在fragment中处理连接成功的逻辑

                }else{
                    if(((App)activity.getApplication()).isClientState == 1){//关闭播放界面

                        Context baseContext = activity.getBaseContext();
                        Intent intent1 = new Intent(baseContext, MainActivity.class);
                        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        baseContext.startActivity(intent1);
                    }

                    // 刷新界面
                    activity.resetData();
                }
                Log.e("zouyingjun", "Wi-Fi对等网络的连接状态发生了改变"+"");
                break;
            //设备配置信息发生了改变_读取自身设备信息
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                //更新设备信息
                DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
                fragment.updateThisDevice(device);
                Log.e("zouyingjun", "更新本机设备信息");
                break;
            //p2p是否在运行或者停止工作
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                break;
        }



    }
}
