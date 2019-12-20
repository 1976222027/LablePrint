package com.apicloud.mylableprint;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.jimmy.gprint.EscCommand;
import com.jimmy.gprint.GpUtils;
import com.jimmy.gprint.LabelCommand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Vector;

/**
 * 打印模板
 */
public class PrintMould {
    /**
     * 打印商品价签模板 50x30 40x30  78x45  78x50
     * <p>
     * *********天猫超市**********
     * 单价：11,0    会员价：10.00
     * |||||||||||||||||||||
     * 1212436465768797979
     * 日期：2019-11-01
     * 尾注尾注尾注尾注尾注
     */
    public static byte[] printLable(JSONArray jsonArray, int size) {
        int x = 0;// 打印纸一行最大的字节    3mm一个字 一个字2*字节
        //字节数= x=(纸宽/3)*2
        LabelCommand tsc = new LabelCommand();
        switch (size) {
            case 100://40*30标签
                x = 26;
                tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置
                tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
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
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(0, 18, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));
                        }
                        tsc.addText(8, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                printTwoData(price == 0 ? "" : "单价:" + price, vipPrice == 0 ? "" : "会员价:" + vipPrice, x));

                        if (!TextUtils.isEmpty(barCode)) { //条码
                            tsc.add1DBarcode(8, 79, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
                        }

                        if (!TextUtils.isEmpty(time)) {
                            tsc.addText(8, 169, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    "日期:" + time);
                        }
                        //尾注
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(8, 195, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    endnotes);
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 0://50*30标签签
                x = 31;
                tsc.addSize(50, 30);
                tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
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
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(8, 18, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));
                        }
                        tsc.addText(18, 50, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                price == 0 ? "单价:" : "单价:" + price);
                        tsc.addText(200, 50, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                vipPrice == 0 ? "" : "会员价:" + vipPrice);

                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(/*18*/ code128W(50,barCode),80, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
                        }

                        if (!TextUtils.isEmpty(time)) {
                            tsc.addText(18, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle("日期:" + time,x));
                        }
                        //尾注
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(18, 202, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(endnotes,x));
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 1://10代表50*30黑白价签
                x = 31;
                tsc.addSize(50, 30);
                tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
                tsc.addCls();// 清除打印缓冲区

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject itemJson = jsonArray.getJSONObject(i);
                        String barCode = itemJson.optString("barCode");
                        String name = itemJson.optString("name");
                        String spec = itemJson.optString("spec");
                        double price = itemJson.optDouble("price", 0.00);
                        String unit = itemJson.optString("unit");
                        String priceType = itemJson.optString("priceType");
                        double vipPrice = itemJson.optDouble("vipPrice", 0.00);
                        int number = itemJson.optInt("number", 1);
                        String endnotes = itemJson.optString("endnotes");
                        if (TextUtils.isEmpty(barCode)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(8, 18, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));//居中商品名
                        }
                        tsc.addText(18, 50, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(spec) ? "规格:" : "规格:" + spec);
                        tsc.addText(200, 50, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(unit) ? "单位:" : "单位:" + unit);
                        if (vipPrice == 0) {
                            tsc.addText(18, 85, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    TextUtils.isEmpty(priceType) ? " " : "计价方式:" + priceType);
                            tsc.addText(200, 85, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    price == 0 ? " " : "零售价:" + price);
                        } else {
                            tsc.addText(18, 85, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    price == 0 ? " " : "零售价:" + price);
                            tsc.addText(200, 85, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    vipPrice == 0 ? " " : "会员价:" + vipPrice);
                        }

                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(/*18*/(code128W(50,barCode)), 122, LabelCommand.BARCODETYPE.CODE128, 50, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
                        }
                        //尾注
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(0, 207, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(endnotes, x));
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 10://10代表68x38标签
                x = 43;
                tsc.addSize(68, 38);
                tsc.addGap(3); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
