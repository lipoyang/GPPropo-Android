package net.lipoyang.gpkonashi_lib;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.RequiresPermission;

import net.lipoyang.gpkonashi_lib.scanner.KonashiScanDialog;

/**
 * Created by shaga on 2016/11/26.
 */
public class GPkonashiManager extends BluetoothGattCallback {
    private static final long CONNECTION_TIMEOUT_LENGTH = 30000;

    public static final int ERROR_NOT_CONNECTED = 0x00;
    public static final int ERROR_INVALID_BAUDRATE = 0x01;
    public static final int ERROR_DISABLE_BLUETOOTH = 0x02;
    public static final int ERROR_FAILED_CONNECT = 0x03;
    public static final int ERROR_NOT_FOUND_SERVICE = 0x04;
    public static final int ERROR_NOT_FOUND_CHARACTERISTIC = 0x05;
    public static final int ERROR_FAILED_WRITE_VALUE = 0x06;
    public static final int ERROR_UART_DATA_TOO_SHORT = 0x07;
    public static final int ERROR_UART_DATA_TOO_LONG = 0x08;

    private Context mContext;

    private  GPkonashiListener mListener;

    private BluetoothManager mBtManager;
    private BluetoothAdapter mBtAdapter;

    private BluetoothGatt mGatt;

    private BluetoothGattService mKonashiService;
    private BluetoothGattCharacteristic mUartConfig;
    private BluetoothGattCharacteristic mUartBaudrate;
    private BluetoothGattCharacteristic mUartTx;
    private BluetoothGattCharacteristic mUartRx;

    private int mBaudRate = GPkonashi.UART_RATE_38K4;

    private boolean mIsConnected = false;
    private boolean mIsConnecting =false;

    private Handler mTimeoutHandler;

    private Runnable mConnectionTimeoutRunnabl = new Runnable() {
        @Override
        public void run() {
            onError(ERROR_FAILED_CONNECT);
            mTimeoutHandler = null;
        }
    };

    public GPkonashiManager(Context context) {
        mContext = context;

        mBtManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
    }

