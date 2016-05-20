/*
This software is available under NYSL(Nirunari Yakunari Sukinishiro License).
*/

#include <Servo.h>
#include <EEPROM.h>
#include "common.h"
#include "MovingAverage.h"

// ピン番号 (デジタル)
#define MOTOR_L_IN1    2
#define SERVO_PWM      3
#define MOTOR_L_IN2    4
#define MOTOR_L_PWM    5
#define MOTOR_R_PWM    6
#define MOTOR_R_IN1    7
#define MOTOR_R_IN2    8
#define LED_L_PWM      9
#define LED_R_PWM     10

// ピン番号 (アナログ)
#define ANALOG_0      0
#define ANALOG_1      1
#define ANALOG_2      2
#define ANALOG_3      3
#define MODE_CHECK    6
#define BATT_CHECK    7

// サーボ
Servo servo;

// バッテリー電圧チェック
MovingAverage Vbat_MovingAve;

// モード
static int drive_mode;
#define MODE_TANK   0   // 戦車モード
#define MODE_CAR    1   // 自動車モード

// 戦車モードのプロポ状態保持用
static int g_fb;  // 前後方向
static int g_lr;  // 左右方向

// 暴走チェックカウンタ
static int cnt_runaway;

// 3.3V未満でローバッテリーとする
// (3.3V / 2) / 3.3V * 1024 =  512
#define LOW_BATTERY    512

// 点滅制御
static int g_blink_state;
static int g_blink_cnt;

// サーボの調整
static int g_servo_pol; // 極性
static int g_servo_ofs; // オフセット
static int g_servo_amp; // 振幅

// デバッグ用
static char dbuff[256];

/**
 * バッテリー電圧チェック
 */
void battery_check()
{
    unsigned short Vbat = analogRead(BATT_CHECK);
    unsigned short Vbat_ave = Vbat_MovingAve.pop(Vbat);
    if(Vbat_ave < LOW_BATTERY){
      // モータ停止
      analogWrite(MOTOR_L_PWM, 0);
      analogWrite(MOTOR_R_PWM, 0);
      digitalWrite(MOTOR_L_IN1, LOW);
      digitalWrite(MOTOR_L_IN2, LOW);
      digitalWrite(MOTOR_R_IN1, LOW);
      digitalWrite(MOTOR_R_IN2, LOW);
      // LEDの点滅を繰り返し続ける (復帰しない)
      while(true){
            // Serial.println("LOW BATTERY!");
            led_ctrl(LED_L_PWM,   0);
            led_ctrl(LED_R_PWM,   0);
            delay(500);
            led_ctrl(LED_L_PWM,   255);
            led_ctrl(LED_R_PWM,   255);
            delay(500);
      }
    }
}

/**
 * 暴走チェック
 */
void runaway_check()
{
    cnt_runaway++;
    
    // 1秒間コマンドが来なければモータ停止
    if(cnt_runaway > 1000)
    {
      analogWrite(MOTOR_L_PWM, 0);
      analogWrite(MOTOR_R_PWM, 0);
      digitalWrite(MOTOR_L_IN1, LOW);
      digitalWrite(MOTOR_L_IN2, LOW);
      digitalWrite(MOTOR_R_IN1, LOW);
      digitalWrite(MOTOR_R_IN2, LOW);
    }
}

/*
 * サーボの初期化
 */
void servo_init()
{
    if(EEPROM.read(0) == 0xA5){
        g_servo_pol = (int)((signed char)EEPROM.read(1));
        g_servo_ofs = (int)((signed char)EEPROM.read(2));
        g_servo_amp = EEPROM.read(3);
    }else{
        g_servo_pol = 1;
        g_servo_ofs = 0;
        g_servo_amp = 90;
        EEPROM.write(0, 0xA5);
        EEPROM.write(1, g_servo_pol);
        EEPROM.write(2, g_servo_ofs);
        EEPROM.write(3, g_servo_amp);
    }
    servo.attach(SERVO_PWM);
    servo_ctrl(0);
}

/*
 * サーボ制御
 */
void servo_ctrl(int val)
{
    int deg = 90 + ((val + g_servo_ofs) *  g_servo_amp) / 127 * g_servo_pol;
    
    // sprintf(dbuff, "%4d %4d / %4d %4d %4d", val, deg, g_servo_ofs, g_servo_amp, g_servo_pol);
    // Serial.println(dbuff);
    
    servo.write(deg);
}

/*
 * LED制御
 */
void led_ctrl(int ch, int pwm)
{
    int val;
    
    // 自動車モードの場合
    if(drive_mode == MODE_CAR)
    {
        val = (pwm > 128) ? HIGH : LOW;
        digitalWrite(ch, val);
    }
    // 戦車モードの場合
    else
    {
        analogWrite(ch, pwm);
    }
}

