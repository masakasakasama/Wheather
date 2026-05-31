# Handoff

## 現在の状態

Android向け個人天気アプリのMVPを `masakasakasama/Wheather` に格納済み。

- Repository: https://github.com/masakasakasama/Wheather
- APK build success commit: `9e8bab6`
- APK build workflow: `.github/workflows/build-apk.yml`
- Successful APK build run: https://github.com/masakasakasama/Wheather/actions/runs/26703172042
- Artifact: `PersonalWeather-debug-apk`
- Direct phone install release: https://github.com/masakasakasama/Wheather/releases/tag/latest-debug
- Direct APK asset name: `PersonalWeather-debug.apk`
- Artifact size: 約 12.1 MB

GitHub Actionsで `assembleDebug` は成功し、スマホだけで直接ダウンロードできるdebug APK releaseを生成する構成に変更済み。

## これまでの作業内容

- 空ディレクトリからAndroidプロジェクトを新規作成。
- Kotlin / Jetpack Compose / Material 3構成を追加。
- Jetpack Glanceのホーム画面ウィジェットを追加。
- DataStore Preferencesによる天気データキャッシュを追加。
- WorkManagerによる30分間隔のバックグラウンド更新を追加。
- Open-Meteo Forecast APIクライアントを追加。
  - `models=jma_seamless` を優先。
  - 失敗時はmodels指定なしのbest matchへフォールバック。
  - current / hourly / daily の指定項目を取得。
- 気象庁雨雲レーダーMVPを追加。
  - `targetTimes_N1.json` から最新時刻を取得。
  - 最新時刻の雨雲タイルURLを組み立て。
  - 現在地点周辺の固定ズームで3x3タイルを表示。
  - 取得失敗時はエラー表示。
- 位置情報MVPを追加。
  - 初回起動時に位置情報許可を要求。
  - 許可なしの場合は東京駅。
  - プリセット地点: 東京、横浜、大阪、名古屋、福岡、札幌。
- 画面MVPを追加。
  - ホーム
  - 雨雲レーダー
  - 時間別天気
  - 週間天気
- READMEを追加。
  - セットアップ方法
  - APKダウンロード方法
  - API仕様
  - 制限事項
- GitHub Actionsでdebug APKをビルドしてArtifactにアップロードする設定を追加。
- GitHub Releasesの `latest-debug` に `PersonalWeather-debug.apk` を上書き配置する設定を追加。
- CI失敗を修正。
  - `gradle.properties` に `android.useAndroidX=true` を追加。
  - Kotlinのsuspend呼び出しをデフォルト引数から除去。

## 変更ファイル

### プロジェクト/ビルド

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `.gitignore`
- `.github/workflows/build-apk.yml`

### Android設定/リソース

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/drawable/ic_launcher.xml`
- `app/src/main/res/layout/weather_widget_loading.xml`
- `app/src/main/res/xml/weather_widget_info.xml`

### アプリ基盤

- `app/src/main/java/com/example/weather/WeatherApplication.kt`
- `app/src/main/java/com/example/weather/AppServices.kt`
- `app/src/main/java/com/example/weather/MainActivity.kt`

### data

- `app/src/main/java/com/example/weather/data/api/OpenMeteoClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaRadarClient.kt`
- `app/src/main/java/com/example/weather/data/cache/WeatherCache.kt`
- `app/src/main/java/com/example/weather/data/model/WeatherModels.kt`
- `app/src/main/java/com/example/weather/data/repository/WeatherRepository.kt`

### location / worker / widget

- `app/src/main/java/com/example/weather/location/LocationProvider.kt`
- `app/src/main/java/com/example/weather/worker/WeatherRefreshWorker.kt`
- `app/src/main/java/com/example/weather/widget/WeatherWidget.kt`

### UI

- `app/src/main/java/com/example/weather/ui/HomeScreen.kt`
- `app/src/main/java/com/example/weather/ui/RadarScreen.kt`
- `app/src/main/java/com/example/weather/ui/HourlyScreen.kt`
- `app/src/main/java/com/example/weather/ui/WeeklyScreen.kt`

### ドキュメント

- `README.md`
- `handoff.md`

## 未解決課題

- ローカル環境にはJava / Gradle / Android SDKが無かったため、ローカルでのビルド・実機確認は未実施。
- GitHub Actionsのdebug APKビルドは成功しているが、実機インストール後の動作確認は未実施。
- 雨雲レーダーはMVP。
  - 気象庁タイル仕様変更に弱い。
  - 地図はOpenStreetMap公開タイルを直接利用しているため、公開配布や高頻度利用には向かない。
  - パン/ズームは最低限で、UIとしては未成熟。
- 手動入力地点は未実装。
  - 現状はプリセット地点と現在地のみ。
- 天気アイコンは絵文字/短い文字ベース。
  - 統一感のあるベクターアイコンには未置換。
- Widgetのサイズ別UIはMVP。
  - Glance上での実機レイアウト確認が必要。
- キャッシュの「古い」判定表示は簡易。
  - 現状は前回更新時刻表示が中心。
- 通信失敗時のUXは最低限。
  - 最後に成功したキャッシュ表示はあるが、状態表現の磨き込みが必要。
- 位置情報は `LocationManager` のlast known location。
  - Google Play ServicesのFused Location Providerは未使用。
- Release署名APKは未作成。
  - 現在はdebug署名APKのみ。

## 次にやること

1. Androidスマホで https://github.com/masakasakasama/Wheather/releases/tag/latest-debug を開き、`PersonalWeather-debug.apk` を直接ダウンロードしてインストールする。
2. 実機で以下を確認する。
   - 初回位置情報許可
   - 許可なし時の東京駅フォールバック
   - Open-Meteo更新
   - ホーム画面表示
   - 時間別/週間画面表示
   - ウィジェット追加と表示
   - WorkManager更新後のウィジェット反映
   - 雨雲レーダー取得失敗時の表示
3. 実機で落ちた箇所をLogcatで確認して修正する。
4. 地点手動入力を追加する。
5. 雨雲レーダーを改善する。
   - タイル取得失敗時の部分表示
   - パン/ズーム
   - 地図タイル利用方針の見直し
6. ウィジェットの実機レイアウトを調整する。
7. 必要ならrelease署名とGitHub Releases配布に切り替える。

## 続きから進めるときの開始点

まず `handoff.md` を読み、GitHub Releasesの `latest-debug` からAPKをスマホだけで直接インストールして動作確認する。実機で確認できた問題を優先して直す。雨雲レーダー改善より前に、通常予報・キャッシュ・ウィジェットの実機安定性を優先する。
