# 個人天気

Kotlin / Jetpack Compose / Material 3 / Jetpack Glanceで作った個人用のAndroid天気アプリです。APIキー不要のOpen-Meteo Forecast APIを通常予報に使い、気象庁の雨雲レーダータイルをMVPとして重ね表示します。

## APKのダウンロード

GitHubにpushすると `.github/workflows/build-apk.yml` がdebug APKをビルドします。

1. GitHubリポジトリの `Actions` タブを開く
2. `Build APK` の最新成功Runを開く
3. `Artifacts` から `PersonalWeather-debug-apk` をダウンロード
4. ZIPを展開し、`app-debug.apk` をAndroidスマホに転送してインストール

debug APKなので、端末側で提供元不明アプリのインストール許可が必要です。

## セットアップ

- Android Studio Meerkat以降
- JDK 17
- Android SDK Platform 36
- minSdk 26
- targetSdk 36

ローカルでビルドする場合:

```bash
gradle assembleDebug
```

## 実装内容

- Open-Meteo Forecast API
  - `current`: `temperature_2m`, `weather_code`, `precipitation`
  - `hourly`: `temperature_2m`, `precipitation_probability`, `weather_code`, `precipitation`
  - `daily`: `weather_code`, `temperature_2m_max`, `temperature_2m_min`, `precipitation_probability_max`
  - `forecast_days=7`
  - `timezone=Asia/Tokyo`
  - `models=jma_seamless` を優先し、失敗時はmodels指定なしへフォールバック
- DataStore PreferencesによるJSONキャッシュ
- WorkManagerによる30分間隔のバックグラウンド更新
- Jetpack Glanceホーム画面ウィジェット
- 気象庁 `targetTimes_N1.json` と雨雲レーダータイルの最新時刻表示

## 画面

- ホーム: 現在気温、天気、最高最低、降水量、次の雨予測
- 雨雲: 現在地点周辺の固定ズーム地図、最新雨雲タイル、更新ボタン
- 時間別: 今日と明日の1時間予報、気温折れ線、降水確率バー
- 週間: 7日分カード

## 位置情報

初回起動時に位置情報許可を求めます。許可されない場合は東京駅を使います。地点選択はMVPとして、東京、横浜、大阪、名古屋、福岡、札幌のプリセットと現在地を用意しています。

## 制限事項

- 雨雲レーダーの気象庁タイル仕様は公式の安定APIではないため、取得失敗時はエラー表示にフォールバックします。
- 地図ベースにはOpenStreetMapの公開タイルを使っています。個人利用MVP向けです。公開配布や高頻度利用ではタイル利用ポリシーに合わせた地図基盤へ差し替えてください。
- debug APKは開発用署名です。Play Store配布にはrelease署名と配布用設定が必要です。
