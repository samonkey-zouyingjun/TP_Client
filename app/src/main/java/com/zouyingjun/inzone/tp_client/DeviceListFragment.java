package com.zouyingjun.inzone.tp_client;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zouyingjun on 2017/6/27.
 */

public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener {
    private List<WifiP2pDevice> peers = new ArrayList<>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //设置适配器
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(),R.layout.row_devices,peers));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list,null);
        return mContentView;
    }

    public void onInitiateDiscovery() {
        //取消上一个进度条
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        progressDialog =
        ProgressDialog.show(getActivity(), "按返回按键取消", "搜索可用设备...", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    /**
     * 接收到设备列表广播后回调
     * @param wifiP2pDeviceList
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        //清空进度条
        if(progressDialog !=null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        //清空原来的
        peers.clear();
        //重新赋值
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        //刷新列表
        ((WiFiPeerListAdapter)getListAdapter()).notifyDataSetChanged();
        if(peers.size() == 0){
            Log.e("zouyingjun", "未发现可用设备");
            return;
        }
    }

    //清除设备
    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * 更新设备信息
     * @param device
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }


    public class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>{

        private List<WifiP2pDevice> items;

        public WiFiPeerListAdapter(@NonNull Context context, @LayoutRes int resource, List<WifiP2pDevice> items) {
            super(context, resource,items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v == null){
                LayoutInflater inflayter = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflayter.inflate(R.layout.row_devices,null);
            }

            WifiP2pDevice device = items.get(position);

            if(device != null){
                TextView top = v.findViewById(R.id.device_name);
                TextView bottom = v.findViewById(R.id.device_details);

                if(top!=null){
                    top.setText(device.deviceName);
                }
                if(top!=bottom){
                    bottom.setText(getDeviceStatus(device.status));
                }
            }
            return v;
        }
    }

    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }
}
