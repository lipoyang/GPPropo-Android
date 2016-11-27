package net.lipoyang.gpkonashi_lib.scanner;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.RequiresPermission;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.lipoyang.gpkonashi_lib.R;

import java.util.ArrayList;
import java.util.Deque;

/**
 * Created by shaga on 2016/11/26.
 */
public class KonashiListAdapter extends BaseAdapter {
    private final ArrayList<Pair<BluetoothDevice, Integer>> mDeviceList = new ArrayList<>();

    private Context mContext;

    public KonashiListAdapter(Context context) { mContext = context; }

    public void addKonashi(BluetoothDevice device, Integer rssi) {
        for (int i = 0; i < mDeviceList.size(); i++) {
            Pair<BluetoothDevice, Integer> pair = mDeviceList.get(i);
            if (pair.first.equals(device)) {
                mDeviceList.set(i, Pair.create(device, rssi));
                notifyDataSetChanged();
                return;
            }
        }

        mDeviceList.add(Pair.create(device, rssi));
        notifyDataSetChanged();
    }

    public void clearList() {
        mDeviceList.clear();
    }

    public Pair<BluetoothDevice, Integer> getDevice(int position) {
        if (position < 0 || mDeviceList.size() <= position) return null;

        return mDeviceList.get(position);
    }

    @Override
    public int getCount() { return mDeviceList.size(); }

    @Override
    public Object getItem(int i) { return mDeviceList.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;

        if (v == null) {
            v = LayoutInflater.from(mContext).inflate(R.layout.found_device_item, viewGroup, false);
        }

        Pair<BluetoothDevice, Integer> pair = getDevice(i);
        BluetoothDevice device = pair.first;
        Integer rssi = pair.second;

        setText(v, R.id.gpkonashi_lib_devname, device.getName());
        setText(v, R.id.gpkonashi_lib_devaddr, device.getAddress());
        setText(v, R.id.gpkonashi_lib_devrssi, String.format("RSSI: %ddB", rssi));

        return v;
    }

    private void setText(View view, int resId, String text) {
        TextView tv = (TextView) view.findViewById(resId);

        tv.setText(text);
    }
}