//        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝上
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
                tsc.addCls();// 清除打印缓冲区
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject itemJson = jsonArray.getJSONObject(i);
                        String barCode = itemJson.optString("barCode");
                        String name = itemJson.optString("name");
                        String time = itemJson.optString("labeldate");
                        double price = itemJson.optDouble("price", 0.00);
                        double vipPrice = itemJson.optDouble("vipPrice", 0.00);
                        String shopName = itemJson.optString("shopName");
                        String endnotes = itemJson.optString("endnotes");
                        int number = itemJson.optInt("number", 1);
                        if (TextUtils.isEmpty(barCode)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(0, 25, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));
                        }
                        tsc.addText(20, 65, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                price == 0 ? "" : "单价:" + price);
                        tsc.addText(280, 65, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                vipPrice == 0 ? "" : "会员价:" + vipPrice);

                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(/*24*/code128W(68,barCode), 105, LabelCommand.BARCODETYPE.CODE128, 70, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
                        }

                        if (!TextUtils.isEmpty(time)) {
                            tsc.addText(18, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle("日期:" + time,x));
                        }
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(20, 245, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(endnotes,x));

                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 11://2代表68x38黑白价签
                x = 43;
                tsc.addSize(68, 38);
                tsc.addGap(3); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
//        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝上
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
                tsc.addCls();// 清除打印缓冲区
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject itemJson = jsonArray.getJSONObject(i);
                        String barCode = itemJson.optString("barCode");
                        String name = itemJson.optString("name");
                        String spec = itemJson.optString("spec");
                        double price = itemJson.optDouble("price", 0.00);
                        double vipPrice = itemJson.optDouble("vipPrice", 0.00);
                        String unit = itemJson.optString("unit");
//                        String packdate = itemJson.optString("packdate");
                        int number = itemJson.optInt("number", 1);
                        String endnotes = itemJson.optString("endnotes");
                        if (TextUtils.isEmpty(barCode)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(0, 25, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));
                        }
                        tsc.addText(18, 65, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(spec) ? "规格:" : "规格:" + spec);
                        tsc.addText(280, 65, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(unit) ? "单位:" : "单位:" + unit);

                        if (vipPrice == 0) {
                            tsc.addText(18, 100, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    TextUtils.isEmpty(spec) ? " " : "规格:" + spec);
                            tsc.addText(280, 100, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    price == 0 ? " " : "零售价:" + price);

                        } else {
                            tsc.addText(18, 100, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    price == 0 ? " " : "零售价:" + price);
                            tsc.addText(280, 100, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    vipPrice == 0 ? " " : "会员价:" + vipPrice);

                        }

                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(/*18*/code128W(68,barCode), 130, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0,
                                    barCode);
                        }
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(0, 218, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(endnotes, x));

                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break;
            case 12://12代表68x38彩色价签 规格单位  产地等级
                x = 38;
                tsc.addSize(68, 38);
                tsc.addGap(3); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
//        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝上
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
                tsc.addCls();// 清除打印缓冲区

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject itemJson = jsonArray.getJSONObject(i);
                        String barCode = itemJson.optString("barCode");
                        String name = itemJson.optString("name");
                        String place = itemJson.optString("place");
                        double price = itemJson.optDouble("price", 0.00);
                        String level = itemJson.optString("level");
                        String spec = itemJson.optString("spec");
                        String unit = itemJson.optString("unit");
                        String shopName = itemJson.optString("shopName");
                        int number = itemJson.optInt("number", 1);
                        if (TextUtils.isEmpty(barCode)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(shopName)) {
                            tsc.addText(240, 30, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    shopName);
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(110, 82, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    name);
                        }

                        tsc.addText(75, 140, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(spec) ? "" : "" + spec);//规格:
                        tsc.addText(215, 140, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(unit) ? "" : "" + unit);//单位:
//                        tsc.addText(80, 120, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                               printTwoData("guige","danwei",12) );

                        tsc.addText(75, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(place) ? "" : "" + place);//产地:
                        tsc.addText(215, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(level) ? "" : "" + level);//等级:
