package com.apicloud.modulelabelprinter;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import com.tools.io.BluetoothPort;
import com.tools.io.PortManager;
import com.tools.io.UsbPort;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.apicloud.modulelabelprinter.Constant.ACTION_USB_PERMISSION;
import static com.apicloud.modulelabelprinter.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.apicloud.modulelabelprinter.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.apicloud.modulelabelprinter.DeviceConnFactoryManager.CONN_STATE_FAILED;


/**
 * Created by Administrator on 2018/8/25.
 */

public class LabelPrinter extends UZModule {
    UsbPrinter usbPrinter = null;
    private static Context mContext;
    boolean initB = false;
    private ThreadPool threadPool;
    private int id = 0;

    public LabelPrinter(UZWebView webView) {
        super(webView);
        mContext = getContext();
    }

    public static Context getContexts() {
        return mContext;
    }

    /**
     * 获取通过Usb外接打印机的设备信息
     *
     * @param moduleContext
     */
    public void jsmethod_printerUsb(final UZModuleContext moduleContext) {
        UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        JSONObject json = new JSONObject();
        int i = 0;
        while (deviceIterator.hasNext()) {
            UsbDevice usbDevice = deviceIterator.next();
            mPort = new UsbPort(getContext(), usbDevice);//add
            int deviceClass = usbDevice.getDeviceClass();
            if (deviceClass == 0) {
                UsbInterface anInterface = usbDevice.getInterface(0);
                int interfaceClass = anInterface.getInterfaceClass();
                if (interfaceClass == 7) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("device", usbDevice.getDeviceName());//得到usb外接设备的路径
                        jsonObject.put("name", usbDevice.getProductName());
                        jsonObject.put("Vid", usbDevice.getVendorId());
                        jsonObject.put("Pid", usbDevice.getProductId());
                        json.put(i + "", jsonObject);
                        i++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        JSONObject ret = new JSONObject();
        try {
            ret.put("json", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        moduleContext.success(ret, true);
    }

    public void jsmethod_requestPermission(final UZModuleContext moduleContext) {
        UsbPrinter.requestUsbPrinter(getContext(), moduleContext);
    }

    /**
     * 标签打印机初始化
     *
     * @param moduleContext
     */
    UZModuleContext backContext;
    boolean isUsb = false;
    boolean isBluetooth = false;

    String macAddress;
    int size;

    public void jsmethod_initPrint(final UZModuleContext moduleContext) {
//        int vid = moduleContext.optInt("vid",1137);
//        int pid = moduleContext.optInt("pid",85);

//        UsbPrinter.requestUsbPrinter(getContext());
//        UsbPrinter.requestUsbPrinter(getContext(), moduleContext);
        String type = moduleContext.optString("type", "usb");
        macAddress = moduleContext.optString("address");
        size = moduleContext.optInt("size", 0);
        if ("usb".equals(type)) {
            try {
                usbPrinter = UsbPrinter.open(getContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (usbPrinter.ready()) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    initB = true;
                    isUsb = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(ret, true);
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(ret, true);
            }
        } else if ("bluetooth".equals(type)) {
            backContext = moduleContext;
            // Get the local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // If the adapter is null, then Bluetooth is not supported
            if (mBluetoothAdapter == null) {
                Utils.toast(getContext(), "设备不支持蓝牙");
            } else {
                mPort = new BluetoothPort(macAddress);// add

                // If BT is not on, request that it be enabled.
                // setupChat() will then be called during onActivityResult
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent,
                            REQUEST_ENABLE_BS);
                } else {
                    mBluetooth();
                }
            }
        }

    }

    public void mBluetooth() {
        if (macAddress.isEmpty()) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入蓝牙的Macaddress");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            backContext.success(ret, false);
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_QUERY_PRINTER_STATE);
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        getContext().registerReceiver(receiver, filter);
        //蓝牙连接
        closeport();
        /*获取蓝牙mac地址*/
//        String macAddress = address;
        //初始化话DeviceConnFactoryManager
        new DeviceConnFactoryManager.Build()
                .setId(id)
                //设置连接方式
                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                //设置连接的蓝牙mac地址
                .setMacAddress(macAddress)
                .build();
        //打开端口
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask(new Runnable() {
            @Override
            public void run() {
                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
            }
        });
    }

    /**
     * 断开蓝牙连接
     *
     * @param moduleContext
     */
    public void jsmethod_disconnect(UZModuleContext moduleContext) {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
        }
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort == null) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public PortManager mPort;

    /**
     * 打印标签
     *
     * @param moduleContext
     */
    public void jsmethod_printerLabel(final UZModuleContext moduleContext) {
        if (!initB) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("err", "打印机未初始化成功");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            moduleContext.success(ret, true);
            return;
        }

        JSONArray jsonArray = moduleContext.optJSONArray("data");
//        标签打印
        LabelCommand tsc = new LabelCommand();
        if (0 == size) {
            tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置
            Log.e("wang", "尺寸为40x30");
        } else if (1 == size) {
            tsc.addSize(50, 30); // 设置标签尺寸，按照实际尺寸设置
        } else {
            tsc.addSize(80, 45); // 设置标签尺寸，按照实际尺寸设置
        }
        tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(LabelUtils.ENABLE.ON); // 撕纸模式开启
        tsc.addCls();// 清除打印缓冲区
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject itemJson = jsonArray.getJSONObject(i);
                String barCode = itemJson.optString("barCode");
                String name = itemJson.optString("name");
                String time = itemJson.optString("labeldate");
                double price = itemJson.optDouble("price", 0.00);
                double vipPrice = itemJson.optDouble("vipPrice", 0.00);
                String endnotes = itemJson.optString("endnotes");
                int number = itemJson.optInt("number", 1);
                if (TextUtils.isEmpty(barCode)) {
                    continue;
                }
                int x = 0;
                if (0 == size) {
                    x = 25;
                    Log.e("wang", "===25");
                } else if (1 == size) {
                    x = 31;
                } else {
                    x = 30;
                }
                int length = getlength(name);
                if (length < x) {
                    int num = x - length;
                    for (int k = 0; k < num / 2; k++) {
                        name = " " + name + "";
                    }
                }
                Log.e("wang", length + "");
                if (!TextUtils.isEmpty(barCode)) {
//                    if (0 == size) {
                    tsc.add1DBarcode(8, 75, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
//                    } else if (1 == size) {
//                        tsc.add1DBarcode(20, 75, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
//                    }
                }
                if (!TextUtils.isEmpty(name)) {
                    tsc.addText(0, 15, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                            name);
                }
                if (price != 0) {
//                    if (0 == size) {
                    tsc.addText(8, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                            "单价:" + price);
//                    } else {
//                        tsc.addText(18, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                                "单价:" + price);
//                    }
                }
                if (vipPrice != 0) {
                    if (0 == size) {
                        tsc.addText(145, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                "会员价:" + vipPrice);
                    } else if (1 == size) {
                        tsc.addText(160, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                "会员价:" + vipPrice);
                    }

                }
                if (!TextUtils.isEmpty(time)) {
//                    if (0 == size) {
                    tsc.addText(8, 165, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                            "日期:" + time);
//                    } else if (1 == size) {
//                        tsc.addText(20, 165, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                                "日期:" + time);
//                    }
                }
                if (!TextUtils.isEmpty(endnotes)) {
//                    if (0 == size) {
                    tsc.addText(8, 195, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                            endnotes);
//                    } else if (1 == size) {
//                        tsc.addText(20, 195, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                                endnotes);
//                    }
                }
                tsc.addPrint(1, number); // 打印标签
                tsc.addCls();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
//        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        if (isUsb) {
//            Toast.makeText(getContext(), "打印-1", Toast.LENGTH_SHORT).show();
//            if (this.mPort == null) {
//                return;
//            }
//            try {
//                Toast.makeText(getContext(), "打印2", Toast.LENGTH_SHORT).show();
//                //  Log.e(TAG, "data -> " + new String(com.gprinter.command.GpUtils.convertVectorByteTobytes(data), "gb2312"));
//                this.mPort.writeDataImmediately(datas, 0, datas.size());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            byte[] bytes = LabelUtils.ByteTo_byte(datas);
//           、、 String str = Base64.encodeToString(bytes, Base64.DEFAULT);
//           、、 byte[] decode_datas = Base64.decode(str, Base64.DEFAULT);
            try {
                usbPrinter.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (isBluetooth) {
            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null) {
                return;
            }
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
        }
    }


    /**
     * 判断字符串长度
     *
     * @param s
     * @return
     */
    public static int getlength(String s) {
        if (s == null) {
            return 0;
        }
        char[] c = s.toCharArray();
        int len = 0;
        for (int i = 0; i < c.length; i++) {
            len++;
            if (!isLetter(c[i])) {
                len++;
            }
        }
        return len;
    }

    public static boolean isLetter(char c) {
        int k = 0x80;
        return c / k == 0 ? true : false;
    }

    /**
     * 连接蓝牙
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                System.out.println("permission ok for device " + device);
//                            usbConn(device);
                            }
                        } else {
                            System.out.println("permission denied for device " + device);
                        }
                    }
                    break;
                //Usb连接断开、蓝牙连接断开广播
                case ACTION_USB_DEVICE_DETACHED:
                    mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
                    break;
                case DeviceConnFactoryManager.ACTION_CONN_STATE:
                    int state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1);
                    int deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1);
                    switch (state) {
                        case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
                            if (id == deviceId) {
//                                tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                                Toast.makeText(context, R.string.str_conn_state_disconnect, Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
//                            tvConnState.setText(getString(R.string.str_conn_state_connecting));
                            Toast.makeText(context, R.string.str_conn_state_connecting, Toast.LENGTH_SHORT).show();
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                            JSONObject ret = new JSONObject();
                            try {
                                ret.put("status", true);
                                initB = true;
                                isBluetooth = true;
                                ret.put("msg", getConnDeviceInfo());
                                backContext.success(ret, false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                        Toast.makeText(context, R.string.str_conn_state_connected + "\n" + getConnDeviceInfo(), Toast.LENGTH_SHORT).show();
                            break;
                        case CONN_STATE_FAILED:
                            JSONObject ret1 = new JSONObject();
                            try {
                                ret1.put("status", false);
//                                ret1.put("msg", R.string.str_conn_fail);
                                ret1.put("msg", "连接失败");
                                backContext.success(ret1, false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                        Utils.toast(getContext(), getContext().getString(R.string.str_conn_fail));
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_QUERY_PRINTER_STATE:
//                    if (counts >=0) {
//                        if(continuityprint) {
//                            printcount++;
//                            Utils.toast(MainActivity.this, getString(R.string.str_continuityprinter) + " " + printcount);
//                        }
//                        if(counts!=0) {
//                            sendContinuityPrint();
//                        }else {
//                            continuityprint=false;
//                        }
//                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 重新连接回收上次连接的对象，避免内存泄漏
     */
    private void closeport() {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
        }

    }

