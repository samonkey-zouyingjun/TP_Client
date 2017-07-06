package com.zouyingjun.inzone.tp_client;

import android.app.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by zouyingjun on 2017/7/5.
 */

public class App extends Application {
    public String EXTRAS_GROUP_OWNER_ADDRESS;
    public int EXTRAS_GROUP_OWNER_PORT;
    public Socket socket ;
    public int isClientState ;//-1:客户端，1服务端

    public Socket getSocket (){
        if(socket == null){
            socket = new Socket();
            try {
                socket.bind(null);
                socket.connect((new InetSocketAddress(EXTRAS_GROUP_OWNER_ADDRESS, EXTRAS_GROUP_OWNER_PORT)), 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    public void resetSocket(){
        if(socket.isConnected()){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                socket = null;
            }
        }
    }


}
