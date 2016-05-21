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

public class SerialReceiver {

    // receiving state
    int rxState;
    final int STATE_READY = 0;
    final int STATE_RECEIVING = 1;

    // receiving buffer
    int rxPtr;
    char[] rxBuff;

    // start of text / end of text
    char STX_CODE = '#';
    char ETX_CODE = '$';

    SerialListener listener;

    // constructor
    public SerialReceiver()
    {
        rxState = STATE_READY;
        rxPtr = 0;
        rxBuff = new char[1024];
    }

    // receiving
    public void put(byte[] data)
    {
        char c;
        for (int i = 0; i < data.length; i++)
        {
            c = (char)data[i];

            switch (rxState)
            {
                case STATE_READY:
                    if (c == STX_CODE)
                    {
                        rxState = STATE_RECEIVING;
                        rxPtr = 0;
                    }
                    break;
                case STATE_RECEIVING:
                    if (c == STX_CODE)
                    {
                        rxPtr = 0;
                    }
                    else if (c == ETX_CODE)
                    {
                        // call event handler
                        rxBuff[rxPtr] = '\0';
                        listener.onCommandReceived(rxBuff);
                        rxState = STATE_READY;
                    }
                    else
                    {
                        rxBuff[rxPtr] = c;
                        rxPtr++;
                        if (rxPtr >= 5000)
                        {
                            rxState = STATE_READY;
                        }
                    }
                    break;
                default:
                    rxState = STATE_READY;
                    break;
            }
        }
    }

    // set event listener
    public void setListener(SerialListener listener){
        this.listener = listener;
    }
}
