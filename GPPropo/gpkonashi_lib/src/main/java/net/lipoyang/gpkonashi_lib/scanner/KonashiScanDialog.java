package net.lipoyang.gpkonashi_lib.scanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.lipoyang.gpkonashi_lib.R;

/**
 * Created by shaga on 2016/11/26.
 */
public class KonashiScanDialog implements AdapterView.OnItemClickListener {
    private static final long SCAN_TIMEOUT_LENGTH = 10000;

    private KonashiListAdapter mAdapter;

    private OnSelectKonashiListener mSelectListener;

    private LinearLayout mFindingLayout;

    private TextView mNotFoundText;

    private AlertDialog mDialog;

    private KonashiScanner mScanner;

    private Handler mTimeoutHandler;

    public KonashiScanDialog(OnSelectKonashiListener listener) { mSelectListener = listener; }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void show(final Activity activity, boolean isKonashiOnly) {
        mAdapter = new KonashiListAdapter(activity);

        mAdapter.clearList();

        mScanner = new KonashiScanner(activity, new KonashiScanListener() {
            @Override
            public void onFoundKonashi(BluetoothDevice device, int rssi) {
                final Integer r = new Integer(rssi);
                final BluetoothDevice d = device;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addKonashi(d, r);
                    }
                });
            }
        });

        View view = LayoutInflater.from(activity).inflate(R.layout.scan_dialog, null);

        ListView list = (ListView) view.findViewById(R.id.gpkonashi_lib_list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setScrollingCacheEnabled(false);

        mFindingLayout = (LinearLayout) view.findViewById(R.id.finding_layout);
        mFindingLayout.setVisibility(View.VISIBLE);
        mNotFoundText = (TextView) view.findViewById(R.id.text_not_found);
        mNotFoundText.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.gpkonashi_lib_dialog_title)
                .setView(view)
                .setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mSelectListener != null) mSelectListener.onCancelSelect();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (mSelectListener != null) mSelectListener.onCancelSelect();
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (mTimeoutHandler != null) {
                            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
                            mTimeoutHandler = null;
                        }
                        if (mScanner != null && mScanner.isScanning()) {
                            mScanner.stopScan();
                        }
                    }
                });

        mDialog = builder.create();

        mDialog.show();

        mScanner.startScan(isKonashiOnly);

        mTimeoutHandler = new Handler(activity.getMainLooper());
        mTimeoutHandler.postDelayed(mTimeoutRunnable, SCAN_TIMEOUT_LENGTH);
    }

    private Runnable mTimeoutRunnable = new Runnable() {
        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
        public void run() {
            if (mDialog == null) return;

            if (mScanner != null && mScanner.isScanning()) mScanner.stopScan();

            mFindingLayout.setVisibility(View.GONE);

            if (mAdapter.getCount() == 0) mNotFoundText.setVisibility(View.VISIBLE);

            mTimeoutHandler = null;
        }
    };


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mDialog != null) mDialog.dismiss();

        if (mSelectListener != null) mSelectListener.onSelectKonashi(mAdapter.getDevice(i).first);
    }



    public interface OnSelectKonashiListener {
        public void onSelectKonashi(BluetoothDevice device);
        public void onCancelSelect();
    }
}
