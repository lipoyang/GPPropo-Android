package net.lipoyang.gpkonashi_lib;

import android.content.Context;

/**
 * Created by shaga on 2016/11/26.
 */
public class GPkonashi {
    public static final int UART_DISABLE = 0;
    public static final int UART_ENABLE = 1;

    public static final int UART_RATE_9K6 = 0x0028;
    public static final int UART_RATE_19K2 = 0x0050;
    public static final int UART_RATE_38K4 = 0x00a0;
    public static final int UART_RATE_57K6 = 0x00f0;
    public static final int UART_RATE_76K8 = 0x0140;
    public static final int UART_RATE_115K2 = 0x01e0;

    public static final int UART_DATE_MAX_LENGTH = 18;

    public static boolean isValidBaudrate(int baudrate) {
        return baudrate == UART_RATE_9K6 || baudrate == UART_RATE_19K2 || baudrate == UART_RATE_38K4 ||
                baudrate == UART_RATE_57K6 || baudrate == UART_RATE_76K8 || baudrate == UART_RATE_115K2;
    }

    private static GPkonashiManager mManager;

    public static void initialize(Context context) {
        mManager = new GPkonashiManager(context);
    }

    public static void initialize(Context context, int baudRate) { mManager = new GPkonashiManager(context,baudRate); }

    public static GPkonashiManager getManager() {
        return mManager;
    }

    public static void close() {
        if (mManager == null) return;;

        mManager.close();
    }
}
