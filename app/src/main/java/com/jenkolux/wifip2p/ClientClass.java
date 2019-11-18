package com.jenkolux.wifip2p;

import android.os.Handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import androidx.annotation.NonNull;

public class ClientClass extends Thread {
    Socket socket;
    String hostAdd;
    SendReceive sendReceive;

    ShowMsg showMsg;

    public ClientClass(InetAddress hostAddress){
        setName("Client_thread1");
        hostAdd = hostAddress.getHostAddress();
        socket = new Socket();
    }

    public void addCallback(@NonNull ShowMsg showMsg){
        this.showMsg = showMsg;
    }

    @Override
    public void run() {
        try {
            socket.connect(new InetSocketAddress(hostAdd,8888),500);//客户端发起的socket

            sendReceive = new SendReceive(socket);
            sendReceive.addCallback(showMsg);
            sendReceive.start();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