/*
 * LED点滅
 */
void led_blink()
{
    int temp; 
    int pwm;
    
    switch(g_blink_state){
    case 1:
        g_blink_cnt++;
        if(g_blink_cnt>=1000) g_blink_cnt = 0;
        pwm = (g_blink_cnt >= 500) ? 255 : 0;
        led_ctrl(LED_L_PWM, pwm);
        led_ctrl(LED_R_PWM, pwm);
        break;
    case 2:
        g_blink_cnt++;
        if(g_blink_cnt>=500) g_blink_cnt = 0;
        pwm = (g_blink_cnt >= 250) ? 255 : 0;
        led_ctrl(LED_L_PWM, pwm);
        led_ctrl(LED_R_PWM, pwm);
        break;
    case 3:
        g_blink_cnt++;
        if(g_blink_cnt >= 512*4) g_blink_cnt=0;
        temp = g_blink_cnt / 4;
        pwm = (temp <= 255) ? temp : (511-temp);
        led_ctrl(LED_L_PWM, pwm);
        led_ctrl(LED_R_PWM, pwm);
        break;
    }
}

// 初期設定
void setup() {
    // シリアル通信の設定
    SerialCom_init();

    // PWMの初期化
    analogWrite(MOTOR_L_PWM, 0);
    analogWrite(MOTOR_R_PWM, 0);
    analogWrite(LED_L_PWM,   0);
    analogWrite(LED_R_PWM,   0);
    
    // GPIOの初期化
    digitalWrite(MOTOR_L_IN1, LOW);
    digitalWrite(MOTOR_L_IN2, LOW);
    digitalWrite(MOTOR_R_IN1, LOW);
    digitalWrite(MOTOR_R_IN2, LOW);
    digitalWrite(LED_L_PWM,   LOW);
    digitalWrite(LED_R_PWM,   LOW);
    pinMode(MOTOR_L_IN1, OUTPUT);
    pinMode(MOTOR_L_IN2, OUTPUT);
    pinMode(MOTOR_R_IN1, OUTPUT);
    pinMode(MOTOR_R_IN2, OUTPUT);
    pinMode(LED_L_PWM,   OUTPUT);
    pinMode(LED_R_PWM,   OUTPUT);
    
    // モード判定
    int mode_check = analogRead( MODE_CHECK );
    drive_mode = (mode_check > 512) ? MODE_CAR : MODE_TANK;

    if(drive_mode == MODE_CAR){
        // サーボの初期化
        servo_init();
    }
    
    // 変数初期化
    g_fb = 0;
    g_lr = 0;
    cnt_runaway = 0;
    Vbat_MovingAve.init();
    g_blink_state = 0;
}

// メインループ
void loop() {
    
    // シリアル受信
    SerialCom_loop();
    // バッテリー電圧チェック
    battery_check();
    // 暴走チェック
    runaway_check();
    // LED点滅
    led_blink();
    
    delay(1);
}

/*
 * モータの制御
 */
void ctrl_motor(int ch, int val)
{
    static const int IN1[]={MOTOR_L_IN1, MOTOR_R_IN1};
    static const int IN2[]={MOTOR_L_IN2, MOTOR_R_IN2};
    static const int PWM[]={MOTOR_L_PWM, MOTOR_R_PWM};
    
    int l,r;
    int lpwm,rpwm,lin1,lin2,rin1,rin2;
    
    int pwm = (abs(val) << 1) + 1;
    if(pwm<0) pwm = 0;
    if(pwm>255) pwm = 255;
    
    int in1,in2;
    if(val > 0){
        in1 = LOW;
        in2 = HIGH;
    }else if(val < 0){
        in1 = HIGH;
        in2 = LOW;
    }else{
        in1 = LOW;
        in2 = LOW;
        pwm = 0;
    }

    digitalWrite(IN1[ch], in1);
    digitalWrite(IN2[ch], in2);
    analogWrite(PWM[ch], pwm);
}

/**
 * 戦車のモータ制御
 */
void ctrl_tank()
{
    int l,r;
    int lpwm,rpwm,lin1,lin2,rin1,rin2;
    
    r = (int)(g_fb - g_lr/2);
    l = (int)(g_fb + g_lr/2);
    
    // sprintf(dbuff, "%4d %4d %4d %4d", l, r, g_fb, g_lr);
    // Serial.println(dbuff);
    
    ctrl_motor(0, l);
    ctrl_motor(1, r);
}

/**
 * 受信したコマンドの実行
 *
 * @param buff 受信したコマンドへのポインタ
 */
