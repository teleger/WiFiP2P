package com.jenkolux.wifip2p;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ShowMsg {
    private static final String TAG = "MainActivity";
    Button btnOnoff,btnDiscover,btnSend;
    ListView listView;
    TextView read_msg,connectionStatus;
    EditText writeMsg;


    WifiManager wifiManager;
    WifiP2pManager nManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;

    IntentFilter mIntnerFilter;


    List<WifiP2pDevice> peers = new ArrayList<>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    ConnectInfo info;

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntnerFilter);//注册
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);//取消掉
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        initialWork();//初始化相关工作

        exqListener();
    }

    private void getPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }
    }

    private void exqListener() {
        btnOnoff.setOnClickListener(new View.OnClickListener() {//按钮监听事件
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnoff.setText("ON");
                }else{
                    wifiManager.setWifiEnabled(true);
                    btnOnoff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                nManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connect to "+ device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String msg = writeMsg.getText().toString();
                Runnable runnable = new Runnable(){
                    @Override
                    public void run() {
                        if(info != null){
                            info.write(msg.getBytes());
                        }
                    }
                };
                new Thread(runnable).start();
            }
        });
    }


    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            if(!wifiP2pDeviceList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());

                deviceNameArray = new String[wifiP2pDeviceList.getDeviceList().size()];

                deviceArray = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];

                int index = 0;
                for (WifiP2pDevice device: wifiP2pDeviceList.getDeviceList()){

                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            if(peers.size() == 0){
                Toast.makeText(getApplicationContext(), "No device Found", Toast.LENGTH_SHORT).show();
            }
        }
    };




    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                if(info != null){
                    info.startServer();
                }

            }else if(wifiP2pInfo.groupFormed){
                connectionStatus.setText("Client");
                if(info != null){
                    info.clientPrepare(groupOwnerAddress);
                }
            }
        }
    };

    private void initialWork() {

        btnOnoff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);

        read_msg = findViewById(R.id.readMsg);
        connectionStatus = findViewById(R.id.connectStatus);
        writeMsg = findViewById(R.id.writeMsg);


        //wifi 服务,与蓝牙类似
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        nManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = nManager.initialize(this,getMainLooper(),null);

        mReceiver = new WiFiDirectBroadcastReceiver(nManager,mChannel,this);

        mIntnerFilter = new IntentFilter();

        mIntnerFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntnerFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntnerFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntnerFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        info = new ConnectInfo();
        info.addCallback(this);
    }

    public static final int  MESSAGE_READ = 1;

    @Override
    public void update(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                read_msg.setText(msg);
            }
        });
    }
}
