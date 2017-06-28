package com.zouyingjun.inzone.tp_client;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by zouyingjun on 2017/6/27.
 *
 * An interface-callback for the activity to listen to fragment interaction
 */

public interface DeviceActionListener {

    void showDetails(WifiP2pDevice device);//点击连接设备回调

    void cancelDisconnect();//取消连接回调

    void connect(WifiP2pConfig config);//点击连接按钮

    void disconnect();//点击断开连接按钮
}
