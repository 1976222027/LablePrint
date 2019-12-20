package com.jimmy.printer.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jimmy.printer.common.SendCallback;
import com.jimmy.printer.common.SendResultCode;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothPrint {
    private static final String TAG = "BluetoothPrint";

    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static BluetoothPrint INSTANCE;
    private SendCallback sendCallback;
    private UUID uuid;
    private MyHandler myHandler;
    private final ExecutorService threadPool;

    private BluetoothPrint(SendCallback sendCallback) {
        this.uuid = UUID.fromString(SPP_UUID);
        this.myHandler = new MyHandler(this);
        this.sendCallback = sendCallback;
        this.threadPool = Executors.newFixedThreadPool(3);
    }

    public static BluetoothPrint getInstance(SendCallback sendCallback) {
        if (INSTANCE == null) {
            synchronized (BluetoothPrint.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluetoothPrint(sendCallback);
                }
            }
        }
        return INSTANCE;
    }

    public void sendPrintCommand(BluetoothDevice device, byte[] bytes) {
        SendCommandThread thread = new SendCommandThread(device, bytes);
        threadPool.execute(thread);
    }


    //添加的
//  public void sendPrintCommand(BluetoothManager device, byte[] bytes) {
//      BluetoothManager btManager = BluetoothManager.getInstance();
//      if (btManager.hasConnectedDevice()) {
//          btManager.printText(bytes, new BluetoothManager.OnPrintListener() {
//              @Override
//              public void onPrintFinished() {
////                  showPrintFinished();
//              }
//
//              @Override
//              public void onPrintFail(final BluetoothDevice device) {
////                  showPrintFailed(device.getName());
//              }
//          });
//      }
//
//  }
//服务端侦听到连接
    public void onBlueAccept(BluetoothSocket socket) {
        if (socket != null) {
            BluetoothDevice device = socket.getRemoteDevice();//获取远程设备信息
           Log.e(device.getName(),device.getAddress());
        }
    }

    private class SendCommandThread extends Thread {
        private BluetoothDevice device;
        private byte[] bytes;
        private BluetoothSocket socket;

        public SendCommandThread(BluetoothDevice device, byte[] bytes) {
            this.device = device;
            this.bytes = bytes;
        }

        @Override
        public void run() {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();//connect：建立蓝牙的socket连接；
               // close：关闭蓝牙的socket连接；
                OutputStream os = socket.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
                sendStatus(device.getName(), SendResultCode.SEND_SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
                sendStatus(device.getName(), SendResultCode.SEND_FAILED);
            }
        }
    }

    private void sendStatus(String ip, int code) {
        Message msg = Message.obtain();
        msg.what = code;
        msg.obj = ip;
        myHandler.sendMessage(msg);
    }

    private static class MyHandler extends Handler {

        private WeakReference<BluetoothPrint> reference;

        public MyHandler(BluetoothPrint manager) {
            this.reference = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothPrint manager = reference.get();
            if (manager != null) {
                manager.sendCallback.onCallback(msg.what, (String) msg.obj);
            }
        }
    }

    //只针对HEADSET设备
    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET)
                == /*BluetoothHeadset*/BluetoothProfile.STATE_CONNECTED;
    }

    //判断蓝牙是否连接
    public void getBlueStates(Context mContext) {
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mAdapter != null && mAdapter.isEnabled()){
////        Intent intent = new Intent(Intent.ACTION_HEADSET_PLUG);
//        if(BluetoothProfile.STATE_CONNECTED == mAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
////            intent.putExtra("state", 1);
////            intent.putExtra("microphone", 1);
////            mContext.sendBroadcast(intent);
//            msg.what = 1;
//            msg.obj = 1;
//            myHandler.sendMessage(msg);
//        }
//        else if(BluetoothProfile.STATE_DISCONNECTED == mAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
////            intent.putExtra("state", -1);
////            mContext.sendBroadcast(intent);
//            msg.what = -1;
//            myHandler.sendMessage(msg);
//        }}
//蓝牙已打开
        if (mAdapter != null && mAdapter.isEnabled()) {
            int a2dp = mAdapter.getProfileConnectionState(BluetoothProfile.A2DP); // 可操控蓝牙设备，如带播放暂停功能的蓝牙耳机
            int headset = mAdapter.getProfileConnectionState(BluetoothProfile.HEADSET); // 蓝牙头戴式耳机，支持语音输入输出
            int health = mAdapter.getProfileConnectionState(BluetoothProfile.HEALTH); // 蓝牙穿戴式设备
            int GATT = mAdapter.getProfileConnectionState(BluetoothProfile.GATT);
            Log.e("lqq", "a2dp=" + a2dp + ",headset=" + headset + ",health=" + health);

            // 查看是否蓝牙是否连接到三种设备的一种，以此来判断是否处于连接状态还是打开并没有连接的状态
//STATE_DISCONNECTED
            int flag = -1;
            if (a2dp == BluetoothProfile.STATE_CONNECTED) {
                flag = a2dp;
            } else if (headset == BluetoothProfile.STATE_CONNECTED) {
                flag = headset;
            } else if (health == BluetoothProfile.STATE_CONNECTED) {
                flag = health;
            } else if (GATT == BluetoothProfile.STATE_CONNECTED) {
                flag = GATT;
            }
            if (flag != -1) {
                sendStatus("蓝牙已连接", SendResultCode.SEND_SUCCESS);
            } else if (flag == -1) {
                //蓝牙手机相互配对连接
                NetworkInfo netInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH);
                if (netInfo == null) {
                    sendStatus("蓝牙已打开", SendResultCode.SEND_SUCCESS);
                } else {
                    sendStatus("蓝牙已连接", SendResultCode.SEND_SUCCESS);
                    // 系统内部，返回连接与否
                }
            }
        } else {
            sendStatus("蓝牙已关闭", SendResultCode.SEND_FAILED);
        }
    }

    public void closebr(Context mContext) {
        //注销蓝牙监听
        mContext.unregisterReceiver(mReceiver);
    }

    public void blueState(Context mContext) {
        //注册蓝牙监听
        mContext.registerReceiver(mReceiver, makeFilter());
    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("TAG", "TURNING_ON");
//                            sendStatus("打开蓝牙", SendResultCode.SEND_SUCCESS);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.e("TAG", "STATE_ON");
                            sendStatus("蓝牙已打开", SendResultCode.SEND_SUCCESS);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("TAG", "STATE_TURNING_OFF");
//                            sendStatus("关闭拉蓝牙", SendResultCode.SEND_FAILED);
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            sendStatus("未开启蓝牙", SendResultCode.SEND_FAILED);
                            Log.e("TAG", "STATE_OFF");
                            break;
                    }
                    break;
            }
        }
    };
}
