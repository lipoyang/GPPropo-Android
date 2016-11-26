/*
 * Copyright (C) 2015 Bizan Nishimura (@lipoyang)
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;

//import android.content.Intent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import net.lipoyang.gpkonashi_lib.GPkonashi;
import net.lipoyang.gpkonashi_lib.GPkonashiListener;
import net.lipoyang.gpkonashi_lib.GPkonashiManager;

public class MainActivity extends Activity implements PropoListener{

    private final MainActivity self = this;

    // Debugging
    private static final String TAG = "GPPropo";
    private static final boolean DEBUGGING = true;
    
    // Konashi
    private GPkonashiManager mGPManager;
    
    // Propo View
    private PropoView propoView;
    
    // Bluetooth state
    private BLEStatus btState = BLEStatus.DISCONNECTED;
    
    // Motor
    private long lastUpdateTimeFB;
    private long lastUpdateTimeLR;

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
        
        // initialize PropoView
        setContentView(R.layout.activity_main);
        propoView = (PropoView)findViewById(R.id.propoView1);
        propoView.setParent(this,this);
        
        // initialize Konashi
        GPkonashi.initialize(getApplicationContext());
        mGPManager = GPkonashi.getManager();
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

        // 4WS Mode
        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        mode4ws = data.getInt("mode4ws", 0 );

        // initialize variables
        lastUpdateTimeFB = 0;
        lastUpdateTimeLR = 0;

        // add Konashi event listener
        mGPManager.registerListener(mGPListener);

        // Bluetooth Status
        if (mGPManager.isConnecting()) btState = BLEStatus.CONNECTING;
        else if (mGPManager.isConnected()) btState= BLEStatus.CONNECTED;
        else btState = BLEStatus.DISCONNECTED;
        propoView.setBtStatus(btState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLocationPermission();
        }
    }
    @Override
    public synchronized void onPause() {
        // remove Konashi event listener
        mGPManager.unregisterListener(mGPListener);
        
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
        // finalize Konashi
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mGPManager.isConnected()) {
                    GPkonashi.close();
                }
            }
        }).start();
        super.onDestroy();
        if(DEBUGGING) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    // On touch PropoView's Bluetooth Button
    public void onTouchBtButton()
    {
        // do nothing if connecting
        if (mGPManager.isConnecting()) return;

        if (!mGPManager.isConnected()) {
            // Bluetooth Status: connecting
            btState = BLEStatus.CONNECTING;
            propoView.setBtStatus(btState);

            // find konashi
            mGPManager.find(this);
        } else {
            // disconnet konashi
            mGPManager.disconnect();
        }
    }
    
    // On touch PropoView's Setting Button
    public void onTouchSetButton()
    {
        if (mGPManager.isConnected()) {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        }
    }
    
    // On touch PropoView's FB Stick
    // fb = -1.0 ... +1.0
    public void onTouchFbStick(float fb)
    {
        if (!mGPManager.isConnected()) return;
        boolean update = false;
        if(lastUpdateTimeFB + 50 < System.currentTimeMillis()) update = true;
        if(fb == 0.0) update = true;
        
        // send the Koshian a message.
        if (update){
            int bFB = (int)(fb * 127);
            if(bFB<0) bFB += 256;
            String command = "#D" + String.format("%02X", bFB) + "$";
            mGPManager.uartWrite(command);
            lastUpdateTimeFB = System.currentTimeMillis();
        }
    }
    
    // On touch PropoView's LR Stick
    // lr = -1.0 ... +1.0
    public void onTouchLrStick(float lr)
    {
        if (!mGPManager.isConnected()) return;
        boolean update = false;
        if(lastUpdateTimeLR + 50 < System.currentTimeMillis()) update = true;
        if(lr == 0.0) update = true;
        
        // send the Koshian a message.
        if (update){
            int bLR = (int)(lr * 127);
            if(bLR<0) bLR += 256;
            String command = "#T" + String.format("%02X", bLR) + String.format("%1d", mode4ws) + "$";
            mGPManager.uartWrite(command);
            lastUpdateTimeLR = System.currentTimeMillis();
        }
    }

    private final GPkonashiListener mGPListener = new GPkonashiListener() {
        @Override
        public void onConnect(GPkonashiManager manager) {
            // Connected!
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btState = BLEStatus.CONNECTED;
                    propoView.setBtStatus(btState);
                }
            });
        }

        @Override
        public void onDisconnect(GPkonashiManager manager) {
            // Disconnected!
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btState = BLEStatus.DISCONNECTED;
                    propoView.setBtStatus(btState);
                }
            });
        }

        @Override
        public void onError(GPkonashiManager manager, int error) {
            Toast.makeText(MainActivity.this, (CharSequence)String.format("konashi error: %d", error), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onUpdateUartRx(GPkonashiManager manager, byte[] value) {
        }
    };

    // permission request

    private static final int REQ_CODE_ALLOW_BLUETOOTH = 100;

    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ALLOW_BLUETOOTH);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_CODE_ALLOW_BLUETOOTH) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
