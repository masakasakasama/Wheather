# Handoff

## 現在の状態

Android向け個人天気アプリMVPを `masakasakasama/Wheather` に格納済み。

- Repository: https://github.com/masakasakasama/Wheather
- APK release: https://github.com/masakasakasama/Wheather/releases/tag/latest-debug
- APK asset: `PersonalWeather-debug.apk`
- Direct APK URL: https://github.com/masakasakasama/Wheather/releases/download/latest-debug/PersonalWeather-debug.apk
- Update metadata: `version.json`
- Build workflow: `.github/workflows/build-apk.yml`

GitHub Actionsでdebug APKをビルドし、`latest-debug` Releaseへ上書きアップロードする構成。GitHub Actions cacheに保存した固定debug署名キーと、Release assetにバックアップした `PersonalWeather-debug-signing-key.keystore`、増加する `versionCode` をCIで設定しているため、同じ署名のAPK導入後はアンインストールせず上書き更新できる。固定署名にする前のAPKからは初回だけアンインストールが必要になる可能性がある。アプリ起動時に `version.json` を確認し、新しいversionCodeがあれば更新ダイアログを表示する。Android 8以降でアプリ自身からAPKを開く場合は「この提供元のアプリを許可」が必要なため、許可OFF時は該当設定画面へ誘導する。更新ダイアログには `ブラウザで開く` もあり、アプリ内インストール経路が詰まってもAPK URLへ直接逃げられる。

## これまでの作業内容

- Androidプロジェクトを新規作成。
- Kotlin / Jetpack Compose / Material 3構成を追加。
- Jetpack Glanceのホーム画面ウィジェットを追加。
- ウィジェットの時間別表示も現在時刻以降のデータを使い、日別と同じ降水確率補完で0%表示の矛盾を抑制。
- DataStore Preferencesによる天気データ・地点・保存地点キャッシュを追加。
- 2時間以上古いキャッシュはホームで「古いデータ」と明示。
- WorkManagerによる30分間隔バックグラウンド更新を追加。
- バックグラウンド更新時の通知を追加。
  - 今後3時間以内の降水確率60%以上、または予想降水量0.2mm以上で雨通知。
  - 気象庁の警報・注意報、台風情報がある場合は重要気象情報通知。
  - SharedPreferencesで同一内容の重複通知を抑制。
  - DataStoreに通知設定を保存し、ホームの設定から雨通知ON/OFF、判定時間、降水確率、雨量、重要気象情報通知ON/OFFを変更可能。
- Open-Meteo Forecast APIクライアントを追加。
  - `models=jma_seamless` を優先。
  - 失敗時はmodels指定なしのbest matchへフォールバック。
  - JMA Seamlessで降水確率が `null` の場合、models指定なしのbest matchから降水確率と降水量を補完。
  - current / minutely_15 / hourly / daily の指定項目を取得。
  - `minutely_15` で直近3時間の15分ごとの降水確率、降水量、天気、気温を取得。JMAで降水確率が欠損する場合はbest matchで補完。
  - 体感温度、湿度、風速、風向、気圧、UV、日の出/日の入を取得。
  - 通信失敗時は1回リトライ。
- Open-Meteo Geocoding APIで世界都市検索を追加。
- Open-Meteo Air Quality APIで空気質を追加。
  - European AQI / US AQI / PM2.5 / PM10 / 二酸化窒素 / オゾンを取得。
  - 天気更新成功後に取得し、空気質だけ失敗しても天気更新は成功扱い。
- 気象庁雨雲レーダーMVPを追加。
  - `targetTimes_N1.json` から最新時刻を取得。
  - 最新時刻の雨雲タイルURLを組み立て。
  - 現在地点周辺の3x3タイルを表示。
  - ボタン式のズーム、上下左右移動、現在地へ戻る操作を追加。
  - 取得失敗時はエラー表示。
- 気象庁重要情報MVPを追加。
  - 府県予報区の警報・注意報JSONを取得。
  - 現在地点に近い府県予報区をアプリ内座標リストから推定。
  - 発表中の警報・注意報名とヘッドラインをホームに表示。
  - 台風 `targetTc.json` を取得し、発生中の台風をホームに表示。
- 位置情報MVPを追加。
  - 初回起動時に位置情報許可を要求。
  - Android 13以降は通知許可も要求。
  - 許可なしの場合は東京駅へフォールバック。
- 画面を追加。
  - ホーム
  - 雨雲レーダー
  - 時間別天気
  - 週間天気
