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

### 世界の地点検索

- 地点ダイアログから日本語または現地語・英語の都市名を入力し、検索ボタンまたはキーボードの検索キーでOpen-Meteo Geocoding APIを検索します。
- 検索結果は保存地点より上に表示し、選択すると保存地点へ追加してその地点の予報を開きます。
- 入力中の古いリクエストはキャンセルし、後から返った古い結果で現在の検索結果を上書きしません。
- 0件と通信エラーを区別して表示します。例: `Heidelberg`、`Berlin`、`München`。
- GPSの座標が更新されても「現在地」は保存リスト内の1件を最新座標へ置き換えます。旧バージョンで保存された複数の「現在地」は読み込み時に自動整理されます。
- アプリで選択中の地点を全ウィジェットの設定地点として使用します。地点変更直後は旧地点のキャッシュを表示せず、取得中表示を経て新地点の予報へ切り替えます。
- 起動時、手動更新、地点削除後、WorkManager更新でも選択地点と予報地点の一致を確認します。
- ホーム上部の現在地ボタンから、保存地点リストを開かずに現在地へ戻れます。位置情報未許可の場合は権限確認を表示します。
- 日本国内の「現在降っているか」は気象庁高解像度降水ナウキャストの最新タイルを優先します。地点周辺約400mの8段階の降水強度を解析し、観測時刻と`mm/h以上`を表示します。
- 気象庁レーダーが取得できない場合や日本国外ではOpen-Meteoへフォールバックします。レーダーの非降水エコーや観測休止により実際と異なる場合があります。

- Open-Meteo Forecast API
  - `current`: `temperature_2m`, `apparent_temperature`, `relative_humidity_2m`, `weather_code`, `precipitation`, `wind_speed_10m`, `wind_direction_10m`, `pressure_msl`
  - `minutely_15`: `temperature_2m`, `precipitation_probability`, `weather_code`, `precipitation`
  - `hourly`: `temperature_2m`, `precipitation_probability`, `weather_code`, `precipitation`
  - `daily`: `weather_code`, `temperature_2m_max`, `temperature_2m_min`, `precipitation_probability_max`, `precipitation_sum`, `uv_index_max`, `sunrise`, `sunset`
  - `forecast_days=14`
  - `timezone=auto` を使い、取得地点の現地タイムゾーンをキャッシュして時刻判定・表示・通知・ウィジェットで共有
  - models指定なしのOpen-Meteo Best Matchを優先し、取得失敗時だけ `models=jma_seamless` へフォールバック
  - 予報元はキャッシュにも保存し、ホーム下部に表示
- Open-Meteo `minutely_15` による短時間データ取得
  - ホームには独立した「直近3時間」カードを表示せず、通知判定など内部処理に利用
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
  - 可変サイズ版に加え、ランチャーで明示的に選択できる専用2×2版を提供
  - 2×2版は地点名、今日/明日の大きい天気アイコン、天気名、最高/最低、降水確率、最終更新時刻を2列比較する構成
- 降水表示の共通判定
  - 0.1mm以上の予想雨量だけを「雨開始」とし、確率だけ高く雨量0.0mmの場合は雨開始と断定しない
  - 15分予報の範囲内は高解像度側を優先し、重複する1時間予報で開始時刻を上書きしない
  - ホーム、次の雨、今日の判断、通知、ウィジェットで同じ雨開始判定を使用
- 今日の判断カード
  - 傘、洗濯、服装、外出注意を降水確率・降水量・湿度・風・UV・AQIから自動判定
- 気象庁 `targetTimes_N1.json` と雨雲レーダータイルの最新時刻表示
- 気象庁の警報・注意報JSONと台風 `targetTc.json` による重要気象情報表示

## 画面

- ホーム: Yahoo!天気を参考にした高密度レイアウト。上部に表示中地点を明示したコンパクト地点タブ、今日/明日の2列比較、同一横スクロール内に揃えた時刻・天気・気温・降水確率・雨量・湿度・風、日の出/日の入、現在値、現在地の雨雲レーダープレビュー、2週間一覧を集約。重要気象情報を押すと地点・警報内容を含むGoogle検索を開く。土曜の曜日は青、日曜は赤で表示。黒と濃紺を基調に、最高気温は赤、最低気温は青、降水は水色で統一。天気アイコンはグレーの雲を基調に、晴れはオレンジ、雨は青、雷は黄を重ねた共通ベクター表示。週間行を押すとその日の1時間ごとの予報を表示
- 地点: 保存地点をリスト表示し、上下並べ替え・削除・世界都市検索・現在地利用が可能
- 雨雲: 現在地周辺の地図に気象庁雨雲タイルを重ねて表示。ズーム、上下左右移動、現在地へ戻る操作に対応
- 時間: 現在時刻以降、APIから取得できた最終時刻までの1時間ごとの天気、気温、降水確率、降水量、湿度、風向・風速を、日付と時刻付きの単一横スクロール表で表示
- 週間: 14日分を高密度リスト表示。今日/明日の概要、日付、天気、最高/最低、降水確率、雨量、AM/PMの概況、押下時の詳細表示

## 予報モデルの実測比較

2026-06-15から2026-07-26までの東京（気象庁観測点47662）の実測41日分と、Open-Meteo Previous Runs APIの過去予報を比較した。最高気温の平均絶対誤差（MAE）は次の通り。

| モデル | 1日前 | 3日前 | 5日前 | 7日前 |
| --- | ---: | ---: | ---: | ---: |
| Best Match | 1.46℃ | 1.95℃ | 2.02℃ | 2.74℃ |
| JMA Seamless | 1.46℃ | 3.77℃ | 4.13℃ | 3.84℃ |
| ECMWF IFS 0.25° | 1.54℃ | 1.82℃ | 2.09℃ | 2.97℃ |
| GFS Seamless | 2.14℃ | 2.28℃ | 2.94℃ | 3.25℃ |

JMA Seamlessは3日後と5日後の最高気温に約-3.7℃の低温バイアスが見られた。短期から2週間まで一貫して使う現在のアプリ構成では、全体の安定性と世界地点対応を優先してBest Matchを通常予報に採用し、JMA Seamlessは通信・取得失敗時の予備とした。この比較は東京の一期間だけなので、季節や地点が変われば結果も変わり得る。

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
