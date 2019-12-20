package com.jimmy.printer.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.jimmy.printer.common.PrinterFinderCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UsbPrinterFinder {
    private static final String TAG = "UsbPrinterFinder";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final UsbManager usbManager;
    private final PendingIntent usbPermissionIntent;
    private Context context;
    private PrinterFinderCallback<UsbPrinter> finderCallback;
    private List<UsbPrinter> usbPrinters;

    //查找打印机  获取权限
    public UsbPrinterFinder(Context context, PrinterFinderCallback<UsbPrinter> finderCallback) {
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        this.context = context;
        this.finderCallback = finderCallback;
        this.usbPrinters = new ArrayList<>();

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);//插入
//        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);//拔出
        context.registerReceiver(usbReceiver, usbFilter);

        IntentFilter outUsb = new IntentFilter();//拔出
        outUsb.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbOutReceiver, outUsb);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbPermissionReceiver, filter);

    }

    String action;
    // usb插入 初始化设备广播接收
    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            //开始查找设备
            startFinder();
        }
    };
    //usb 拔出
    private BroadcastReceiver usbOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
            usbPrinters.clear();
            if (finderCallback != null) {
//                finderCallback.onStart();
                finderCallback.onUnFind();//设备拔出信号
            }
            //重新查找usb
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            Set<Map.Entry<String, UsbDevice>> entries = deviceList.entrySet();
            if (entries.size() > 0) {//还有设备
                for (Map.Entry<String, UsbDevice> entry : entries) {
                    UsbDevice device = entry.getValue();
//            for (UsbDevice device : usbManager.getDeviceList().values()) {
                    if (!isUsbPrinter(device)) {
                        continue;
                    }
                    //如果usb设备有权限
                    if (usbManager.hasPermission(device)) {
                        UsbPrinter usbPrinter = getUsbPrinter(device);
                        if (usbPrinter != null) {
                            usbPrinters.add(usbPrinter);
                            if (finderCallback != null) {
                                //查找到设备
                                finderCallback.onFound(usbPrinter);
                            }
                            Log.d(TAG, "hasPermission add " + usbPrinter.getPrinterName());
                        } else {
                            if (finderCallback != null) {
                                finderCallback.onUnFind();
                            }//add 设备已拔出
                        }
                    } else {
                        //申请权限
                        usbManager.requestPermission(device, usbPermissionIntent);
                        Log.d(TAG, "requestPermission " + device.getDeviceName());
                    }
                }
                if (finderCallback != null) {
                    //给出查找结果
                    finderCallback.onFinished(usbPrinters);//开始查找结束
                    Log.d(TAG, "for startFinder finished");
                }
            }
        }
//        }
    };

    //查找usb广播接收
    private BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean hasPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (hasPermission && usbDevice != null) {
                    UsbPrinter usbPrinter = getUsbPrinter(usbDevice);
                    if (usbPrinter != null) {
                        usbPrinters.add(usbPrinter);
                        if (finderCallback != null) {
                            finderCallback.onFound(usbPrinter);//找到USB设备回调
                            finderCallback.onFinished(usbPrinters);//完成查找
                        }
                        Log.d(TAG, "usbPermissionReceiver add " + usbPrinter.getPrinterName());
                    } else {
                        if (finderCallback != null) {
                            finderCallback.onUnFind();
                        }//add 未找到设备
                    }
                } else {
                    if (finderCallback != null) {
                        finderCallback.onUnFind();
                    }//add 未给权限
                }
            }
        }
    };

    //开始查找 TODO 查找不到和拔出的情况
    public void startFinder() {
        usbPrinters.clear();
        if (finderCallback != null) {
            finderCallback.onStart();
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Set<Map.Entry<String, UsbDevice>> entries = deviceList.entrySet();
        for (Map.Entry<String, UsbDevice> entry : entries) {
            UsbDevice usbDevice = entry.getValue();
            if (!isUsbPrinter(usbDevice)) {
                continue;
            }
            //如果usb设备有权限
            if (usbManager.hasPermission(usbDevice)) {
                UsbPrinter usbPrinter = getUsbPrinter(usbDevice);
                if (usbPrinter != null) {
                    usbPrinters.add(usbPrinter);
                    if (finderCallback != null) {
                        //查找到设备
                        finderCallback.onFound(usbPrinter);
                    }
                    Log.d(TAG, "hasPermission add " + usbPrinter.getPrinterName());
                } else {
                    if (finderCallback != null) {
                        finderCallback.onUnFind();
                    }//add 设备已拔出
                }
            } else {
                //申请权限
                usbManager.requestPermission(usbDevice, usbPermissionIntent);
                Log.d(TAG, "requestPermission " + usbDevice.getDeviceName());
            }
        }

        if (finderCallback != null) {
            //给出查找结果
            finderCallback.onFinished(usbPrinters);//开始查找结束完成
            Log.d(TAG, "for startFinder finished");
        }
    }

    //判断有无USB设备
    private boolean isUsbPrinter(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return false;
        }

        UsbInterface usbInterface = null;
        for (int intf = 0; intf < usbDevice.getInterfaceCount(); intf++) {
            if (usbDevice.getInterface(intf).getInterfaceClass() == 7) {
                usbInterface = usbDevice.getInterface(intf);
                break;
            }
        }
        return usbInterface != null;
    }

    //获取usb
    private UsbPrinter getUsbPrinter(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return null;
        }

        UsbInterface usbInterface = null;
        UsbEndpoint usbIn = null;
        UsbEndpoint usbOut = null;
        UsbDeviceConnection usbDeviceConnection = null;

        for (int intf = 0; intf < usbDevice.getInterfaceCount(); intf++) {
            if (usbDevice.getInterface(intf).getInterfaceClass() == 7) {
                usbInterface = usbDevice.getInterface(intf);
                break;
            }
        }

        if (usbInterface != null) {
            for (int ep = 0; ep < usbInterface.getEndpointCount(); ep++) {
                int dir = usbInterface.getEndpoint(ep).getDirection();
                if (usbInterface.getEndpoint(ep).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && dir == UsbConstants.USB_DIR_OUT) {
                    usbOut = usbInterface.getEndpoint(ep);
                } else if (usbInterface.getEndpoint(ep).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && dir == UsbConstants.USB_DIR_IN) {
                    usbIn = usbInterface.getEndpoint(ep);
                }
            }
        }

        if (usbIn != null && usbOut != null) {
            usbDeviceConnection = usbManager.openDevice(usbDevice);
        }

        if (usbDeviceConnection != null) {
            usbDeviceConnection.close();
            return new UsbPrinter(usbDevice, usbInterface, usbIn, usbOut, usbDeviceConnection);
        }

        return null;
    }

    public void unregisterReceiver() {
        Log.d(TAG, "unregisterReceiver");
        if (context != null) {
            context.unregisterReceiver(usbReceiver);
            context.unregisterReceiver(usbPermissionReceiver);
//            context.unregisterReceiver(usbOutReceiver);
        }
    }
}
