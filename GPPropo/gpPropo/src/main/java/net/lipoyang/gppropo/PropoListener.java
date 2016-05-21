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

public interface PropoListener {

    // On touch PropoView's Bluetooth Button
    void onTouchBtButton();

    // On touch PropoView's Setting Button
    void onTouchSetButton();

    // On touch PropoView's FB Stick
    // fb = -1.0 ... +1.0
    void onTouchFbStick(float fb);

    // On touch PropoView's LR Stick
    // lr = -1.0 ... +1.0
    void onTouchLrStick(float lr);
}
