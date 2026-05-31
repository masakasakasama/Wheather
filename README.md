# 個人天気アプリ

Kotlin / Jetpack Compose / Material 3 / Jetpack Glanceで作る個人用Android天気アプリです。APIキー不要のOpen-Meteo Forecast APIを通常予報に使い、気象庁の雨雲レーダータイルはMVPとして表示します。

## APKダウンロード

GitHubにpushすると `.github/workflows/build-apk.yml` がdebug APKをビルドし、GitHub Releasesの `latest-debug` に `PersonalWeather-debug.apk` を上書き配置します。

スマホだけでインストールする手順:

1. Androidスマホで https://github.com/masakasakasama/Wheather/releases/tag/latest-debug を開く
2. `PersonalWeather-debug.apk` をタップしてダウンロード
3. ダウンロード完了後、APKを開いてインストール

CIでは固定debug署名キーとビルドごとに増える `versionCode` を使います。そのため、同じ署名のAPKを入れた後はアンインストールせず上書きアップデートできます。固定署名にする前のAPKを入れている場合だけ、初回のみアンインストールが必要になる可能性があります。

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
- Open-Meteo Geocoding APIによる世界都市検索
- DataStore PreferencesによるJSONキャッシュ
- 保存地点リスト、選択、追加、削除、上下並べ替え
- WorkManagerによる30分間隔のバックグラウンド更新
- Jetpack Glanceホーム画面ウィジェット
- 気象庁 `targetTimes_N1.json` と雨雲レーダータイルの最新時刻表示

## 画面

- ホーム: 現在気温、天気、最高/最低、降水量、雨予測、今後12時間、週間天気を集約
- 地点: 保存地点をリスト表示し、上下並べ替え・削除・世界都市検索・現在地利用が可能
- 雨雲: 現在地周辺の固定ズーム地図に雨雲タイルを重ねて表示
- 時間: 今日と明日の1時間ごとの気温・降水確率。グラフ上に時刻と気温ラベルを表示
- 週間: 7日分のカード。カードを押すと詳細表示

## 制限事項

- 雨雲レーダーは気象庁タイル仕様変更に弱いMVPです。取得失敗時はエラー表示にフォールバックします。
- 地図ベースにはOpenStreetMapの公開タイルを直接利用しています。公開配布や高頻度利用では利用ポリシーに合わせた地図基盤へ差し替えてください。
- debug APKは開発用署名です。Play Store配布にはrelease署名と配布設定が必要です。
- ローカル開発環境にJava / Gradle / Android SDKがない場合、ビルド確認はGitHub Actionsで行います。
