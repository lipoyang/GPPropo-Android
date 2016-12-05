GP Propo (Android版)
=========

## 概要
GPduinoを使ったBLEラジコンのためのプロポアプリです。（Android版）  
GPduinoは、Arduino+Konashi互換のBLEラジコン制御ボードです。  
GPduinoに関する詳細は、[GPduino特設ページ](http://lipoyang.net/gpduino)をごらんください。

![概念図](image/BLE_overview.png)

ラジコンは、GPduinoとRCサーボやDCモータを組み合わせて作ります。  
下図はミニ四駆を改造して作ったラジコンです。

![ラジコンの写真](image/Mini4WD.jpg)

## アプリの操作

![アプリの画面](image/MainUI.png)

* BLEボタンを押すと、接続するデバイスを選択する画面になります。
* ボタンの色は橙が未接続、黄色が接続中、青が接続済を示します。
* 見てのとおり、ラジコンプロポの要領で2本のスティックを操作します。
* 設定ボタンを押すと、設定画面に遷移します。

![設定画面](image/SettingUI.png)

* RCサーボ CH0～2の、反転、トリム、ゲインを設定できます。
* REVのスイッチをONにすると、サーボの回転方向が反転します。
* TRIMの数値を上下すると、サーボのニュートラル位置を調整できます。
* GAINの数値を上下すると、サーボの回転幅を調整できます。
* SAVEボタンを押すと、設定をGPduinoの不揮発メモリに保存します。
* RELOADボタンを押すと、GPduinoの不揮発メモリから設定を読み出します。
* 4WS MODE は、RCサーボ CH1(前輪)
とCH2(後輪)を使った四輪操舵のモードを設定します。
    * FRONT は、前輪のみのステアリング
    * REAR は、後輪のみのステアリング
    * NORMAL は、前輪と後輪が同相の四輪操舵
    * REVERSE は、前輪と後輪が逆相の四輪操舵
* BATTERYは、バッテリー電圧を表示します。

## 動作環境
### システム要件
* Android端末: Android 4.3 (API Level 18)以上で、BluetoothでBLEが使用可能な機種
* Android Studio
* GPduino
* DCモータとRCサーボを有するラジコンカー または DCモータ2個を有するラジコン戦車

### 確認済み環境

* Android端末: Nexus7(2013), Android 5.1, xdpi 1920×1200 pixel

## ファイル一覧

* GPPropo/: プロポアプリのソース一式
* LICENSE: Apache Licence 2.0です
* README.md これ

## 開発環境と依存ライブラリ
* このアプリは、Android Studioで開発されました。
* このアプリは、ユカイ工学の[Konashi Android SDK](https://github.com/YUKAI/konashi-android-sdk)に依存しています。

<!--
* Eclipse環境の[旧Konashi Android SDK](https://github.com/YUKAI/konashi-v1-android-sdk)をベースに作成したアプリのソースは[こちら](http://licheng.sakura.ne.jp/gpduino/KoshiPropo_20151102.zip)にありますが、今後はメンテナンスされません。
-->
