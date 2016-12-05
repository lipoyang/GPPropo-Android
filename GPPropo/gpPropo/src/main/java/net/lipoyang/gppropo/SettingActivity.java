/*
 * Copyright (C) 2016 Bizan Nishimura (@lipoyang)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lipoyang.gppropo;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import info.izumin.android.bletia.BletiaException;

public class SettingActivity extends AppCompatActivity
        implements View.OnClickListener, NumericUpDownListener,
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener,
        SerialListener
{
    private final SettingActivity self = this;

    // Debugging
    private static final String TAG = "Setting";
    private static final boolean DEBUGGING = true;

    // Konashi
    private KonashiManager mKonashiManager;

    // Bluetooth state
    private BLEStatus btState = BLEStatus.DISCONNECTED;

    // serial receiver
    private SerialReceiver serialReceiver;

    private TextView textVbat;
    private Switch switchRev0, switchRev1, switchRev2;
    private NumericUpDownView viewTrim0, viewTrim1, viewTrim2;
    private NumericUpDownView viewGain0, viewGain1, viewGain2;

    final Handler handler = new Handler();

    // 4WS Mode
    private int mode4ws;
    private final int MODE_FRONT = 0;
    private final int MODE_COMMON = 1;
    private final int MODE_REVERSE = 2;
    private final int MODE_REAR = 3;

    //***** onCreate, onStart, onResume, onPause, onStop, onDestroy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUGGING) Log.e(TAG, "++ ON CREATE ++");

        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        mode4ws = data.getInt("mode4ws", 0 );

        setContentView(R.layout.activity_setting);

        switchRev0     = (Switch)findViewById(R.id.switchRev0);
        switchRev1     = (Switch)findViewById(R.id.switchRev1);
        switchRev2     = (Switch)findViewById(R.id.switchRev2);
        viewTrim0 = (NumericUpDownView)findViewById(R.id.viewTrim0);
        viewTrim1 = (NumericUpDownView)findViewById(R.id.viewTrim1);
        viewTrim2 = (NumericUpDownView)findViewById(R.id.viewTrim2);
        viewGain0 = (NumericUpDownView)findViewById(R.id.viewGain0);
        viewGain1 = (NumericUpDownView)findViewById(R.id.viewGain1);
        viewGain2 = (NumericUpDownView)findViewById(R.id.viewGain2);
        Button buttonSave     = (Button)findViewById(R.id.buttonSave);
        Button buttonReload   = (Button)findViewById(R.id.buttonReload);
        Spinner spinner4WS    = (Spinner)findViewById(R.id.spinner4WS);
        textVbat  = (TextView)findViewById(R.id.textVbat);

        switchRev0.setOnCheckedChangeListener(this);
        switchRev1.setOnCheckedChangeListener(this);
        switchRev2.setOnCheckedChangeListener(this);
        viewTrim0.setListener(this);
        viewTrim1.setListener(this);
        viewTrim2.setListener(this);
        viewGain0.setListener(this);
        viewGain1.setListener(this);
        viewGain2.setListener(this);
        buttonSave  .setOnClickListener(this);
        buttonReload.setOnClickListener(this);

        viewTrim0.setMaxMin(127, -127);
        viewTrim1.setMaxMin(127, -127);
        viewTrim2.setMaxMin(127, -127);
        viewGain0.setMaxMin(255, 0);
        viewGain1.setMaxMin(255,0);
        viewGain2.setMaxMin(255, 0);
        viewTrim0.setFormat("%1$+4d");
        viewTrim1.setFormat("%1$+4d");
        viewTrim2.setFormat("%1$+4d");
        viewGain0.setFormat("%1$4d");
        viewGain1.setFormat("%1$4d");
        viewGain2.setFormat("%1$4d");

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item);

        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        adapter.add("FRONT");
        adapter.add("REAR");
        adapter.add("NORMAL");
        adapter.add("REVERSE");
        spinner4WS = (Spinner) findViewById(R.id.spinner4WS);
        spinner4WS.setAdapter(adapter);
        spinner4WS.setOnItemSelectedListener(this);
        switch(mode4ws)
        {
            case MODE_FRONT:
                spinner4WS.setSelection(0);
                break;
            case MODE_REAR:
                spinner4WS.setSelection(1);
                break;
            case MODE_COMMON:
                spinner4WS.setSelection(2);
                break;
            case MODE_REVERSE:
                spinner4WS.setSelection(3);
                break;
        }

        // Konashi singleton
        mKonashiManager = Konashi.getManager();
        serialReceiver = new SerialReceiver();
        serialReceiver.setListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(DEBUGGING) Log.e(TAG, "++ ON START ++");
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(DEBUGGING) Log.e(TAG, "+ ON RESUME +");

        // add Konashi event listener
        mKonashiManager.addListener(mKonashiListener);

        btState = mKonashiManager.isReady() ? BLEStatus.CONNECTED : BLEStatus.DISCONNECTED;

        sendCommand("#AL$");
    }
    @Override
    public synchronized void onPause() {
        // remove Konashi event listener
        mKonashiManager.removeListener(mKonashiListener);

        super.onPause();
        if(DEBUGGING) Log.e(TAG, "- ON PAUSE -");
    }
    @Override
    public void onStop() {
        super.onStop();
        if(DEBUGGING) Log.e(TAG, "-- ON STOP --");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(DEBUGGING) Log.e(TAG, "--- ON DESTROY ---");
    }

    // On click any buttons
    public void onClick(View v) {
        String command;

        switch (v.getId()) {
            // [SAVE]
            case R.id.buttonSave:
                command = "#AS$";
                sendCommand(command);
                break;
            // [RELOAD]
            case R.id.buttonReload:
                command = "#AL$";
                sendCommand(command);
                break;
        }
    }

    // On change any switches
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String command;

        switch (buttonView.getId()) {
            // Rev 0
            case R.id.switchRev0:
                command = isChecked ? "#AP-0$" : "#AP+0$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            // Rev 1
            case R.id.switchRev1:
                command = isChecked ? "#AP-1$" : "#AP+1$";
                sendCommand(command);
                command = "#S001$";
                sendCommand(command);
                break;
            // Rev 2
            case R.id.switchRev2:
                command = isChecked ? "#AP-2$" : "#AP+2$";
                sendCommand(command);
                command = "#S002$";
                sendCommand(command);
                break;
        }
    }

    public void onChangeValue(NumericUpDownView view, int value){
        String command;

        switch (view.getId()) {
            case R.id.viewTrim0:
                command = "#AO" + String.format("%02X", (value & 0xFF)) + "0$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            case R.id.viewTrim1:
                command = "#AO" + String.format("%02X", (value & 0xFF)) + "1$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            case R.id.viewTrim2:
                command = "#AO" + String.format("%02X", (value & 0xFF)) + "2$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            case R.id.viewGain0:
                command = "#AA" + String.format("%02X", (value & 0xFF)) + "0$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            case R.id.viewGain1:
                command = "#AA" + String.format("%02X", (value & 0xFF)) + "1$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
            case R.id.viewGain2:
                command = "#AA" + String.format("%02X", (value & 0xFF)) + "2$";
                sendCommand(command);
                command = "#S000$";
                sendCommand(command);
                break;
        }
    }

    private void sendCommand(String command)
    {
        if(!mKonashiManager.isConnected()) return;

        byte [] bCommand=command.getBytes();
        mKonashiManager.uartWrite(bCommand)
                .fail(new FailCallback<BletiaException>() {
                    @Override
                    public void onFail(BletiaException result) {
                        //Toast.makeText(self, result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // On select 4WS spinner
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Spinner spinner = (Spinner) parent;
        int index = spinner.getSelectedItemPosition();
        switch(index){
            // [FRONT]
            case 0:
                mode4ws = MODE_FRONT;
                break;
            // [REAR]
            case 1:
                mode4ws = MODE_REAR;
                break;
            // [NORMAL]
            case 2:
                mode4ws = MODE_COMMON;
                break;
            // [REVERSE]
            case 3:
                mode4ws = MODE_REVERSE;
                break;
        }
        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putInt("mode4ws", mode4ws);
        editor.apply();
    }
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    // On received a serial command
    public void onCommandReceived(char[] data)
    {
        String strData = new String(data);
        switch(data[0])
        {
            // adjust servo
            case 'A':
                if (data[1] == 'L')
                {
                    final boolean[] pol = new boolean[3];
                    final int[] ofs = new int[3];
                    final int[] amp = new int[3];

                    try
                    {
                        for (int i = 0; i < 3; i++)
                        {
                            pol[i] = (data[2 + i * 5] == '-');
                            ofs[i] = Integer.parseInt(strData.substring(3 + i * 5, (3 + i * 5)+2), 16);
                            if (ofs[i] >= 128) ofs[i] -= 256;
                            amp[i] = Integer.parseInt(strData.substring(5 + i * 5, (5 + i * 5)+2), 16);
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                switchRev0.setChecked(pol[0]);
                                switchRev1.setChecked(pol[1]);
                                switchRev2.setChecked(pol[2]);
                                viewTrim0.setValue(ofs[0]);
                                viewTrim1.setValue(ofs[1]);
                                viewTrim2.setValue(ofs[2]);
                                viewGain0.setValue(amp[0]);
                                viewGain1.setValue(amp[1]);
                                viewGain2.setValue(amp[2]);
                                sendCommand("#S000$");
                                sendCommand("#S001$");
                                sendCommand("#S002$");
                            }
                        });
                    }
                    catch (Exception ex)
                    {
                        ;// do nothing
                    }
                }
                break;
            // battery voltage
            case 'B':
                int adc = Integer.parseInt(strData.substring(1, 1+3), 16);
                final double voltage = adc * 2 * 3.3 / 1024;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textVbat.setText(String.format("%1$5.2f V", voltage) );
                    }
                });
                break;
        }
    }

    /**
     * Konashi's Event Listener
     */
    private final KonashiListener mKonashiListener = new KonashiListener() {
        @Override
        public void onConnect(KonashiManager manager) {
            // Connected!
            btState = BLEStatus.CONNECTED;

            mKonashiManager.uartMode(Konashi.UART_ENABLE)
                    .then(new DoneCallback<BluetoothGattCharacteristic>() {
                        @Override
                        public void onDone(BluetoothGattCharacteristic result) {
                            mKonashiManager.uartBaudrate(Konashi.UART_RATE_38K4);

                        }
                    })
                    .fail(new FailCallback<BletiaException>() {
                        @Override
                        public void onFail(BletiaException result) {
                            Toast.makeText(self, result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        @Override
        public void onDisconnect(KonashiManager manager) {
            // Disconnected!
            btState = BLEStatus.DISCONNECTED;
            self.finish();
        }
        @Override
        public void onError(KonashiManager manager, BletiaException e) {

        }
        @Override
        public void onUpdatePioOutput(KonashiManager manager, int value) {

        }
        @Override
        public void onUpdateUartRx(KonashiManager manager, byte[] value) {
            // mResultText.setText(new String(value));
            serialReceiver.put(value);
        }
        @Override
        public void onUpdateBatteryLevel(KonashiManager manager, int level) {

        }
        @Override
        public void onUpdateSpiMiso(KonashiManager manager, byte[] value) {

        }
    };
}
