package com.apicloud.myusbprint;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.apicloud.mylableprint.R;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.jimmy.gprint.EscCommand;
import com.jimmy.gprint.GpUtils;
import com.jimmy.gprint.LabelCommand;
import com.jimmy.printer.common.PrinterFinderCallback;
import com.jimmy.printer.common.SendCallback;
import com.jimmy.printer.common.SendResultCode;
import com.jimmy.printer.ethernet.EthernetPrint;
import com.jimmy.printer.usb.UsbPrinter;
import com.jimmy.printer.usb.UsbPrinterFinder;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * USB打印模块
 */

public class UsbPrint extends UZModule {
    private com.jimmy.printer.usb.UsbPrint usbPrint;//usb打印工具
    private EthernetPrint ethernetPrint;//WiFi打印机
    private UsbPrinterFinder printerFinder;//查找打印机
    boolean stasteUsb = false;//打印机是否就绪
    private static final String TAG = "UsbPrint";
    List<UsbPrinter> mUsblist;//设备列表
    List<String> mUsbDevice;//设备列表
    private static Activity mContext;

    public static Context getContexts() {
        return mContext;

    }

    public UsbPrint(final UZWebView webView) {
        super(webView);
        mContext = getContext();
        Log.e("mhy","上下文");
    }

    //usb
    private void initData() {
        if (mUsblist == null) {
            mUsblist = new ArrayList<>();
        }
        //初始化 USB打印工具
        if (usbPrint == null) {
            usbPrint = com.jimmy.printer.usb.UsbPrint.getInstance(mContext, sendCallback);
        }
//        printerFinder = new UsbPrinterFinder(mContext, printerFinderCallback);
//        printerFinder.startFinder();
    }

