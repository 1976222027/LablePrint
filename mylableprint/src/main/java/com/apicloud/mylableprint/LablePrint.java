package com.apicloud.mylableprint;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.jimmy.gprint.EscCommand;
import com.jimmy.gprint.LabelCommand;
import com.jimmy.gprint.GpUtils;
//import com.gprinter.command.EscCommand;
//import com.gprinter.command.LabelCommand;
import com.jimmy.printer.bluetooth.BluetoothPrint;
import com.jimmy.printer.bluetooth.BluetoothPrinter;
import com.jimmy.printer.common.PrinterFinderCallback;
import com.jimmy.printer.common.SendCallback;
import com.jimmy.printer.common.SendResultCode;
import com.jimmy.printer.usb.UsbPrint;
import com.jimmy.printer.usb.UsbPrinter;
import com.jimmy.printer.usb.UsbPrinterFinder;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class LablePrint extends UZModule {

    private BluetoothPrint bluetoothPrint;//打印工具
    private BluetoothPrinter bluetoothDev = null;
    private BluetoothDevice bluetoothDevice;
    private List<BluetoothPrinter> printers;
    private BluetoothAdapter bluetoothAdapter;
    private static Activity mContext;

    private UsbPrint usbPrint;//usb打印工具
    private List<UsbPrinter> mUsblist;//设备列表
    //    private UsbPrinter usbPrinter = null;
    private UsbPrinterFinder printerFinder;//查找打印机
    boolean initB = false;


    public LablePrint(UZWebView webView) {
        super(webView);
        mContext = getContext();
        Log.e("mhy","上下文");
    }

    public static Context getContexts() {
        return mContext;
    }

    //usb
    private void initData() {
        if (mUsblist == null) {
            mUsblist = new ArrayList<>();
        }
        //初始化 USB打印工具
        if (usbPrint == null) {
            usbPrint = UsbPrint.getInstance(mContext, sendCallback);
        }

    }
//初始化蓝牙
    private void initBluetoothData() {
        if (printers == null) {
            printers = new ArrayList<>();
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(mContext, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BS);
            } else {
                //蓝牙打开可用 直接去连接
                mBluetooth();
            }
        }
        //初始化 打印工具
        if (bluetoothPrint == null) {
            bluetoothPrint = BluetoothPrint.getInstance(sendCallback);
        }
    }

    //回调
    private SendCallback sendCallback = new SendCallback() {
        @Override
        public void onCallback(int code, String printId) {
            String msg = "";
            if (code == SendResultCode.SEND_SUCCESS) {
                msg = "发送成功";
            } else if (code == SendResultCode.SEND_FAILED) {
                msg = "发送失败";
                Toast.makeText(mContext, "请检查打印机"+printId+"是否连接", Toast.LENGTH_SHORT).show();
            }
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", msg.equals("发送成功"));
                ret.put("msg", printId + "" + msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            backContext.success(ret, true);
        }
    };
    //查找Usb设备回调
    private PrinterFinderCallback<UsbPrinter> printerFinderCallback = new PrinterFinderCallback<UsbPrinter>() {

        @Override
        public void onStart() {
            Log.d("TAG", "startFind开始查找打印机 print");
        }

        @Override
        public void onFound(UsbPrinter usbPrinter) {
            Log.d("TAG", "onFound 设备名deviceName = " + usbPrinter.getPrinterName());
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", "找到设备" + usbPrinter.getPrinterName() +
                        "\nPID = " + usbPrinter.getUsbDevice().getProductId() +
                        "\nVID = " + usbPrinter.getUsbDevice().getVendorId());

            } catch (JSONException e) {
                e.printStackTrace();
            }
            backContext.success(ret, true);
            //获取打印机
            //listAdapter.addData(usbPrinter);
//            mUsblist.add(usbPrinter);
        }

        @Override
        public void onFinished(List<UsbPrinter> usbPrinters) {
            Log.d("TAG", "打印机数：printCount = " + usbPrinters.size());
            //获取打印机
            mUsblist.clear();//add 每次拔插更新
            mUsblist.addAll(usbPrinters);
            if (mUsblist.size()<=0){
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未找到设备" );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                backContext.success(ret, true);
            }
        }

        @Override
        public void onUnFind() {
//            if (mUsblist.size() <= 0) {
            Toast.makeText(mContext, "USB设备失联", Toast.LENGTH_SHORT).show();
//            JSONObject ret = new JSONObject();
//            try {
//                ret.put("status", false);
//                ret.put("msg", "USB设备失联");
//                backContext.success(ret, true);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        }
    };

    /**
     * 页面关闭时调用
     */
    public void jsmethod_closeBr() {
        closeBr();
    }

    /**
     * 获取通过Usb外接打印机的设备信息
     *
     * @param moduleContext
     */
    public void jsmethod_printerUsb(final UZModuleContext moduleContext) {
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        JSONObject json = new JSONObject();
        int i = 0;
        while (deviceIterator.hasNext()) {
            UsbDevice usbDevice = deviceIterator.next();
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
        //请求权限只有usb需要
        backContext = moduleContext;
        initData();
        if (printerFinder == null) {
            printerFinder = new UsbPrinterFinder(mContext, printerFinderCallback);
        }
        printerFinder.startFinder();
//        UsbPrinter.requestUsbPrinter(mContext, moduleContext);

    }

    /**
     * 标签打印机初始化
     *
     * @param moduleContext
     */
    UZModuleContext backContext;
    UZModuleContext initContext;
    boolean isUsb = false;
    boolean isBluetooth = false;
    String macAddress;
    int size;

    public void jsmethod_initPrint(final UZModuleContext moduleContext) {

        String type = moduleContext.optString("type", "usb");
        macAddress = moduleContext.optString("address");
        size = moduleContext.optInt("size", 0);

        // * 初始化*/
//        type = moduleContext.optString("type");
        initContext = moduleContext;
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(mContext, "请输入打印机类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("usb".equals(type)) {
            initData();
            initB = true;
            isUsb = true;
        } else if ("bluetooth".equals(type)) {
            initBluetoothData();
            initB = true;
            isBluetooth = true;
//
        }
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            ret.put("msg", "初始化完成");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        initContext.success(ret, true);
        // bluetoothPrint.blueState(mContext);//add 注册蓝牙开关监听
    }

    /**
     * 根据Mac地址连接蓝牙
     */
    public void mBluetooth() {
        if (macAddress.isEmpty()) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入蓝牙的Mac地址");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            initContext.success(ret, false);
            return;
        }
        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            Toast.makeText(mContext, "蓝牙地址不合法", Toast.LENGTH_SHORT).show();
            return;//不往下走 dev就是空
        }