- ホームを拡充。
  - ヘッダーを地点名、更新時刻、更新、地点、設定のまとまりに整理。
  - 現在天気カードを大きな気温、天気名、現在降水量、4つの主要指標タイルで構成するヒーロー表示へ刷新。
  - 現在天気、雨予測、今後48時間、2週間カードを集約。
  - 日別の最大降水確率と予想降水量を表示。daily値とhourly値が食い違う場合は、表示上は大きい値を採用して0%表示の矛盾を避ける。
  - 体感、湿度、風、気圧、UV、日の出/日の入を表示。
  - 今日の判断カードを追加し、傘、洗濯、服装、外出注意を降水確率・降水量・湿度・風・UV・AQIから自動判定。
  - 空気質カードでAQI、PM2.5、PM10、オゾン、24時間以内の最大AQIを表示。
  - 直近3時間カードで15分ごとの雨予報を表示。
  - 48時間カードに日付つきAM/PM時刻、気温、降水確率、降水量、降水バーを同じ時刻単位で表示。
  - 48時間グラフとカード列を同じ横スクロールに統合し、スクロール位置のズレを解消。
  - ホームの主要カード色を低彩度の濃色に寄せ、雨があるカードだけ青系アクセントに変更。
  - 週間カードを日付、天気名、最高/最低、降水、AM/PMチップが読みやすい行構成へ刷新。
  - 降水確率と雨量を「雨具必要」「大雨警戒」「災害級の大雨」などの判断ラベルに変換し、週間/AMPM/時間カードに表示。
  - 通知設定ダイアログを表示。
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
  - Android 8以降で「この提供元のアプリを許可」がOFFの場合は、アプリの不明なアプリ許可設定を開いて再試行できるようにした。
  - 更新ダイアログの `ブラウザで開く` から直接APK URLを開けるようにした。
- READMEを追加・更新。
  - セットアップ方法。
  - APKスマホ直接インストール方法。
  - API仕様。
  - 制限事項。
- GitHub Actionsを追加。
  - debug APKビルド。
  - Release `latest-debug` へのAPK配置。
  - 固定debug署名キー生成。
  - debug署名キーをGitHub Actions cacheとRelease asset `PersonalWeather-debug-signing-key.keystore` で再利用。
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
- `app/src/main/java/com/example/weather/data/api/AirQualityClient.kt`
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
- `app/src/main/java/com/example/weather/notification/WeatherNotificationCenter.kt`
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
  - パン/ズームはボタン式で最低限。ピンチズーム、慣性スクロール、スムーズな地図操作は未実装。
  - 地図タイル利用ポリシーは公開配布前に再確認が必要。
- 重要気象情報はMVP。
  - 警報・注意報の地点判定は府県予報区レベルの近似。
  - 台風は一覧表示のみで、進路図・暴風域確率・詳細諸元は未実装。
- 天気アイコンは文字ベース。統一感のあるベクターアイコンは未実装。
- Widgetのサイズ別レイアウトはMVP。実機ホーム画面での詰め調整が必要。
- 通知はAndroid 13以降でユーザーが通知許可を拒否すると動作しない。
- 空気質はOpen-Meteo経由のCAMS系モデル値。観測局の実測値そのものではない。
- 通信失敗時のUXは最低限。状態別のより細かい表示は未実装。
- Play Store向けrelease署名APKは未作成。
- Play Store外APKのため、完全サイレント更新は不可。Androidのインストール確認画面は必ず表示される。
- debug署名キーは上書き更新を安定させるためRelease assetにもバックアップしている。長期運用ではrelease署名キーをGitHub Secretsで管理する方が安全。

## 次にやること

1. GitHub Actionsのビルド結果を確認する。
2. Androidスマホで `latest-debug` ReleaseからAPKを直接インストールして動作確認する。
3. 実機で以下を確認する。
   - 初回位置情報許可。
   - Android 13以降の通知許可。
   - 許可なし時の東京駅フォールバック。
   - Open-Meteo更新。
   - ホームの48時間カードの時刻・降水バー・降水確率・降水量が一致すること。
   - 今日の判断カードの傘・洗濯・服装・外出注意が、降水確率・雨量・湿度・UV・AQIに応じて自然に変わること。
   - 直近3時間カードの15分ごとの時刻、降水確率、降水量が取得データと一致すること。
   - 時間画面のグラフとカードの時刻が横スクロール位置で一致すること。
   - 地点検索、保存、並べ替え、削除。
   - 起動時アップデート確認。
   - 更新ダイアログからAPKインストール画面が開くこと。
   - 週間カード詳細。
   - ウィジェット追加と更新。
   - バックグラウンド更新時の雨通知・重要気象情報通知。
   - 通知設定の保存と、設定値が通知判定に反映されること。
   - 雨雲レーダー取得失敗時の表示。
   - 雨雲レーダーのズーム、上下左右移動、現在地へ戻る操作。
4. 実機で崩れた箇所をlogcatとスクリーンショットで修正する。
5. 雨雲レーダーのピンチズームとドラッグ移動を改善する。
6. 必要ならrelease署名APKとGitHub Releases配布に切り替える。