    //wifi
    public void initWifi() {
        if (mUsblist == null) {
            mUsblist = new ArrayList<>();
        }
        //初始化 WiFi 打印
        if (ethernetPrint == null) {
            ethernetPrint = EthernetPrint.getInstance(sendCallback);
            requestPermission();
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext,
                    new String[]{Manifest.permission.INTERNET},
                    1);
        }
    }

    public boolean isIP(String address) {
        if (address.length() < 7 || address.length() > 15 || "".equals(address)) {
            return false;
        }
        // \\d 代表0-9
        String regex="^(127\\.0\\.0\\.1)|(localhost)|(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(172\\.((1[6-9])|(2\\d)|(3[01]))\\.\\d{1,3}\\.\\d{1,3})|(192\\.168\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]/d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]/d|25[0-5]))$";
//      String regex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(address);
        return mat.find();
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
                backContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            Toast.makeText(mContext, printId + " " + msg, Toast.LENGTH_LONG).show();
        }
    };
    //查找设备回调
    private PrinterFinderCallback<UsbPrinter> printerFinderCallback = new PrinterFinderCallback<UsbPrinter>() {

        @Override
        public void onStart() {
            Log.d(TAG, "startFind开始查找打印机 print");
        }

        @Override
        public void onFound(com.jimmy.printer.usb.UsbPrinter usbPrinter) {
            //listAdapter.addData(usbPrinter);
            Log.d(TAG, "onFound 设备名deviceName = " + usbPrinter.getPrinterName());
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", "找到设备" + usbPrinter.getPrinterName() +
                        "\nPID = " + usbPrinter.getUsbDevice().getProductId() +
                        "\nVID = " + usbPrinter.getUsbDevice().getVendorId());
                backContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //获取打印机
//            mUsblist.add(usbPrinter);
        }

        @Override
        public void onFinished(List<UsbPrinter> usbPrinters) {
            Log.d(TAG, "打印机数：printCount = " + usbPrinters.size());

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
        if (printerFinder != null) {
            printerFinder.unregisterReceiver();
        }
    }

    //usb打印数据  item==null wifi打印数据
    public void sendReceipt(UZModuleContext backContext, boolean isbox, int isInstruction, UsbPrinter item) {
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
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
//        esc.addPrintAndFeedLines((byte) 3);
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject itemDataObj = dataArr.optJSONObject(i);
            String rowtype = itemDataObj.optString("rowtype");//行类型
            if (!TextUtils.isEmpty(rowtype)) {
                switch (rowtype) {
                    case "printQRCode":
                        String datas = itemDataObj.optString("data");//二维码内容
                        String alignment = itemDataObj.optString("alignment", "left");
                        // 取消倍高倍宽
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                        esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                        esc.addPrintAndLineFeed();
//                        // 发送打印图片前导指令
//                        byte[] start = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B, 0x40, 0x1B, 0x33, 0x00 };
//                        esc.addByteCommand(start);
                        if (!TextUtils.isEmpty(datas)) {
                            if (alignment.equals("right")) {
                                esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                            } else if (alignment.equals("left")) {
                                esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                            } else {
                                esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                            }

                            if (2 == isInstruction) {
                            try {
                                Bitmap bmp = encodeQRCode(datas, ErrorCorrectionLevel.L, 8);
                                esc.addRastBitImage(bmp, 180, 0);
                            } catch (WriterException e) {
                                e.printStackTrace();
                                Log.e("eer", e.getMessage());
                            }
                            } else {
                                // 设置纠错等级
                                esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                                // 设置qrcode模块大小
                                esc.addSelectSizeOfModuleForQRCode((byte) 7);
                                // 设置qrcode内容
                                esc.addStoreQRCodeData(datas);
                                esc.addPrintQRCode();// 打印QRCode
                            }
                            // 发送结束指令
//                            byte[] end = { 0x1d, 0x4c, 0x1f, 0x00 };
//                            esc.addByteCommand(end);
                        }
                        break;
                    case "printTitle":
                        String text = itemDataObj.optString("text");
                        String alignment1 = itemDataObj.optString("alignment", "left");
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                        esc.addSetKanjiFontMode(EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                        if (alignment1.equals("right")) {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                        } else if (alignment1.equals("left")) {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                        } else {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                        }
                        esc.addText(text);
                        break;
                    case "printColumnsText":
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                        esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                        JSONArray colsTextArr = itemDataObj.optJSONArray("colsTextArr");
                        JSONArray colsWidthArr = itemDataObj.optJSONArray("colsWidthArr");
                        JSONArray colsAlign = itemDataObj.optJSONArray("colsAlign");
                        String[] text1 = new String[colsTextArr.length()];
                        for (int j = 0; j < colsTextArr.length(); j++) {
                            text1[j] = colsTextArr.optString(j);
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
                        for (int k = 0; k < text1.length; k++) {
                            String s = text1[k];
                            int x = getlength(text1[k]);
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
                        break;
                    case "printText":
                        String text2 = itemDataObj.optString("text");
                        String alignment2 = itemDataObj.optString("alignment", "left");
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                        esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                        if (alignment2.equals("right")) {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                        } else if (alignment2.equals("left")) {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                        } else {
                            esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                        }
                        esc.addText(text2);
                        break;
                    default:
                        break;
                }

            }

        }
//        esc.addPrintAndFeedLines((byte) 3);
        esc.addCutPaper();
        if (isbox) {
            // 开钱箱
            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//            esc.addPrintAndFeedLines((byte) 8);
        }
//        // 加入查询打印机状态，用于连续打印
//        byte[] bytes = {29, 114, 1};
//        esc.addUserCommand(bytes);
//      发送数据
//        Vector<Byte> datas = esc.getCommand();
//        byte[] bytes = GpUtils.ByteTo_byte(datas);
        byte[] bytes = esc.getByteArrayCommand();
        Log.e("打印", bytes.length+"");
//        if (isInstruction == 2) {
//            String str = Base64.encodeToString(bytes, Base64.DEFAULT);
//            bytes = Base64.decode(str, Base64.DEFAULT);
//        }
        //发送数据给打印机
        if (item == null) {
            ethernetPrint.sendPrintCommand(mPrinterIp, port/*Integer.parseInt(sport)*/, bytes);  //发送WiFi打印机
        } else {
            usbPrint.sendPrintCommand(item, bytes);
        }
    }

    @Override
    protected void onClean() {
        super.onClean();
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

    public void openBox() {
        EscCommand esc = new EscCommand();
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//        esc.addPrintAndFeedLines((byte) 8);
        Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        //发送数据给打印机
        usbPrint.sendPrintCommand(mUsblist.get(0), bytes);
    }

    public void openWBox() {
        EscCommand esc = new EscCommand();
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//        esc.addPrintAndFeedLines((byte) 8);
        Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        //发送数据给打印机
        //发送WiFi打印机
        ethernetPrint.sendPrintCommand(mPrinterIp, port/*Integer.parseInt(sport)*/, bytes);
    }

    /**
     * 打印测试
     */
    public void sendReceipt() {
//        EscCommand esc = new EscCommand();
//        esc.addInitializePrinter();
//        esc.addPrintAndFeedLines((byte) 3);
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
//        esc.addText("Sample\n"); // 打印文字
//        esc.addPrintAndLineFeed();
//
//        /* 打印文字 */
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
//        esc.addText("Print text\n"); // 打印文字
//        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字
//
//        /* 打印繁体中文 需要打印机支持繁体字库 */
//        String message = "佳博智匯票據打印機\n";
//        // esc.addText(message,"BIG5");
//        esc.addText(message, "GB2312");
//        esc.addPrintAndLineFeed();
//
//        /* 绝对位置 具体详细信息请查看GP58编程手册 */
//        esc.addText("智汇");
//        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
//        esc.addSetAbsolutePrintPosition((short) 6);
//        esc.addText("网络");
//        esc.addSetAbsolutePrintPosition((short) 10);
//        esc.addText("设备");
//        esc.addPrintAndLineFeed();
//

//        /* 打印一维条码 */
//        esc.addText("Print code128\n"); // 打印文字
//        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);//
//        // 设置条码可识别字符位置在条码下方
//        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
//        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
//        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
//        esc.addPrintAndLineFeed();
//
//        /*
//         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
//         */
//        esc.addText("Print QRcode\n"); // 打印文字
//        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
//        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
//        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
//        esc.addPrintQRCode();// 打印QRCode
//        esc.addPrintAndLineFeed();
//
//        /* 打印文字 */
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
//        esc.addText("打印结束!\r\n"); // 打印结束
//        esc.addPrintAndFeedLines((byte) 3);
//        esc.addCutPaper();//切纸
//// 开钱箱
//        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//        Vector<Byte> datas = esc.getCommand(); // 发送数据
//        byte[] bytes = GpUtils.ByteTo_byte(datas);
//        for (UsbPrinter usbPrinter : mUsblist) {
//            usbPrint.sendPrintCommand(usbPrinter, bytes);
//        }
    }

    //    同步映射在该类下定义public类型，jsmethod前缀sync后缀的return类型函数，接收UZModuleContext类型参数，并同步返回经ModuleResult包装过的数据。
//    如何一个异步接口给Javascript
//    在该类下定义public类型，jsmethod前缀的void类型函数，并接收UZModuleContext类型参数， 将操作结果通过success 或者error回调。
    public void jsmethod_requestPermission(UZModuleContext moduleContext) {
        //请求权限只有usb需要
        backContext = moduleContext;
        initData();
        if (printerFinder == null) {
            printerFinder = new UsbPrinterFinder(mContext, printerFinderCallback);
        }
        printerFinder.startFinder();
    }


    /**
     * 打印小票
     *
     * @param moduleContext
     */
//    UZModuleContext backContext;//打印数据
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void jsmethod_printData(UZModuleContext moduleContext) {
        backContext = moduleContext;
//        sendReceiptWithResponse();
//        sendReceipt();
        if (type.equals("usb-Xprinter") || type.equals("usb")) {

            if (usbPrint != null) {
                if (printerFinder == null) {
                    // printerFinder = new UsbPrinterFinder(mContext, printerFinderCallback);
                    //  printerFinder.startFinder();return;
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("msg", "未获取权限，未找到usb设备");
                        backContext.success(ret, false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (mUsblist.size() <= 0) {
                        JSONObject ret = new JSONObject();
                        try {
                            ret.put("status", false);
                            ret.put("msg", "USB设备未连接");
                            backContext.success(ret, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //打印
                        for (UsbPrinter usbPrinter : mUsblist) {
                            sendReceipt(moduleContext, isbox, isInstruction, usbPrinter);
                        }

                    }
                }
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未初始化");
                    backContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //WiFi
            boolean isIp = isIP(mPrinterIp);
            if (!isIp) {
                Toast.makeText(mContext, "IP地址格式错误", Toast.LENGTH_LONG).show();
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "IP地址格式错误");
                    backContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
            if (TextUtils.isEmpty(port + "")) {
                Toast.makeText(mContext, "端口不能为空", Toast.LENGTH_LONG).show();
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "端口不能为空");
                    backContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
            //打印wifi
            sendReceipt(moduleContext, isbox, isInstruction, null);
//            testPrint(null);
        }
//        js    method_closeBr();
    }

    private Bitmap encodeQRCode(String text, ErrorCorrectionLevel errorCorrectionLevel, int scale) throws WriterException {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
//        QRCode code = new QRCode();
//        Encoder.encode(text, errorCorrectionLevel, hints, code);
        QRCode code = Encoder.encode(text, errorCorrectionLevel, hints);
        final ByteMatrix m = code.getMatrix();
        final int mw = m.getWidth();
        final int mh = m.getHeight();
        final int IMG_WIDTH = mw * scale;
        final int IMG_HEIGHT = mh * scale;
        Bitmap bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        c.drawColor(Color.WHITE);
        p.setColor(Color.BLACK);
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < mw; x++) {
                if (m.get(x, y) == 1) {
                    c.drawRect(x * scale, y * scale,
                            (x + 1) * scale, (y + 1) * scale, p);
                }
            }
        }
        return bmp;
    }

    /**
     * 开启钱箱
     *
     * @param moduleContext
     */
    public void jsmethod_openCashBox(UZModuleContext moduleContext) {
        backContext = moduleContext;
//        mContext.unregisterReceiver(receiver);
        if (type.equals("usb") || type.equals("usb-Xprinter")) {

            if (usbPrint != null) {
                if (printerFinder == null) {
                    // printerFinder = new UsbPrinterFinder(mContext, printerFinderCallback);
                    //  printerFinder.startFinder();return;
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("msg", "未获取权限，未找到usb设备");
                        backContext.success(ret, true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (mUsblist.size() <= 0) {
                        JSONObject ret = new JSONObject();
                        try {
                            ret.put("status", false);
                            ret.put("msg", "USB设备未连接");
                            backContext.success(ret, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        openBox();
                    }
                }
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未初始化");
                    moduleContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            openWBox();
        }
//        jsmethod_closeBr();

//        openBox();
//        if (mUsblist.size() <= 0) {
//            JSONObject ret = new JSONObject();
//            try {
//                ret.put("status", false);
//                ret.put("msg", "USB设备未连接");
//                backContext.success(ret, false);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else  {
//            openBox();
//        }
    }

    /**
     * 初始化小票打印机
     *
     * @param moduleContext
     */
    UZModuleContext backContext;//打印机配置
    int isInstruction;
    boolean isbox;
    int port;
    String mPrinterIp, type = "usb";//默认
    boolean isXpriner = false;

    public void jsmethod_initPrint(UZModuleContext moduleContext) {
        // * 初始化*/
        if (mUsbDevice == null) {
            mUsbDevice = new ArrayList<>();
        }
        backContext = moduleContext;
        type = moduleContext.optString("type");
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(mContext, "请输入打印机类型", Toast.LENGTH_SHORT).show();
            return;
        }
        isInstruction = moduleContext.optInt("isinstruction", 1);//指令非指令
        isbox = moduleContext.optBoolean("isbox", true);
        mPrinterIp = moduleContext.optString("mPrinterIp");
        port = moduleContext.optInt("port", 9100);

        isXpriner = false;
        if (type.equals("usb-Xprinter")) {
            isXpriner = true;
            initData();
        } else if (type.equals("usb")) {
            initData();
        } else {
            initWifi();
            boolean isIp = isIP(mPrinterIp);
            if (!isIp) {
                Toast.makeText(mContext, "IP地址格式错误", Toast.LENGTH_LONG).show();
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "IP地址格式错误");
                    backContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
            if (TextUtils.isEmpty(port + "")) {
                Toast.makeText(mContext, "端口不能为空", Toast.LENGTH_LONG).show();
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "端口不能为空");
                    backContext.success(ret, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            ret.put("msg", "初始化完成");
            backContext.success(ret, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    UsbManager manager;
    List<UsbDevice> deviceList = new ArrayList<>();
    UsbDevice device;

    public void getUsbDeviceList() {
        manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        // Get the list of attached devices
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        Log.d(TAG, "count " + count);
        if (count > 0) {
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                String devicename = device.getDeviceName();
                Log.e("wang", devicename + "---------");
                if (checkUsbDevicePidVid(device)) {
                    mUsbDevice.add(devicename);
                    deviceList.add(device);
                }
            }
        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "usb未连接打印机");
                backContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
    }

    boolean checkUsbDevicePidVid(UsbDevice dev) {
        int pid = dev.getProductId();
        int vid = dev.getVendorId();
        return ((vid == 34918 && pid == 256) || (vid == 1137 && pid == 85)
                || (vid == 6790 && pid == 30084)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 512)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 768)
                || (vid == 26728 && pid == 1024) || (vid == 26728 && pid == 1280)
                || (vid == 26728 && pid == 1536) || (vid == 1155 && pid == 1803));
    }


    //Handler+Message进行简单传值；
    Bitmap bitmap;
    Handler handler = new MyHandler();
    EscCommand esc;

    public Bitmap returnBitMap(final String url, EscCommand fesc) {
        esc = fesc;
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL myFileUrl = null;
                try {
                    myFileUrl = new URL(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                try {
                    HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    message.setData(bundle);//bundle传值，耗时，效率低
                    handler.sendMessage(message);//发送message信息
                    message.what = 1;//标志是哪个线程传数据

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Log.e("daduhui", "url转bitmap");

        return bitmap;
    }

    ;

    byte[] printData = null;

    //接受message的信息
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if (msg.what == 1) {
//                imageView.setImageBitmap(bitmap);
                try {
                    printData = draw2PxPoint(compressPic(bitmap, 160));
                    esc.addByteCommand(printData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    //对图片进行压缩（去除透明度）：
    public static Bitmap compressPic(Bitmap bitmap, int widthAndHigh) {
        // 获取这个图片的宽和高
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 指定调整后的宽度和高度
        int newWidth = widthAndHigh;
        int newHeight = widthAndHigh;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);
        targetCanvas.drawBitmap(bitmap, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        return targetBmp;
    }

    /**
     * * 灰度图片黑白化，黑色是1，白色是0
     * *
     * * @param x   横坐标
     * * @param y   纵坐标
     * * @param bit 位图
     * * @return
     */
    public static byte px2Byte(int x, int y, Bitmap bit) {
        if (x < bit.getWidth() && y < bit.getHeight()) {
            byte b;
            int pixel = bit.getPixel(x, y);
            int red = (pixel & 0x00ff0000) >> 16; // 取高两位
            int green = (pixel & 0x0000ff00) >> 8; // 取中两位
            int blue = pixel & 0x000000ff; // 取低两位
            int gray = RGB2Gray(red, green, blue);
            if (gray < 128) {
                b = 1;
            } else {
                b = 0;
            }
            return b;
        }
        return 0;
    }

    /**
     * 图片灰度的转化
     */
    private static int RGB2Gray(int r, int g, int b) {
        int gray = (int) (0.29900 * r + 0.58700 * g + 0.11400 * b); //灰度转化公式
        return gray;
    }

    //转换为字节流
    public byte[] draw2PxPoint(Bitmap bmp) throws IOException {

        //用来存储转换后的 bitmap 数据。为什么要再加1000，这是为了应对当图片高度无法
        //整除24时的情况。比如bitmap 分辨率为 240 * 250，占用 7500 byte，
        //但是实际上要存储11行数据，每一行需要 24 * 240 / 8 =720byte 的空间。再加上一些指令存储的开销，
        //所以多申请 1000byte 的空间是稳妥的，不然运行时会抛出数组访问越界的异常。
        int size = bmp.getWidth() * bmp.getHeight() / 8 + 1000;
        byte[] data = new byte[size];
        int k = 0;
        //设置行距为0的指令
        data[k++] = 0x1B;
        data[k++] = 0x33;
        data[k++] = 0x00;
        // 逐行打印
        for (int j = 0; j < bmp.getHeight() / 24f; j++) {
            //打印图片的指令
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33;
            data[k++] = (byte) (bmp.getWidth() % 256); //nL
            data[k++] = (byte) (bmp.getWidth() / 256); //nH
            //对于每一行，逐列打印
            for (int i = 0; i < bmp.getWidth(); i++) {
                //每一列24个像素点，分为3个字节存储
                for (int m = 0; m < 3; m++) {
                    //每个字节表示8个像素点，0表示白色，1表示黑色
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(i, j * 24 + m * 8 + n, bmp);
                        data[k] += data[k] + b;
                    }
                    k++;
                }
            }
            data[k++] = 10;//换行
        }
        return data;
    }

}