    public GPkonashiManager(Context context, int baudRate) {
        mContext = context;

        mBtManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();

        mBaudRate = baudRate;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void find(Activity activity) {
        find(activity, true);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void find(Activity activity, boolean isKonashiOnly) {
        KonashiScanDialog dialog = new KonashiScanDialog(new KonashiScanDialog.OnSelectKonashiListener() {
            @Override
            @RequiresPermission(Manifest.permission.BLUETOOTH)
            public void onSelectKonashi(BluetoothDevice device) {
                connect(device);
            }

            @Override
            public void onCancelSelect() {
                if (mListener != null) mListener.onDisconnect(GPkonashiManager.this);
            }
        });

        dialog.show(activity, isKonashiOnly);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean connect(BluetoothDevice device) {
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            onError(ERROR_DISABLE_BLUETOOTH);
            return false;
        }

        if (!GPkonashi.isValidBaudrate(mBaudRate)) onError(ERROR_INVALID_BAUDRATE);

        if (mGatt != null && isConnectingDevice(device)) {
            if (mGatt.connect()) return true;
        }

        if (mGatt != null) mGatt.close();

        mGatt = device.connectGatt(mContext, false, this);

        mIsConnecting = true;

        mTimeoutHandler = new Handler(mContext.getMainLooper());
        mTimeoutHandler.postDelayed(mConnectionTimeoutRunnabl, CONNECTION_TIMEOUT_LENGTH);

        return true;
    }

    public void disconnect() {
        if (mGatt == null) return;

        mGatt.disconnect();
    }

    public void close() {
        if (mGatt == null) return;

        mGatt.close();
        mGatt = null;
    }

    public void uartWrite(String value) {
        uartWrite(value.getBytes());
    }

    public void uartWrite(byte[] bytes) {
        if (mGatt == null) {
            onError(ERROR_NOT_CONNECTED);
            return;
        }

        int length = bytes.length;

        if (length > GPkonashi.UART_DATE_MAX_LENGTH) {
            onError(ERROR_UART_DATA_TOO_LONG);
            return;
        }

        byte[] value = new byte[length+1];
        value[0] = (byte)length;
        System.arraycopy(bytes, 0, value, 1, length);

        writeCharacteristic(mUartTx, value);
    }

    public void registerListener(GPkonashiListener listener) {
        mListener = listener;
    }

    public void unregisterListener(GPkonashiListener listener) {
        if (mListener.equals(listener)) mListener = null;
    }

    public boolean isConnected() { return mIsConnected; }
    public boolean isConnecting() { return mIsConnecting; }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (!isConnectingDevice(gatt)) return;

        if (mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacks(mConnectionTimeoutRunnabl);
            mTimeoutHandler = null;
        }

        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                gatt.discoverServices();
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                mIsConnected = false;
                mIsConnecting = false;
                if (mListener != null) mListener.onDisconnect(this);
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (!isConnectingDevice(gatt)) return;

        mKonashiService = gatt.getService(GPkonashiUuid.KONASHI_SERVICE);

        if (mKonashiService == null) {
            onError(ERROR_NOT_FOUND_SERVICE);
            gatt.disconnect();
            return;
        }

        mUartConfig = mKonashiService.getCharacteristic(GPkonashiUuid.UART_CONFIG);
        mUartBaudrate = mKonashiService.getCharacteristic(GPkonashiUuid.UART_BAUDRATE);
        mUartTx = mKonashiService.getCharacteristic(GPkonashiUuid.UART_TX);
        mUartRx = mKonashiService.getCharacteristic(GPkonashiUuid.UART_RX);

        if (mUartConfig == null || mUartBaudrate == null || mUartTx == null || mUartRx == null) {
            onError(ERROR_NOT_FOUND_CHARACTERISTIC);
            gatt.disconnect();
            return;
        }

        // set uart enable
        byte[] value = new byte[]{GPkonashi.UART_ENABLE};
        writeCharacteristic(mUartConfig, value);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (!isConnectingDevice(gatt)) return;

        if (characteristic.getUuid().equals(GPkonashiUuid.UART_RX) && mListener != null) {
            byte [] value = characteristic.getValue();

            int length = value[0];
            byte[] data = new byte[length];
            System.arraycopy(value, 1, data, 0, length);
            mListener.onUpdateUartRx(this, data);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if (!isConnectingDevice(gatt)) return;

        if (characteristic.getUuid().equals(GPkonashiUuid.UART_CONFIG)) {
            byte[] value = new byte[] { (byte)((mBaudRate >> 8) & 0xff), (byte)(mBaudRate & 0xff)};
            writeCharacteristic(mUartBaudrate, value);
        } else if (characteristic.getUuid().equals(GPkonashiUuid.UART_BAUDRATE)) {
            mGatt.setCharacteristicNotification(mUartRx, true);
            mIsConnected = true;
            mIsConnecting = false;
            if (mListener != null) mListener.onConnect(this);
        } else if (characteristic.getUuid().equals(GPkonashiUuid.UART_TX)) {
            if (status != BluetoothGatt.GATT) onError(ERROR_FAILED_WRITE_VALUE);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        if (!isConnectingDevice(gatt)) return;

        if (characteristic.getUuid().equals(GPkonashiUuid.UART_RX) && mListener != null) {
            byte [] value = characteristic.getValue();

            int length = value[0];
            byte[] data = new byte[length];
            System.arraycopy(value, 1, data, 0, length);
            mListener.onUpdateUartRx(this, data);
        }
    }

    private void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        int property = characteristic.getProperties();

        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 && (property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) ==0) {
            onError(ERROR_FAILED_WRITE_VALUE);
            return;
        }

        characteristic.setValue(value);
        mGatt.writeCharacteristic(characteristic);
    }

    private boolean isConnectingDevice(BluetoothGatt gatt) {
        return gatt.equals(mGatt);
    }

    private boolean isConnectingDevice(BluetoothDevice device) {
        return device.equals(mGatt.getDevice());
    }

    private void onError(int error) {
        if (mListener == null) return;

        mListener.onError(this, error);
    }
}
