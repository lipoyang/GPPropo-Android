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

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NumericUpDownView extends LinearLayout
        implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener{

    NumericUpDownListener mListener;

    Button buttonUp;
    Button buttonDown;
    TextView textValue;

    /**
     * 連続してボタンを押す間隔のデフォルト値 (ms)
     */
    private static final int REPEAT_INTERVAL =100;
    private boolean isContinue = false;
    private View mLongClickedButton;
    final Handler handler = new Handler();

    private int mValue = 0;
    private int MAX_VALUE = 0;
    private int MIN_VALUE = 0;
    private String FORMAT = "%1$+4d";

    public NumericUpDownView(Context context, AttributeSet attr) {
        super(context, attr);

        View layout = LayoutInflater.from(context).inflate(R.layout.numeric_updown_view, this);

        buttonUp = (Button)layout.findViewById(R.id.buttonUp);
        buttonDown = (Button)layout.findViewById(R.id.buttonDown);
        textValue = (TextView)layout.findViewById(R.id.textValue);

        buttonUp.setOnClickListener(this);
        buttonUp.setOnLongClickListener(this);
        buttonUp.setOnTouchListener(this);
        buttonDown.setOnClickListener(this);
        buttonDown.setOnLongClickListener(this);
        buttonDown.setOnTouchListener(this);
    }

    public void setValue(int value)
    {
        mValue = value;
        textValue.setText(String.format(FORMAT, mValue));
    }
    public void setMaxMin(int max, int min)
    {
        MAX_VALUE = max;
        MIN_VALUE = min;
    }
    public void setFormat(String format)
    {
        FORMAT = format;
    }
    public void setListener(NumericUpDownListener listener)
    {
        mListener = listener;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            // [-]
            case R.id.buttonDown:
                if(mValue > MIN_VALUE) mValue--;
                textValue.setText(String.format(FORMAT, mValue));
                if(mListener!=null) mListener.onChangeValue(this, mValue);
                break;
            // [+]
            case R.id.buttonUp:
                if(mValue < MAX_VALUE) mValue++;
                textValue.setText(String.format(FORMAT, mValue));
                if(mListener!=null) mListener.onChangeValue(this, mValue);
                break;
        }
    }
    // On long click any buttons
    public boolean onLongClick(View v) {

        switch (v.getId()) {
            case R.id.buttonDown:
            case R.id.buttonUp:

                if(!isContinue){
                    mLongClickedButton = v;
                    isContinue = true;
                    handler.post(repeatRunnable);
                }
        }
        return false;
    }
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP) {
            isContinue = false;
        }
        return false;
    }

    final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isContinue) {
                return;
            }
            mLongClickedButton.performClick();
            handler.postDelayed(this, REPEAT_INTERVAL);
        }
    };
}
