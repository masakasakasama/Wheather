# 個人天気アプリ

Kotlin / Jetpack Compose / Material 3 / Jetpack Glanceで作る個人用Android天気アプリです。APIキー不要のOpen-Meteo Forecast APIを通常予報に使い、気象庁の雨雲レーダータイルはMVPとして表示します。

## APKダウンロード

GitHubにpushすると `.github/workflows/build-apk.yml` がdebug APKをビルドし、GitHub Releasesの `latest-debug` に `PersonalWeather-debug.apk` を上書き配置します。

スマホだけでインストールする手順:

1. Androidスマホで https://github.com/masakasakasama/Wheather/releases/tag/latest-debug を開く
2. `PersonalWeather-debug.apk` をタップしてダウンロード
3. ダウンロード完了後、APKを開いてインストール

直接APK URL: https://github.com/masakasakasama/Wheather/releases/download/latest-debug/PersonalWeather-debug.apk

CIではGitHub Actionsのcacheに保存した固定debug署名キーと、Release assetにバックアップした `PersonalWeather-debug-signing-key.keystore`、ビルドごとに増える `versionCode` を使います。そのため、同じ署名のAPKを入れた後はアンインストールせず上書きアップデートできます。固定署名にする前のAPKを入れている場合だけ、初回のみアンインストールが必要になる可能性があります。

アプリ起動時にはReleaseの `version.json` を確認し、インストール中の `versionCode` より新しいAPKがある場合は更新ダイアログを表示します。`更新する` を押すとAPKを取得し、Androidのインストール確認画面を開きます。アプリ自身からAPKを開く場合、Android 8以降では初回だけ「この提供元のアプリを許可」をオンにする必要があります。許可がOFFの場合は該当設定画面を開き、オンにした後に再度 `更新する` を押せるようにしています。インストーラが詰まる場合に備えて、更新ダイアログには `ブラウザで開く` も用意し、スマホだけで直接APK URLを開けるようにしています。Play Store外APKのため、ユーザー確認なしの完全自動インストールはAndroidの制限でできません。

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
  - `current`: `temperature_2m`, `apparent_temperature`, `relative_humidity_2m`, `weather_code`, `precipitation`, `wind_speed_10m`, `wind_direction_10m`, `pressure_msl`
  - `minutely_15`: `temperature_2m`, `precipitation_probability`, `weather_code`, `precipitation`
  - `hourly`: `temperature_2m`, `precipitation_probability`, `weather_code`, `precipitation`
  - `daily`: `weather_code`, `temperature_2m_max`, `temperature_2m_min`, `precipitation_probability_max`, `precipitation_sum`, `uv_index_max`, `sunrise`, `sunset`
  - `forecast_days=14`
  - `timezone=Asia/Tokyo`
  - `models=jma_seamless` を優先し、失敗時はmodels指定なしへフォールバック
  - JMA Seamlessで降水確率が欠損する場合は、models指定なしのbest matchから降水確率と降水量を補完
- 直近3時間の15分ごと雨予報
  - Open-Meteo `minutely_15` を使い、降水確率・降水量・天気を表示
  - 日本では一部の15分データが hourly からの補間になる可能性があるため、短時間の目安として扱う
- Open-Meteo Geocoding APIによる世界都市検索
- Open-Meteo Air Quality APIによる空気質表示
  - `current`: `european_aqi`, `us_aqi`, `pm10`, `pm2_5`, `nitrogen_dioxide`, `ozone`
  - `hourly`: `european_aqi`, `pm2_5`, `pm10`, `uv_index`
  - 天気更新は空気質APIの失敗に巻き込まない
- GitHub Release `version.json` による起動時アップデート確認
- DataStore PreferencesによるJSONキャッシュ
- 更新失敗時は最後に成功したキャッシュを表示し、2時間以上古い場合はホームに「古いデータ」と表示
- 保存地点リスト、選択、追加、削除、上下並べ替え
- WorkManagerによる30分間隔のバックグラウンド更新
- バックグラウンド更新時の通知
  - 今後3時間以内に降水確率60%以上、または予想降水量0.2mm以上なら雨通知
  - 気象庁の警報・注意報、台風情報がある場合は重要気象情報通知
  - 同じ内容の重複通知は抑制
  - ホームの設定から雨通知ON/OFF、判定時間、降水確率、雨量、重要気象情報通知ON/OFFを変更可能
- Jetpack Glanceホーム画面ウィジェット
  - 現在時刻以降の時間別データを使い、日別表示と同じ降水確率補完ロジックで0%表示の矛盾を避ける
- 今日の判断カード
  - 傘、洗濯、服装、外出注意を降水確率・降水量・湿度・風・UV・AQIから自動判定
- 気象庁 `targetTimes_N1.json` と雨雲レーダータイルの最新時刻表示
- 気象庁の警報・注意報JSONと台風 `targetTc.json` による重要気象情報表示

## 画面

- ホーム: 重要気象情報、現在気温、天気、最高/最低、降水確率、降水量、体感、湿度、風、気圧、UV、日の出/日の入、今日の判断、空気質、雨予測、直近3時間、今後48時間、2週間天気、通知設定を集約。黒背景に低彩度の濃色カード、雨がある時間だけ青系アクセント
- 地点: 保存地点をリスト表示し、上下並べ替え・削除・世界都市検索・現在地利用が可能
- 雨雲: 現在地周辺の地図に気象庁雨雲タイルを重ねて表示。ズーム、上下左右移動、現在地へ戻る操作に対応
- 時間: 現在時刻以降48時間の1時間ごとの気温・降水確率・降水量。ホームの48時間グラフとカード列は同じ横スクロールで同期し、時刻は日付つきAM/PM表記
- 週間: 14日分のカード。AM/PMの概況と、カード押下時の詳細表示

## 制限事項

- 雨雲レーダーは気象庁タイル仕様変更に弱いMVPです。取得失敗時はエラー表示にフォールバックします。
- 雨雲レーダーの地図操作はボタン式の簡易パン/ズームです。慣性スクロールやピンチズームは未実装です。
- 15分ごとの短時間雨予報はOpen-Meteoの `minutely_15` を利用します。日本では高解像度モデルの直接値ではなく補間値になる場合があります。
- 警報・注意報は現在地点に近い府県予報区をアプリ内の座標リストから推定します。市区町村単位の完全一致ではありません。
- 台風情報は全国で発表中の台風を表示します。進路図や詳細諸元は未実装です。
- 空気質はOpen-Meteo経由のCAMS系データです。観測局の実測値そのものではなく、予測モデル由来の値として扱います。
- 地図ベースにはOpenStreetMapの公開タイルを直接利用しています。公開配布や高頻度利用では利用ポリシーに合わせた地図基盤へ差し替えてください。
- debug APKは開発用署名です。Play Store配布にはrelease署名と配布設定が必要です。
- 上書き更新を安定させるためdebug署名キーをRelease assetにバックアップしています。個人利用向けの簡易配布前提で、第三者向け配布ではrelease署名キーをGitHub Secrets等で非公開管理してください。
- 通知はAndroid 13以降で通知許可が必要です。許可されない場合、アプリ内表示とウィジェット更新のみ動作します。
- ローカル開発環境にJava / Gradle / Android SDKがない場合、ビルド確認はGitHub Actionsで行います。
