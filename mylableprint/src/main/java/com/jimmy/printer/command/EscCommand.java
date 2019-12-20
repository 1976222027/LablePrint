package com.jimmy.printer.command;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.jimmy.gprint.LabelCommand;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

public class EscCommand {

    private Vector<Byte> command;

    public EscCommand() {
        this.command = new Vector<>(4096, 1024);
    }

    private void addArrayToCommand(byte[] array) {
        for (byte anArray : array) {
            this.command.add(anArray);
        }
    }
    public void addBitMapCommand(Bitmap bitmap, int widthAndHight) {
        try {
            byte[] array= draw2PxPoint(compressPic(bitmap,widthAndHight));
            this.addArrayToCommand(array);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //对图片进行压缩（去除透明度）：
    public static Bitmap compressPic(Bitmap bitmap,int widthAndHight) {
        // 获取这个图片的宽和高
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 指定调整后的宽度和高度
        int newWidth = widthAndHight/*160*/;
        int newHeight = widthAndHight/*160*/;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, 	Bitmap.Config.ARGB_8888);
        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);
        targetCanvas.drawBitmap(bitmap, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        return targetBmp;
    }
    /**
     *      * 灰度图片黑白化，黑色是1，白色是0
     *      * @param x   横坐标
     *      * @param y   纵坐标
     *      * @param bit 位图
     *      * @return
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
     *
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

//add qr
    public void addSelectSizeOfModuleForQRCode(byte n) {
        byte[] command = new byte[]{29, 40, 107, 3, 0, 49, 67, 3};
        command[7] = n;
        this.addArrayToCommand(command);
    }

    public void addSelectErrorCorrectionLevelForQRCode(byte n) {
        byte[] command = new byte[]{29, 40, 107, 3, 0, 49, 69, n};
        this.addArrayToCommand(command);
    }

    public void addStoreQRCodeData(String content) {
        byte[] command = new byte[]{29, 40, 107, (byte) ((content.getBytes().length + 3) % 256), (byte) ((content.getBytes().length + 3) / 256), 49, 80, 48};
        this.addArrayToCommand(command);
        byte[] bs = null;
        if (!content.equals("")) {
            try {
                bs = content.getBytes("utf-8");
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            for (int i = 0; i < bs.length; ++i) {
                this.command.add(Byte.valueOf(bs[i]));
            }
        }

    }
    //开钱箱
    public void addGeneratePlus(LabelCommand.FOOT foot, byte t1, byte t2) {
        byte[] command = new byte[]{27, 112, (byte) foot.getValue(), t1, t2};
        this.addArrayToCommand(command);
    }
//***************************************//
    public void addPrintQRCode() {
        byte[] command = new byte[]{29, 40, 107, 3, 0, 49, 81, 48};
        this.addArrayToCommand(command);
    }

    private void addStrToCommand(String str) {
        byte[] bs = null;
        if (!str.equals("")) {
            try {
                bs = str.getBytes("GB2312");
            } catch (UnsupportedEncodingException var4) {
                var4.printStackTrace();
            }

            if (bs != null) {
                for (byte b : bs) {
                    this.command.add(b);
                }
            }
        }
    }

    /**
     * 添加文本
     *
     * @param text 文本
     */
    public void addText(String text) {
        this.addStrToCommand(text);
    }

    /**
     * 添加空行
     *
     * @param n 行数
     */
    public void addPrintAndFeedLines(byte n) {
        byte[] command = new byte[]{27, 100, n};
        this.addArrayToCommand(command);
    }

    /**
     * 切纸命令
     */
    public void addCutPaper() {
        byte[] bytes = new byte[]{(byte) 29, (byte) 86, (byte) 0};
        this.addArrayToCommand(bytes);
    }

    public void addCleanCache() {
        byte[] bytes = {(byte) 27, (byte) 74, (byte) 0};
        this.addArrayToCommand(bytes);
    }

    //0 居左 1居中 2居右
    public void addSelectJustification(int just) {
        byte[] command = new byte[]{27, 97, (byte) just};
        this.addArrayToCommand(command);
    }

    /**
     * 获取打印命令
     *
     * @return byte[] 打印命令
     */
    public byte[] getByteArrayCommand() {
        return convertToByteArray(getCommand());
    }

    public Vector<Byte> getCommand() {
        return this.command;
    }

    private byte[] convertToByteArray(Vector<Byte> vector) {
        if (vector == null || vector.isEmpty())
            return new byte[0];

        Byte[] bytes = vector.toArray(new Byte[vector.size()]);
        return toPrimitive(bytes);
    }

    private byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new byte[0];
        } else {
            byte[] result = new byte[array.length];
            for (int i = 0; i < array.length; ++i) {
                result[i] = array[i];
            }
            return result;
        }
    }
}
