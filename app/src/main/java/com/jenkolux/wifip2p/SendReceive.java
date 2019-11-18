package com.jenkolux.wifip2p;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import androidx.annotation.NonNull;

import static com.jenkolux.wifip2p.MainActivity.MESSAGE_READ;

public class SendReceive extends Thread {
    Socket          socket;
    InputStream     inputStream;
    OutputStream    outputStream;

    Handler         handler;

    ShowMsg         showMsg;
    private static final String TAG = "SendReceive";

    public void  addCallback(@NonNull ShowMsg showMsg){
        this.showMsg = showMsg;
    }

    public SendReceive(@NonNull  Socket skt){
        this.socket = skt;
        try{
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                byte[] readBuff = (byte[]) message.obj;
                String tempMsg = new String(readBuff,0,message.arg1);
                showMsg.update(tempMsg);
                return true;
            }
        });

        byte[]  buffer = new byte[1024];
        int     bytes;

        while (socket != null){
            try {
                bytes = inputStream.read(buffer);
                if(bytes > 0){
                    handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        Looper.loop();
    }

    public void write(@NonNull byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