    private static final int CONN_PRINTER = 0x12;
    /**
     * 连接状态断开
     */
    private static final int CONN_STATE_DISCONN = 0x007;
    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONN_STATE_DISCONN:
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                        Utils.toast(getContext(), getContext().getString(R.string.str_disconnect_success));
                    }
                    break;
                case PRINTER_COMMAND_ERROR:
                    Utils.toast(getContext(), getContext().getString(R.string.str_choice_printer_command));
                    break;
                case CONN_PRINTER:
                    Utils.toast(getContext(), getContext().getString(R.string.str_cann_printer));
                    break;
                case MESSAGE_UPDATE_PARAMETER:
                    String strIp = msg.getData().getString("Ip");
                    String strPort = msg.getData().getString("Port");
                    //初始化端口信息
                    new DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp(strIp)
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(Integer.parseInt(strPort))
                            .build();
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
                default:
                    new DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp("192.168.2.227")
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(9100)
                            .build();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
            }
        }
    };

    private String getConnDeviceInfo() {
        String str = "";
        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if (deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState()) {
            if ("USB".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "USB\n";
                str += "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ("WIFI".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "WIFI\n";
                str += "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str += "Port: " + deviceConnFactoryManager.getPort();
            } else if ("BLUETOOTH".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "BLUETOOTH\n";
                str += "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ("SERIAL_PORT".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "SERIAL_PORT\n";
                str += "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str += "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return str;
    }

    /**
     * 扫描蓝牙设备
     */
    UZModuleContext lanYaContext;
    int i;
    JSONObject json;
    /**
     * 扫描蓝牙设备
     */
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> DevicesArrayAdapter;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_ENABLE_BS = 3;

    public void jsmethod_ScanBluetooth(UZModuleContext moduleContext) {
        lanYaContext = moduleContext;
        i = 0;
        json = new JSONObject();
        IntentFilter filters = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(mFindBlueToothReceiver, filters);
        // Register for broadcasts when discovery has finished
        filters = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getContext().registerReceiver(mFindBlueToothReceiver, filters);
        initBluetooth();
        discoveryDevice();
    }

    private void discoveryDevice() {
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    private void initBluetooth() {
        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Utils.toast(getContext(), "Bluetooth is not supported by the device");
        } else {
            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,
                        REQUEST_ENABLE_BT);
            } else {
                getDeviceList();
            }
        }
    }
    /**
     * ESC查询打印机实时状态指令
     */
    private byte[] esc = { 0x10, 0x04, 0x02 };


    /**
     * CPCL查询打印机实时状态指令
     */
    private byte[] cpcl = { 0x1b, 0x68 };


    /**
     * TSC查询打印机状态指令
     */
    private byte[] tsc = { 0x1b, '!', '?' };
