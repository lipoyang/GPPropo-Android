package net.lipoyang.gpkonashi_lib;

/**
 * Created by shaga on 2016/11/26.
 */
public interface GPkonashiListener {
    void onConnect(GPkonashiManager manager);
    void onDisconnect(GPkonashiManager manager);
    void onError(GPkonashiManager manager, int error);
    void onUpdateUartRx(GPkonashiManager manager, byte[] value);
}
