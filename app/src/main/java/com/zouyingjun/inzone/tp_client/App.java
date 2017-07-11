package com.zouyingjun.inzone.tp_client;

import android.app.Application;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by zouyingjun on 2017/7/5.
 */

public class App extends Application {
    public String EXTRAS_GROUP_OWNER_ADDRESS;
    public final int EXTRAS_GROUP_OWNER_PORT = 8988;
    private Socket socket;
    private ServerSocket serverSocket;
    private boolean isService;//根据自动分配，记录是否为客户端，以便确定socket通讯方式

    public boolean isService() {
        return isService;
    }

    public void setService(boolean service) {
        isService = service;
    }

    public ServerSocket getServerSocket() {
        if(serverSocket !=null){
            return serverSocket;
        }
        try {
            Log.e("zouyingjun", "服务端: " + "开始");
            serverSocket = new ServerSocket(EXTRAS_GROUP_OWNER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serverSocket;
    }

    public Socket getSocket() {

        if (socket != null) return socket;

        if (isService) {
            ServerSocket serverSocket = getServerSocket();
            try {
                socket = serverSocket.accept();
                Log.e("zouyingjun", "服务端: " + "接收");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            socket = new Socket();
            try {
                Log.e("zouyingjun", "客户端: " + "开始");
                socket.bind(null);
                socket.connect((new InetSocketAddress(EXTRAS_GROUP_OWNER_ADDRESS, EXTRAS_GROUP_OWNER_PORT)), 5000);
                Log.e("zouyingjun", "客户端: " + "接收");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    public void resetSocket() {
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }
        }
    }
}
