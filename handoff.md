# Handoff

## 現在の状態

Android向け個人天気アプリMVPを `masakasakasama/Wheather` に格納済み。

- Repository: https://github.com/masakasakasama/Wheather
- APK release: https://github.com/masakasakasama/Wheather/releases/tag/latest-debug
- APK asset: `PersonalWeather-debug.apk`
- Update metadata: `version.json`
- Build workflow: `.github/workflows/build-apk.yml`

GitHub Actionsでdebug APKをビルドし、`latest-debug` Releaseへ上書きアップロードする構成。固定debug署名キーと増加する `versionCode` をCIで設定しているため、同じ署名のAPK導入後はアンインストールせず上書き更新できる。アプリ起動時に `version.json` を確認し、新しいversionCodeがあれば更新ダイアログを表示する。

## これまでの作業内容

- Androidプロジェクトを新規作成。
- Kotlin / Jetpack Compose / Material 3構成を追加。
- Jetpack Glanceのホーム画面ウィジェットを追加。
- DataStore Preferencesによる天気データ・地点・保存地点キャッシュを追加。
- WorkManagerによる30分間隔バックグラウンド更新を追加。
- Open-Meteo Forecast APIクライアントを追加。
  - `models=jma_seamless` を優先。
  - 失敗時はmodels指定なしのbest matchへフォールバック。
  - JMA Seamlessで降水確率が `null` の場合、models指定なしのbest matchから降水確率と降水量を補完。
  - current / hourly / daily の指定項目を取得。
- Open-Meteo Geocoding APIで世界都市検索を追加。
- 気象庁雨雲レーダーMVPを追加。
  - `targetTimes_N1.json` から最新時刻を取得。
  - 最新時刻の雨雲タイルURLを組み立て。
  - 現在地点周辺の固定ズーム3x3タイルを表示。
  - 取得失敗時はエラー表示。
- 気象庁重要情報MVPを追加。
  - 府県予報区の警報・注意報JSONを取得。
  - 現在地点に近い府県予報区をアプリ内座標リストから推定。
  - 発表中の警報・注意報名とヘッドラインをホームに表示。
  - 台風 `targetTc.json` を取得し、発生中の台風をホームに表示。
- 位置情報MVPを追加。
  - 初回起動時に位置情報許可を要求。
  - 許可なしの場合は東京駅へフォールバック。
- 画面を追加。
  - ホーム
  - 雨雲レーダー
  - 時間別天気
  - 週間天気
- ホームを拡充。
  - 現在天気、雨予測、今後48時間、2週間カードを集約。
  - 日別の最大降水確率と予想降水量を表示。
  - 48時間グラフにAM/PM時刻と気温を直接表示。
  - 2週間予報にAM / PMの概況を表示。
  - 週間カードをタップすると詳細表示。
- 地点管理を改善。
  - 保存地点リスト表示。
  - 世界都市検索。
  - プリセット追加。
  - 上下並べ替え。
  - 削除。
  - 現在地利用。
- 起動時アップデート確認を追加。
  - GitHub Releaseの `version.json` を取得。
  - 現在の `BuildConfig.VERSION_CODE` より新しければ更新ダイアログ表示。
  - `更新する` でAPKをダウンロードし、Androidのインストール画面を開く。
- READMEを追加・更新。
  - セットアップ方法。
  - APKスマホ直接インストール方法。
  - API仕様。
  - 制限事項。
- GitHub Actionsを追加。
  - debug APKビルド。
  - Release `latest-debug` へのAPK配置。
  - 固定debug署名キー生成。
  - `VERSION_CODE` / `VERSION_NAME` 注入。

## 変更ファイル

### プロジェクト・ビルド

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `.gitignore`
- `.github/workflows/build-apk.yml`
- `app/src/main/res/xml/file_paths.xml`

### Android設定・リソース

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
- `app/src/main/java/com/example/weather/data/api/AppUpdateClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaRadarClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaDisasterClient.kt`
- `app/src/main/java/com/example/weather/data/cache/WeatherCache.kt`
- `app/src/main/java/com/example/weather/data/model/WeatherModels.kt`
- `app/src/main/java/com/example/weather/data/model/AppUpdateModels.kt`
- `app/src/main/java/com/example/weather/data/model/DisasterModels.kt`
- `app/src/main/java/com/example/weather/data/repository/WeatherRepository.kt`

### location / worker / widget

- `app/src/main/java/com/example/weather/location/LocationProvider.kt`
- `app/src/main/java/com/example/weather/worker/WeatherRefreshWorker.kt`
- `app/src/main/java/com/example/weather/widget/WeatherWidget.kt`
- `app/src/main/java/com/example/weather/update/AppUpdateInstaller.kt`

### UI

- `app/src/main/java/com/example/weather/ui/HomeScreen.kt`
- `app/src/main/java/com/example/weather/ui/RadarScreen.kt`
- `app/src/main/java/com/example/weather/ui/HourlyScreen.kt`
- `app/src/main/java/com/example/weather/ui/WeeklyScreen.kt`

### ドキュメント

- `README.md`
- `handoff.md`

## 未解決課題

- ローカル環境にJava / Gradle / Android SDKがない場合、ローカルビルド確認は不可。GitHub Actionsで確認する。
- 実機での表示確認は未実施。
- 雨雲レーダーはMVP。
  - 気象庁タイル仕様変更に弱い。
  - パン/ズームは最低限。
  - 地図タイル利用ポリシーは公開配布前に再確認が必要。
- 重要気象情報はMVP。
  - 警報・注意報の地点判定は府県予報区レベルの近似。
  - 台風は一覧表示のみで、進路図・暴風域確率・詳細諸元は未実装。
- 天気アイコンは文字ベース。統一感のあるベクターアイコンは未実装。
- Widgetのサイズ別レイアウトはMVP。実機ホーム画面での詰め調整が必要。
- 通信失敗時のUXは最低限。状態別のより細かい表示は未実装。
- Play Store向けrelease署名APKは未作成。
- Play Store外APKのため、完全サイレント更新は不可。Androidのインストール確認画面は必ず表示される。

## 次にやること

1. GitHub Actionsのビルド結果を確認する。
2. Androidスマホで `latest-debug` ReleaseからAPKを直接インストールして動作確認する。
3. 実機で以下を確認する。
   - 初回位置情報許可。
   - 許可なし時の東京駅フォールバック。
   - Open-Meteo更新。
   - ホームの12時間グラフの時刻・気温ラベル。
   - 地点検索、保存、並べ替え、削除。
   - 起動時アップデート確認。
   - 更新ダイアログからAPKインストール画面が開くこと。
   - 週間カード詳細。
   - ウィジェット追加と更新。
   - 雨雲レーダー取得失敗時の表示。
4. 実機で崩れた箇所をlogcatとスクリーンショットで修正する。
5. 雨雲レーダーのパン/ズームを改善する。
6. 必要ならrelease署名APKとGitHub Releases配布に切り替える。