//打印机状态查询
    public void btnPrinterState() {
        /* 打印机状态查询 */
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(getContext(), "请先连接打印机");
            return;
        }
        ThreadPool.getInstantiation().addTask(new Runnable() {
            @Override
            public void run() {
                Vector<Byte> data = new Vector<>(esc.length);
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                    for (int i = 0; i < esc.length; i++) {
                        data.add(esc[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(data);
                } else if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC) {
                    for (int i = 0; i < tsc.length; i++) {
                        data.add(tsc[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(data);
                } else if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL) {
                    for (int i = 0; i < cpcl.length; i++) {
                        data.add(cpcl[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(data);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // bluetooth is opened
                getDeviceList();
            } else {
                // bluetooth is not open
                Toast.makeText(getContext(), R.string.bluetooth_is_not_enabled, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BS) {
            if (resultCode == Activity.RESULT_OK) {
                // bluetooth is opened
                mBluetooth();
            } else {
                // bluetooth is not open
                Toast.makeText(getContext(), R.string.bluetooth_is_not_enabled, Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void getDeviceList() {
        DevicesArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.bluetooth_device_name_item);
        // Get a set of currently paired devices获取当前已成对的设备
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        DevicesArrayAdapter.add(getContext().getString(R.string.str_title_pairedev));
        if (pairedDevices.size() > 0) {
            //  tvPairedDevice.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                DevicesArrayAdapter.add(device.getName() + "\n"
                        + device.getAddress());
                Log.e("wang", device.getName() + "=======" + device.getAddress());
            }

        } else {
            String noDevices = getContext().getResources().getText(R.string.none_paired)
                    .toString();
            DevicesArrayAdapter.add(noDevices);
        }
    }

    private final BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    DevicesArrayAdapter.add(device.getName() + "\n"
                            + device.getAddress());
                    try {
                        json.put("" + i, device.getName() + "----" + device.getAddress());
                        i++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("wang", device.getName() + "=-----------" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
//                Log.e("wang", "finish discovery" + (DevicesArrayAdapter.getCount() - 2));
                if (DevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getContext().getResources().getText(
                            R.string.none_bluetooth_device_found).toString();
                    DevicesArrayAdapter.add(noDevices);
                } else {
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", true);
                        ret.put("msg", json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    lanYaContext.success(ret, true);
                    if (mFindBlueToothReceiver != null) {
                        getContext().unregisterReceiver(mFindBlueToothReceiver);
                    }
                }

            }
        }
    };
}