void SerialCom_callback(char* buff)
{
    unsigned short val;
    int sval, sval2;
    int deg;
    
    cnt_runaway = 0; // 暴走チェックカウンタのクリア
    
    // Serial.println(buff); // TEST
    
    switch(buff[0])
    {
    /* Dコマンド(前進/後退)
       書式: #Dxx$
       xx: 0のとき停止、正のとき前進、負のとき後退。
     */
    case 'D':
        // 値の解釈
        if( HexToUint16(&buff[1], &val, 2) != 0 ) break;
        sval = (int)((signed char)val);
        
        // 自動車モードの場合
        if(drive_mode == MODE_CAR)
        {
            ctrl_motor(0, sval);
        }
        // 戦車モードの場合
        else
        {
            g_fb = sval;
            ctrl_tank();
        }
        break;
        
    /* Tコマンド(旋回)
       書式: #Txx$
       xx: 0のとき中立、正のとき右旋回、負のとき左旋回
     */
    case 'T':
        // 値の解釈
        if( HexToUint16(&buff[1], &val, 2) != 0 ) break;
        sval = (int)((signed char)val);
        
        // 自動車モードの場合
        if(drive_mode == MODE_CAR)
        {
            servo_ctrl(sval);
        }
        // 戦車モードの場合
        else
        {
            g_lr = sval;
            ctrl_tank();
        }
        break;
        
    /* Mコマンド(モータ制御)
       書式1: #Mnxx$
       n: '1'はモータ1(左)、'2'はモータ2(右)
       xx: 0のとき停止、正のとき正転、負のとき反転
       
       書式2: #MAxxyy$
       xx: モータ1(左)  0のとき停止、正のとき正転、負のとき反転
       yy: モータ2(右)  0のとき中立、正のとき正転、負のとき反転
     */
    case 'M':
        switch(buff[1]){
        case '1':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            sval = (int)((signed char)val);
            ctrl_motor(0, sval);
            break;
        case '2':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            sval = (int)((signed char)val);
            ctrl_motor(1, sval);
            break;
        case 'A':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            sval = (int)((signed char)val);
            if( HexToUint16(&buff[4], &val, 2) != 0 ) break;
            sval2 = (int)((signed char)val);
            ctrl_motor(0, sval);
            ctrl_motor(1, sval2);
            break;
        }
        break;
        
    /* Sコマンド(サーボ制御)
       書式: #Sxx$
       xx: 0のとき中立、正のとき正転、負のとき反転
     */
    case 'S':
        // 値の解釈
        if( HexToUint16(&buff[1], &val, 2) != 0 ) break;
        sval = (int)((signed char)val);
        // サーボの制御
        servo_ctrl(sval);
        break;
    
    /* Lコマンド(LED制御)
       書式1: #Lnxx$
       n: '1'はLED1(左)、'2'はLED2(右)
       xx: 0のとき消灯、255のとき最大輝度で点灯
       
       書式2: #LAxxyy$
       xx: LED11(左)  0のとき消灯、255のとき最大輝度で点灯
       yy: LED12(右)  0のとき消灯、255のとき最大輝度で点灯
       
       書式3: #LXn$
       n: '1'は1秒周期での点滅、'2'は0.5秒周期での点滅、'3'は輝度が周期的に変化
          '0'は点滅停止
     */
    case 'L':
        switch(buff[1]){
        case '1':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            led_ctrl(LED_L_PWM, val);
            break;
        case '2':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            led_ctrl(LED_R_PWM, val);
            break;
        case 'A':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            led_ctrl(LED_L_PWM, val);
            if( HexToUint16(&buff[4], &val, 2) != 0 ) break;
            led_ctrl(LED_R_PWM, val);
            break;
        case 'X':
            val = (unsigned short)(buff[2] - '0');
            if(val <= 3){
                g_blink_state = val;
                g_blink_cnt = 0;
                if(g_blink_state == 0)
                {
                    led_ctrl(LED_L_PWM, 0);
                    led_ctrl(LED_R_PWM, 0);
                }
            }
            break;
        }
        break;
    
    case 'A':
        switch(buff[1]){
        case 'P':
            if(buff[2] == '+'){
                g_servo_pol = 1;
                EEPROM.write(1, g_servo_pol);
            }else if(buff[2] == '-'){
                g_servo_pol = -1;
                EEPROM.write(1, g_servo_pol);
            }
            break;
        case 'O':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            g_servo_ofs = (int)((signed char)val);
            EEPROM.write(2, g_servo_ofs);
            break;
        case 'A':
            // 値の解釈
            if( HexToUint16(&buff[2], &val, 2) != 0 ) break;
            g_servo_amp = (int)((signed char)val);
            EEPROM.write(3, g_servo_amp);
            break;
        }
        break;
    }
}