//                        tsc.addText(80, 150, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                                printTwoData("guige","danwei",12) );
                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(70, 200, LabelCommand.BARCODETYPE.CODE128, 40, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);

                        }

                        if (price != 0) {
                            String valuePrice=(int)price==price?String.valueOf((int)price):String.valueOf(price);
                            int len=getBytesLength(valuePrice);
                            if (len>6) {
//                                tsc.addText(363, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_3, String.valueOf(price));
                                tsc.addText(363, 222, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                                        valuePrice);
                            }else if (len>4){
                                tsc.addText(370, 222, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                                        valuePrice);
                            }
                            else if (len>3){
                                tsc.addText(378, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_3, LabelCommand.FONTMUL.MUL_3,
                                        valuePrice);
                            }else if (len>0){// 123
                                tsc.addText(400, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_3, LabelCommand.FONTMUL.MUL_3,
                                        valuePrice);
                            }
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break; case 13://13代表68x38彩色价签、 产地单位 规格 等级
                x = 38;
                tsc.addSize(68, 38);
                tsc.addGap(3); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
//                tsc.addOffset(38);//控制每张标签停止位置 mm
//                tsc.addLimitFeed(38);//限制走纸长度 到止未找到分割线 报错
//                tsc.addHome();//校验
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
//        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝上
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
                tsc.addCls();// 清除打印缓冲区

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject itemJson = jsonArray.getJSONObject(i);
                        String barCode = itemJson.optString("barCode");
                        String name = itemJson.optString("name");
                        String place = itemJson.optString("place");
                        double price = itemJson.optDouble("price", 0.00);
                        String level = itemJson.optString("level");
                        String spec = itemJson.optString("spec");
                        String unit = itemJson.optString("unit");
                        String shopName = itemJson.optString("shopName");
                        int number = itemJson.optInt("number", 1);
                        if (TextUtils.isEmpty(barCode)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(shopName)) {
                            tsc.addText(240, 30, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    shopName);
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(110, 82, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    name);
                        }
                        tsc.addText(75, 140, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(place) ? "" : "" + place);//产地:

                        tsc.addText(215, 140, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(unit) ? "" : "" + unit);//单位:
//                        tsc.addText(80, 120, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                               printTwoData("guige","danwei",12) );

                        tsc.addText(75, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(spec) ? "" : "" + spec);//规格:
                        tsc.addText(215, 170, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                TextUtils.isEmpty(level) ? "" : "" + level);//等级:
//                        tsc.addText(80, 150, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
//                                printTwoData("guige","danwei",12) );
                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(70, 200, LabelCommand.BARCODETYPE.CODE128, 40, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);

                        }

                        if (price != 0) {
//                            tsc.addText(363/*310*/, 210/*160*/, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_3,
//                                    price + "");
                            //判断是整数否
                           String valuePrice=(int)price==price?String.valueOf((int)price):String.valueOf(price);
                           int len=getBytesLength(valuePrice);
                            if (len>6) {
//                                tsc.addText(363, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_3, String.valueOf(price));
                                tsc.addText(363, 222, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                                        valuePrice);
                            }else if (len>4){
                                tsc.addText(370, 222, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                                        valuePrice);
                            }
                            else if (len>3){
                                tsc.addText(378, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_3, LabelCommand.FONTMUL.MUL_3,
                                        valuePrice);
                            }else if (len>0){// 123
                                tsc.addText(400, 210, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_3, LabelCommand.FONTMUL.MUL_3,
                                        valuePrice);
                            }
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break;
            default:
                x = 31;
                tsc.addSize(50, 30);
                tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
                tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
                tsc.addReference(0, 0);// 设置原点坐标
                tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
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
                        if (!TextUtils.isEmpty(name)) {
                            tsc.addText(0, 18, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    printTitle(name, x));
                        }
                        tsc.addText(18, 45, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                printTwoData(price == 0 ? "单价:" : "单价:" + price, vipPrice == 0 ? "" : "会员价:" + vipPrice, x));

                        if (!TextUtils.isEmpty(barCode)) {
                            //条码
                            tsc.add1DBarcode(18, 75, LabelCommand.BARCODETYPE.CODE128, 60, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);
                        }

                        if (!TextUtils.isEmpty(time)) {
                            tsc.addText(18, 165, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    "日期:" + time);
                        }
                        //尾注
                        if (!TextUtils.isEmpty(endnotes)) {
                            tsc.addText(18, 195, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                                    endnotes);
                        }
                        tsc.addPrint(1, number); // 打印标签份数
                        tsc.addCls();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
//        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        return bytes;
    }

    /**
     * 打印彩色价签模板  76x50
     */
    public static byte[] printColorLable(JSONArray jsonArray, int size) {
        int x = 0;// 打印纸一行最大的字节
        LabelCommand tsc = new LabelCommand();
        switch (size) {
            case 0:
                tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置
                break;
            case 1:
                tsc.addSize(50, 30);
                break;
            case 2:
                tsc.addSize(68, 38);
                break;
            case 3:
                tsc.addSize(76, 50);
                break;
            default:
                tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置
                break;
        }
        tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝下
//        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向 朝上
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
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

                if (!TextUtils.isEmpty(name)) {
                    tsc.addText(70, 60, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                            name);
                }

                tsc.addText(30, 130, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                        price == 0 ? "" : "产地:" + price);
                tsc.addText(130, 130, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                        vipPrice == 0 ? "" : "单位:" + vipPrice);

                tsc.addText(30, 160, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                        price == 0 ? "" : "规格:" + price);
                tsc.addText(130, 160, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                        vipPrice == 0 ? "" : "等级:" + vipPrice);

                if (!TextUtils.isEmpty(barCode)) {
                    //条码
                    tsc.add1DBarcode(90, 190, LabelCommand.BARCODETYPE.CODE128, 40, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, barCode);

                }

                if (!TextUtils.isEmpty(endnotes)) {
                    tsc.addText(375, 190, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_3, LabelCommand.FONTMUL.MUL_3,
                            endnotes);
                }
                tsc.addPrint(1, number); // 打印标签份数
                tsc.addCls();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
//        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        return bytes;
    }

    /**
     * 获取数据长度
     * 计算某个文字所占的字节数
     *
     * @param msg
     * @return
     */
    @SuppressLint("NewApi")
    public static int getBytesLength(String msg) {
        return msg.getBytes(Charset.forName("GB2312")).length;
    }

    /**
     * 打印标题
     *
     * @return
     */
    @SuppressLint("NewApi")
    public static String printTitle(String middleText, int x) {
        StringBuilder sb = new StringBuilder();
        int middleTextLength = getBytesLength(middleText);
        Log.e("mhy字符长",middleTextLength+"#"+x);
        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = (x - middleTextLength) / 2;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(middleText);
        return sb.toString();
    }

    //return条码宽度 mm
    //只有纯数字是 code128C 2位一码 奇数位+1
    public static int code128W(int wih,String str) {
        //定义三个变量用来统计大写字母，小写字母，数字的个数
        int bCount = 0;//非数字字符
        int numCount = 0;
        //遍历字符串，对每个字符进行判断
        for (int i = 0; i < str.length(); i++) {
            char chs = str.charAt(i);
            //如果
            if (chs >= 'A' && chs <= 'Z') {
                bCount++;
            } else if (chs >= 'a' && chs <= 'z') {
                bCount++;
            } else if (chs >= '0' && chs <= '9') {
                numCount++;
            } else {
                bCount++;
            }
        }
        //输出结果
        Log.e("codeA1/Ab-1字符：" , bCount + "个");
        Log.e("codeC1数字字符：" ,numCount + "个");
        int count = bCount + (numCount + 1) / 2;
       // L = (11字符数 + 35)*0.416306 毫米

        return (wih-(int)((11*count+35)*0.416306/1.55))*4;// /2*8点;//计算比实际大1.55 可能是缩放
    }
    /***
     *
     * 统计字符串中中文，英文，数字，空格等字符个数
     * @param str 需要统计的字符串
     *   String str = "adbs13姿z势12年概~3!a @x # $率 论zs12 szsgss  1234@#￥說說愛き ，。？！%……&*（）——{}【】";
     *   count(str);
     *         }
     */
    public void count(String str) {
        /**中文字符 */
        int chCharacter = 0;
        /**英文字符 */
        int enCharacter = 0;
        /**空格 */
        int spaceCharacter = 0;
        /**数字 */
        int numberCharacter = 0;
        /**其他字符 */
        int otherCharacter = 0;
        if (null == str || str.equals("")) {
            System.out.println("字符串为空");
            return;
        }
        for (int i = 0; i < str.length(); i++) {
            char tmp = str.charAt(i);
            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
                enCharacter++;
            } else if ((tmp >= '0') && (tmp <= '9')) {
                numberCharacter++;
            } else if (tmp == ' ') {
                spaceCharacter++;
            } else if (isChinese(tmp)) {
                chCharacter++;
            } else {
                otherCharacter++;
            }
        }
        System.out.println("字符串:" + str + "");
        System.out.println("中文字符有:" + chCharacter);
        System.out.println("英文字符有:" + enCharacter);
        System.out.println("数字有:" + numberCharacter);
        System.out.println("空格有:" + spaceCharacter);
        System.out.println("其他字符有:" + otherCharacter);
    }

    /***
     * 判断字符是否为中文
     * @param ch 需要判断的字符
     * @return 中文返回true，非中文返回false
     */
    private  boolean isChinese(char ch) {
        //获取此字符的UniCodeBlock
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        //  GENERAL_PUNCTUATION 判断中文的“号
        //  CJK_SYMBOLS_AND_PUNCTUATION 判断中文的。号
        //  HALFWIDTH_AND_FULLWIDTH_FORMS 判断中文的，号
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            System.out.println(ch + " 是中文");
            return true;
        }
        return false;
    }

    /**
     * 打印两列
     *
     * @param leftText  左侧文字
     * @param rightText 右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printTwoData(String leftText, String rightText, int x) {
        StringBuilder sb = new StringBuilder();
        int leftTextLength = getBytesLength(leftText);
        int rightTextLength = getBytesLength(rightText);
        sb.append(leftText);

        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = x - leftTextLength - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        // 前面有空间距 防止后边越界 删除1空格
        sb.delete(sb.length() - 1, sb.length());
        sb.append(rightText);
        return sb.toString();
    }

    /**
     * 打印三列时，第一列汉字最多显示几个文字
     */
    private final int LEFT_TEXT_MAX_LENGTH = 5;

    /**
     * 打印三列
     *
     * @param leftText   左侧文字
     * @param middleText 中间文字
     * @param rightText  右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public String printThreeData(String leftText, String middleText, String rightText, int x) {
        StringBuilder sb = new StringBuilder();
        // 左边最多显示 LEFT_TEXT_MAX_LENGTH 个汉字 + 两个点
        if (leftText.length() > LEFT_TEXT_MAX_LENGTH) {
            leftText = leftText.substring(0, LEFT_TEXT_MAX_LENGTH) + "..";
        }
        int leftTextLength = getBytesLength(leftText);
        int middleTextLength = getBytesLength(middleText);
        int rightTextLength = getBytesLength(rightText);

        sb.append(leftText);
        // 计算左侧文字和中间文字的空格长度
        int marginBetweenLeftAndMiddle = x / 2 - leftTextLength - middleTextLength / 2;

        for (int i = 0; i < marginBetweenLeftAndMiddle; i++) {
            sb.append(" ");
        }
        sb.append(middleText);

        // 计算右侧文字和中间文字的空格长度
        int marginBetweenMiddleAndRight = x / 2 - middleTextLength / 2 - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }

        // 打印的时候发现，最右边的文字总是偏右一个字符，所以需要删除一个空格
        sb.delete(sb.length() - 1, sb.length()).append(rightText);
        return sb.toString();
    }

}
