package com.jimmy.printer.common;

import android.hardware.usb.UsbDevice;

import java.util.List;

public interface PrinterFinderCallback<C> {
    void onStart();

    void onFound(C c);

    void onFinished(List<C> cs);

    void onUnFind();//add
}
