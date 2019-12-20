package com.jimmy.printer.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jimmy.printer.common.SendCallback;
import com.jimmy.printer.common.SendResultCode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbPrint {
    private static UsbPrint INSTANCE;
    private final UsbManager usbManager;
    private SendCallback sendCallback;
    private final ExecutorService threadPool;
    private MyHandler myHandler;
    private Context mContext;
    UsbDeviceConnection mConnection;
    byte[] buffer = new byte[100];
    UsbInterface mInterface;
    UsbEndpoint mInEndpoint;
    UsbEndpoint mEndpoint;
    UsbDevice mDevice;
    // ESC查询打印机实时状态指令
//    private byte[] esc = {0x10, 0x04, 0x02};
//    // TSC查询打印机状态指令
//    private byte[] tsc = {0x1b, '!', '?'};
//    private byte[] cpcl = {0x1b, 0x68};
//    private byte[] sendCommand;
//    private int currentPrinterCommand = -1, ESC = 0, TSC = 1, CPCL = 2;

    private UsbPrint(Context context, SendCallback sendCallback) {
        this.mContext = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.sendCallback = sendCallback;
        this.threadPool = Executors.newFixedThreadPool(3);
        this.myHandler = new MyHandler(this);
    }

    public static UsbPrint getInstance(Context context, SendCallback sendCallback) {
        if (INSTANCE == null) {
            synchronized (UsbPrint.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UsbPrint(context, sendCallback);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * add
     * 发送打印命令
     *
     * @param printer
     * @param bytes
     */
//    public void sendPrintCmd(UsbPrinter printer, byte[] bytes) {
//        if (printer == null || printer.getUsbDevice() == null) {
//            return;
//        }
//        //标签打印
//        LabelCommand tsc = new LabelCommand();
//        tsc.addSize(80, 50); // 设置标签尺寸，按照实际尺寸设置
//        tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
//        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
//        tsc.addReference(0, 0);// 设置原点坐标
//        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
//
//        tsc.addCls();// 清除打印缓冲区
//// 绘制简体中文
//        tsc.addText(20, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "     天猫超市");
//        tsc.addText(20, 50, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "商品：农夫山泉");
//        tsc.addText(20, 80, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "规格：箱");
//        tsc.addText(20, 110, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "金额：￥16.00");
//        tsc.addText(20, 140, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "时间：2017/05/19 15:00");
//        tsc.addText(20, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                "电话：18818181818");
//
//// 绘制图片
//// Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.product_000001);
//// tsc.addBitmap(20, 50, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);
//
//// 绘制二维码
//// tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.gg.com.cn");
//
//// 绘制条形码
//// tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 50, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "printer");
//
//        tsc.addPrint(1, 1); // 打印标签
//        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
//
//// tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
//        Vector<Byte> datas = tsc.getCommand(); // 发送数据
//        bytes = LabelUtils.ByteTo_byte(datas);
//
//        SendCommandThread thread = new SendCommandThread(usbManager, printer, bytes);
//        threadPool.execute(thread);
//    }

    public void sendPrintCommand(UsbPrinter printer, byte[] bytes) {
        if (printer == null || printer.getUsbDevice() == null) {
            return;
        }
//        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
//        byte[] decode_datas = Base64.decode(str, Base64.DEFAULT);

        SendCommandThread thread = new SendCommandThread(usbManager, printer, bytes);
        threadPool.execute(thread);

    }

    public void readData(UsbPrinter usbPrinter, byte[] bytes) throws IOException {
        mConnection = usbManager.openDevice(usbPrinter.getUsbDevice());
        int result = -1;
        int resultCode = SendResultCode.SEND_FAILED;
        if (mConnection != null && mConnection.claimInterface(usbPrinter.getUsbInterface(), true)) {
            result = mConnection.bulkTransfer(usbPrinter.getUsbOut(), bytes, bytes.length, 2000);
            mConnection.close(); //最后关闭
            //result 只是数据长度
            resultCode = result > 0 ? SendResultCode.SEND_SUCCESS : SendResultCode.SEND_FAILED;
        }
        sendMessage(resultCode, usbPrinter.getPrinterName());
    }

    private class SendCommandThread extends Thread {
        private UsbManager usbManager;
        private UsbPrinter usbPrinter;
        private byte[] bytes;

        public SendCommandThread(UsbManager usbManager, UsbPrinter usbPrinter, byte[] bytes) {
            this.usbManager = usbManager;
            this.usbPrinter = usbPrinter;
            this.bytes = bytes;
        }

        @Override
        public void run() {
            super.run();
            mDevice = usbPrinter.getUsbDevice();
            mInterface = usbPrinter.getUsbInterface();
            mEndpoint = usbPrinter.getUsbOut();
            /*UsbDeviceConnection */
            mConnection = usbManager.openDevice(usbPrinter.getUsbDevice());

            if (mConnection != null && mConnection.claimInterface(usbPrinter.getUsbInterface(), true)) {
                int result = mConnection.bulkTransfer(usbPrinter.getUsbOut(), bytes, bytes.length, 2200);
                mConnection.close();
                if (result != bytes.length) {
                    Log.e("mhy", "异常"+result);
                }
                //result 只是数据长度
                int resultCode = result > 0 ? SendResultCode.SEND_SUCCESS : SendResultCode.SEND_FAILED;
                sendMessage(resultCode, usbPrinter.getPrinterName());

            }
        }
    }

    private void sendMessage(int resultCode, String printerId) {
        /*******************************************************************/
        //打印返回数据 多返回一个状态码可根据打印机指令对比 打印机处于什么状态 比如缺纸 开盖
        if (resultCode == SendResultCode.SEND_SUCCESS) {
            //读取打印机返回信息
            //如果返回数据则通了
//            if (sendCommand == esc) {
//                //设置当前打印机模式为ESC模式
//                currentPrinterCommand = ESC;
//            } else if (sendCommand == tsc) {
//                //设置当前打印机模式为TSC模式
//                currentPrinterCommand = TSC;
//            } else if (sendCommand == cpcl) {
//                currentPrinterCommand = CPCL;
//            }
        }
        /*****************************************************************/
        Message msg = new Message();
        msg.obj = printerId;
        msg.what = resultCode;//0成功 1 失败
        myHandler.sendMessage(msg);
    }

    //   mHandler.removeCallbacksAndMessages(null);
    private class MyHandler extends Handler {
        private WeakReference<UsbPrint> weakReference;

        MyHandler(UsbPrint usbPrint) {
            this.weakReference = new WeakReference<>(usbPrint);
        }

        @Override
        public void handleMessage(Message msg) {
            UsbPrint usbPrint = weakReference.get();
            int resultCode = msg.what;//返回码
            String printName = (String) msg.obj;

            if (usbPrint != null && usbPrint.sendCallback != null) {
                usbPrint.sendCallback.onCallback(resultCode, printName);
            }

        }
    }

/***********************************************************/

    /**
     * 打开钱箱
     */
    public void openCashBox(UsbPrinter usbPrinter) {
        byte[] command = new byte[]{27, 112, (byte) 1, (byte) 255, (byte) 255};
        sendPrintCommand(usbPrinter, command);
    }


}
