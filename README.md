GP Propo (Android版) 【工事中】
=========

## 概要
GPduinoを使ったBLEラジコンのためのプロポアプリです。（Android版）  
GPduinoは、Arduino+Konashi互換のBLEラジコン制御ボードです。  
GPduinoに関する詳細は、[GPduino特設ページ](http://lipoyang.net/gpduino)をごらんください。

![概念図](image/BLE_overview.png)

プロポアプリは、下図のようなUIです。

![アプリの画面](image/BLE_UI_small.png)

ラジコンは、GPduinoとRCサーボやDCモータを組み合わせて作ります。  
下図はミニ四駆を改造して作ったラジコンです。

![ラジコンの写真](image/Mini4WD.jpg)

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
* GPduino/ : GPduinoのファームウェア(Arduino互換のスケッチ)
* LICENSE: Apache Licence 2.0です
* README.md これ

## アプリの操作

* BLEボタンを押すと、接続するデバイスを選択する画面になります。
* ボタンの色は橙が未接続、黄色が接続中、青が接続済を示します。
* 見てのとおり、ラジコンプロポの要領で2本のスティックを操作します。

## 開発環境と依存ライブラリ
* このアプリは、Android Studioで開発されました。
* このアプリは、ユカイ工学の[Konashi Android SDK](https://github.com/YUKAI/konashi-android-sdk)に依存しています。
* Eclipse環境の[旧Konashi Android SDK](https://github.com/YUKAI/konashi-v1-android-sdk)をベースに作成したアプリのソースは[こちら](http://licheng.sakura.ne.jp/gpduino/KoshiPropo_20151102.zip)にありますが、今後はメンテナンスされません。
