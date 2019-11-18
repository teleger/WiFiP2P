package com.jenkolux.wifip2p;

import java.net.InetAddress;

public class ConnectInfo {
    private static final String TAG = "ConnectInfo";
    ServerClass serverClass = null;
    ClientClass clientClass = null;

    ShowMsg showMsg;

    public void addCallback(ShowMsg showMsg){
        this.showMsg = showMsg;
    }

    ConnectInfo(){
    }

    public void startServer(){
        serverClass = new ServerClass();
        serverClass.addCallback(showMsg);
        serverClass.start();
    }

    public void clientPrepare(InetAddress inetAddress){
        clientClass = new ClientClass(inetAddress);
        clientClass.addCallback(showMsg);
        clientClass.start();
    }

    void write(byte[] data){
        if(serverClass != null){
            if(serverClass.sendReceive != null){
                serverClass.sendReceive.write(data);
            }
        }

        if(clientClass != null){
            if(clientClass.sendReceive != null){
                clientClass.sendReceive.write(data);
            }
        }
    }

}
