package net.lipoyang.gpkonashi_lib.scanner;

import android.bluetooth.BluetoothDevice;

/**
 * Created by shaga on 2016/11/26.
 */
public interface KonashiScanListener {
    void onFoundKonashi(BluetoothDevice device, int rssi);
}