//根据Mac地址连接蓝牙
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        if(bluetoothDevice==null){
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "蓝牙连接失败");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            initContext.success(ret, false);
        }else {
                int bondState = bluetoothDevice.getBondState();//获得该设备的绑定状态；
                if (bondState == BluetoothDevice.BOND_NONE) {
                    //未配对
                    try {
                        //配对
                        Toast.makeText(mContext, "蓝牙未配对", Toast.LENGTH_SHORT).show();
                        Method creMethod = BluetoothDevice.class.getMethod("createBond");
                        creMethod.invoke(bluetoothDevice);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
            }/*else if (bondState == BluetoothDevice.BOND_BONDED) {
                    *//**
                     * TSC查询打印机状态指令
                     *//*
                    byte[] bytes = { 0x1b, '!', '?' };
                //已配对 直接打印
                bluetoothPrint.sendPrintCommand(bluetoothDevice, bytes);
            }*/
        bluetoothDev = new BluetoothPrinter(bluetoothDevice);
        }

//扫描后选取一个蓝牙对象 打印
//                    bluetoothDev = new BluetoothPrinter(bluetoothDevice);
//                    printers.add(bluetoothDev);
////                    获取对象后打印即可
//                    BluetoothDevice device =/*printers.get(0).getDevice()*/ item.getDevice();
//                    int bondState = device.getBondState();
//                    if (bondState == BluetoothDevice.BOND_BONDED) {
////                        showTestPrintDialog(device);打印
//                        bluetoothPrint.sendPrintCommand(device, bytes);
//                    } else if (bondState == BluetoothDevice.BOND_NONE) {
//                        try {
//                            Method creMethod = BluetoothDevice.class.getMethod("createBond");
//                            creMethod.invoke(device);
//                        } catch (NoSuchMethodException e) {
//                            e.printStackTrace();
//                        } catch (IllegalAccessException e) {
//                            e.printStackTrace();
//                        } catch (InvocationTargetException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    //Log.d(TAG, "device bondState=" + bondState);

    }

    /**
     * 断开蓝牙连接
     *
     * @param moduleContext
     */
    public void jsmethod_disconnect(UZModuleContext moduleContext) {
        bluetoothDevice = null;
        if (bluetoothAdapter != null&&bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            initB = false;
            isBluetooth = false;
            ret.put("msg", "断开成功");
            moduleContext.success(ret, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //退纸
    public void jsmethod_backPaper(UZModuleContext moduleContext) {
        LabelCommand tsc = new LabelCommand();
        if (size>=10){
        tsc.addBackFeed(304);}//dot
        else{
            tsc.addBackFeed(240);//dot
        }
//        tsc.addHome();
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);

//下面进行打印即可
        if (isUsb) {
            if (mUsblist.size()>0){
                for (UsbPrinter usbPrinter : mUsblist) {
                    usbPrint.sendPrintCommand(usbPrinter, bytes);
                }}else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("err", "打印机未连接");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(ret, true);
            }
        } else if (isBluetooth) {

            BluetoothDevice device = bluetoothDevice /*bluetoothDev.getDevice()*/;
            if (device != null) {
                int bondState = device.getBondState();
                int state= bluetoothAdapter.getState(); //本地蓝牙适配器状态
                if (state== BluetoothAdapter.STATE_ON){
                    Log.e("mhy状态",state+"开");

                    if (bondState == BluetoothDevice.BOND_BONDED) {
//                    bluetoothPrint.getBlueStates(mContext);//add
                        //已配对 直接打印
                        bluetoothPrint.sendPrintCommand(device, bytes);
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        JSONObject ret = new JSONObject();
                        try {
                            ret.put("status", false);
                            ret.put("err", "蓝牙未配对");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        moduleContext.success(ret, true);
                        //未配对
                        try {
                            //去配对
                            Toast.makeText(mContext, "蓝牙未配对", Toast.LENGTH_SHORT).show();
                            Method creMethod = BluetoothDevice.class.getMethod("createBond");
                            creMethod.invoke(device);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (state==BluetoothAdapter.STATE_OFF){
                    Toast.makeText(mContext, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("err", "蓝牙未开启");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    moduleContext.success(ret, true);
                }
            }}
    }
    /**
     * 打印小票
     *
     * @param moduleContext
     */
//    UZModuleContext backContext;//打印数据
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void jsmethod_printBill(UZModuleContext moduleContext) {
        backContext = moduleContext;
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
        final JSONArray dataArr = backContext.optJSONArray("data");
        if ((dataArr == null) || (dataArr.length() == 0)) {
            try {
                JSONObject ret = new JSONObject();
                ret.put("status", false);
                ret.put("msg", "打印小票数据不能为空");
                backContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        byte[]bytes= sendReceipt(dataArr);
        if (isUsb) {
            for (UsbPrinter usbPrinter : mUsblist) {
                usbPrint.sendPrintCommand(usbPrinter, bytes);
            }
        } else if (isBluetooth) {
            BluetoothDevice device = bluetoothDevice /*bluetoothDev.getDevice()*/;
            if (device != null) {
                int bondState = device.getBondState();
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    //已配对 直接
                    bluetoothPrint.sendPrintCommand(device, bytes);
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    try {
                        //配对
                        Method creMethod = BluetoothDevice.class.getMethod("createBond");
                        creMethod.invoke(device);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(mContext, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //打印小票数据
    public byte[] sendReceipt(JSONArray dataArr) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject itemDataObj = dataArr.optJSONObject(i);
            String rowtype = itemDataObj.optString("rowtype");//行类型
            if (!TextUtils.isEmpty(rowtype)) {
                if ("printQRCode".equals(rowtype)) {//二维码
                    String datas = itemDataObj.optString("data");//二维码内容
                    String alignment = itemDataObj.optString("alignment", "left");
                    // 取消倍高倍宽
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    if (!TextUtils.isEmpty(datas)) {
                            // 设置纠错等级
                            esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                            // 设置qrcode模块大小
                            esc.addSelectSizeOfModuleForQRCode((byte) 6);
                            // 设置qrcode内容
                            esc.addStoreQRCodeData(datas);
                            esc.addPrintQRCode();// 打印QRCode
                    }
                    //        returnBitMap("http://a1000.top/a/logo.png",esc);
                } else if ("printTitle".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                } else if ("printColumnsText".equals(rowtype)) {
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    JSONArray colsTextArr = itemDataObj.optJSONArray("colsTextArr");
                    JSONArray colsWidthArr = itemDataObj.optJSONArray("colsWidthArr");
                    JSONArray colsAlign = itemDataObj.optJSONArray("colsAlign");
                    String[] text = new String[colsTextArr.length()];
                    for (int j = 0; j < colsTextArr.length(); j++) {
                        text[j] = colsTextArr.optString(j);
                    }
                    int[] width = new int[colsWidthArr.length()];
                    for (int j = 0; j < colsWidthArr.length(); j++) {
                        width[j] = colsWidthArr.optInt(j);
                    }
                    int[] align = new int[colsAlign.length()];
                    for (int j = 0; j < colsAlign.length(); j++) {
                        align[j] = colsAlign.optInt(j);
                    }
                    String str = "";
                    for (int k = 0; k < text.length; k++) {
                        String s = text[k];
                        int x = getlength(text[k]);
                        int y = width[k];
                        int b = y - x;
                        if (align[k] == 0) {
                            for (int a = 0; a < b; a++) {
                                s = s + " ";
                            }
                        } else if (align[k] == 1) {
                            if (b % 2 == 0) {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                            } else {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                                s = " " + s;
                            }
                        } else if (align[k] == 2) {
                            for (int a = 0; a < b; a++) {
                                s = " " + s;
                            }
                        }
                        str += s;
                    }
                    str += "\n";
                    esc.addText(str);
                } else if ("printText".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                }

            }

        }
        esc.addPrintAndFeedLines((byte) 3);
        esc.addCutPaper();
//        // 加入查询打印机状态，用于连续打印
//        byte[] bytes = {29, 114, 1};
//        esc.addUserCommand(bytes);
        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
//        if (isInstruction == 2) {
//            String str = Base64.encodeToString(bytes, Base64.DEFAULT);
//            bytes = Base64.decode(str, Base64.DEFAULT);
//        }
        //发送数据给打印机
        return bytes;
    }
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
        return c / k == 0;
    }
    /**
     * 打印标签
     *
     * @param moduleContext
     */
  //  int x = 0;// 打印纸一行最大的字节

    public void jsmethod_printerLabel(final UZModuleContext moduleContext) {
        backContext=moduleContext;
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
        //最终要打印的数据
        byte[] bytes = PrintMould.printLable(jsonArray, size);/*Vector<Byte> datas = tsc.getCommand(); // 发送数据GpUtils.ByteTo_byte(datas)*/

//下面进行打印即可
        if (isUsb) {
            if (mUsblist.size()>0){
            for (UsbPrinter usbPrinter : mUsblist) {
                usbPrint.sendPrintCommand(usbPrinter, bytes);
            }}else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("err", "打印机未连接");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(ret, true);
            }
        } else if (isBluetooth) {

            BluetoothDevice device = bluetoothDevice /*bluetoothDev.getDevice()*/;
            if (device != null) {
                int bondState = device.getBondState();
            int state= bluetoothAdapter.getState(); //本地蓝牙适配器状态
            if (state== BluetoothAdapter.STATE_ON){
                Log.e("mhy状态",state+"开");

                if (bondState == BluetoothDevice.BOND_BONDED) {
//                    bluetoothPrint.getBlueStates(mContext);//add
                    //已配对 直接打印
                    bluetoothPrint.sendPrintCommand(device, bytes);
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("err", "蓝牙未配对");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    moduleContext.success(ret, true);
                    //未配对
                    try {
                        //去配对
                        Toast.makeText(mContext, "蓝牙未配对", Toast.LENGTH_SHORT).show();
                        Method creMethod = BluetoothDevice.class.getMethod("createBond");
                        creMethod.invoke(device);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else if (state==BluetoothAdapter.STATE_OFF){
                Toast.makeText(mContext, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("err", "蓝牙未开启");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(ret, true);
            }
        }}
}

    UZModuleContext lanYaContext;
    int i;
    JSONObject json;
    /**
     * 扫描蓝牙设备
     */
    private ArrayAdapter<String> DevicesArrayAdapter;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_ENABLE_BS = 3;

    public void jsmethod_ScanBluetooth(UZModuleContext moduleContext) {
        lanYaContext = moduleContext;
        if (printers == null) {
            printers = new ArrayList<>();
        }
        i = 0;//初始
        json = new JSONObject();
        initBluetooth();//已配对的
    }

    //获取位
    private void requestPermission() {
        int local = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (local != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void discoveryDevice() {
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();//发现扫描设备 系统挥发广播 去接收即可
    }
//已配对设备
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            //打开蓝牙
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            getDeviceList();
        }
    }

    private void closeBr() {
        mContext.unregisterReceiver(mFindBlueToothReceiver);
        if (printerFinder != null) {
            printerFinder.unregisterReceiver();
        }
        if (bluetoothAdapter != null&&bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                getDeviceList();
            } else {
                // bluetooth is not open
                Toast.makeText(mContext, "设备未开启", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BS) {
            if (resultCode == Activity.RESULT_OK) {
                mBluetooth();
            } else {
                // bluetooth is not open
                Toast.makeText(mContext, "蓝牙未开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //查看已配对设备
    private void getDeviceList() {
        DevicesArrayAdapter = new ArrayAdapter<>(mContext, R.layout.bluetooth_device_name_item);
        // 获取当前已配对的设备
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();//获取已绑定的设备列表
        // 添加已配对设备到数组
        DevicesArrayAdapter.add(mContext.getString(R.string.str_title_pairedev));//已配对
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                DevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                try {
                    json.put("" + i, device.getName() + "----" + device.getAddress());
                    i++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (json.length()>0){
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            lanYaContext.success(ret, true);}
        } else {
            String noDevices = mContext.getResources().getText(R.string.none_paired).toString();
            DevicesArrayAdapter.add(noDevices);
//          没有配对的设备再去扫描
            //扫描未配对设备
            discoveryDevice();//扫描未配对设备
            requestPermission();
            //广播接收
            IntentFilter bluetoothFilter = new IntentFilter();
            bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);
            // 注册广播 当 发现和完成
            bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mFindBlueToothReceiver, bluetoothFilter);//扫描蓝牙广播接收

        }
    }

    //扫描蓝牙结果广播
    private final BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 当发现一个设备完成
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 获取蓝牙对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    DevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    try {
                        json.put("" + i, device.getName() + "----" + device.getAddress());
                        i++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.e("mhy", device.getName() + "=-----------" + device.getAddress());
                }
                // 扫描完成
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (DevicesArrayAdapter.getCount() == 0) {
                    String noDevices =mContext.getResources().getText(R.string.none_bluetooth_device_found).toString();
                    DevicesArrayAdapter.add(noDevices);
                } else {
                    if (json.length()>0){
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                lanYaContext.success(ret, true);
            }
                }
            if (mFindBlueToothReceiver != null) {
                    mContext.unregisterReceiver(mFindBlueToothReceiver);
                }
            }
        }
    };
}
