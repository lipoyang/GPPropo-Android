package net.lipoyang.gpkonashi_lib.scanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.PermissionChecker;

/**
 * Created by shaga on 2016/11/26.
 */
public class KonashiScanner {
    private static final String TAG = KonashiScanner.class.getName();

    private BluetoothManager mBtManager;

    private BluetoothAdapter mBtAdapter;

    private BluetoothLeScanner mBleScanner;

    private ScanSettings mScanSettings;

    private ScanCallback mScanCallback;

    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    private KonashiScanListener mListener;

    private boolean mScanning;

    private boolean mKonashiOnly;

    public KonashiScanner(Context context, KonashiScanListener listener) {
        mListener = listener;
        mBtManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBtAdapter = mBtManager.getAdapter();

        if (isAfterLollipop()) {
            initScanCallback();
        } else {
            initLeScanCallback();
        }
    }

    public void setListener(KonashiScanListener listener) {
        mListener = listener;
    }

    public void clearListener() {
        mListener = null;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void startScan() {
        if (!mBtAdapter.isEnabled() || mScanning) return;

        mKonashiOnly = true;

        if (isAfterLollipop()) {
            mBleScanner.startScan(null, mScanSettings, mScanCallback);
        } else {
            mBtAdapter.startLeScan(mLeScanCallback);
        }

        mScanning = true;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void startScan(boolean konashiOnly) {
        if (!mBtAdapter.isEnabled() || mScanning) return;

        mKonashiOnly = konashiOnly;

        if (isAfterLollipop()) {
            mBleScanner.startScan(null, mScanSettings, mScanCallback);
        } else {
            mBtAdapter.startLeScan(mLeScanCallback);
        }

        mScanning = true;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public void stopScan() {
        if (!mScanning) return;

        if (isAfterLollipop()) {
            mBleScanner.stopScan(mScanCallback);
        } else {
            mBtAdapter.stopLeScan(mLeScanCallback);
        }

        mScanning = false;
    }

    public boolean isScanning() { return mScanning; }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initScanCallback() {
        if (!isAfterLollipop()) return;

        mBleScanner = mBtAdapter.getBluetoothLeScanner();
        mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        mScanCallback = new ScanCallback() {
            @Override
            @RequiresPermission(Manifest.permission.BLUETOOTH)
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                onFoundKonashi(result.getDevice(), result.getRssi());
            }
        };
    }

    private void initLeScanCallback() {
        if (isAfterLollipop()) return;

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            @RequiresPermission(Manifest.permission.BLUETOOTH)
            public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
                onFoundKonashi(bluetoothDevice, rssi);
            }
        };
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    private void onFoundKonashi(BluetoothDevice device, int rssi) {
        if (!device.getName().startsWith("konashi") || mListener == null) return;

        mListener.onFoundKonashi(device, rssi);
    }

    private boolean isAfterLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
