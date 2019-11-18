package com.jenkolux.wifip2p;

import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import androidx.annotation.NonNull;

public class ServerClass extends Thread {
    private Socket socket;
    private ServerSocket serverSocket;

    SendReceive sendReceive = null;

    ShowMsg showMsg;

    ServerClass() {
        setName("Server_thread1");
    }

    public void addCallback(@NonNull ShowMsg showMsg){
        this.showMsg = showMsg;
    }

    private static final int server_port_num = 8888;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(server_port_num);
            socket = serverSocket.accept();

            sendReceive = new SendReceive(socket);
            sendReceive.addCallback(showMsg);
            sendReceive.start();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
