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

- 2026-08-03 海外地点へ日本の重要気象情報が表示される問題を修正。
  - 原因は、地点の国を判定せず最寄りの気象庁管区を選び、全国の台風一覧も無条件取得していたこと。シドニーでは沖縄の注意報と日本周辺の台風が混在していた。
  - Open-Meteo Geocoding APIの`country_code`を地点へ保存し、気象庁の警報・台風、アメダス実況、雨雲レーダーの取得を日本の地点だけに制限。旧保存データとGPS現在地は日本列島の座標範囲で互換判定する。
  - 重要気象情報へ取得地点キーを持たせ、地点変更中に前地点の警報が一時表示される競合を遮断。海外のホームでは気象庁レーダー状態とプレビューを非表示にし、雨雲タブでは日本国内限定と明示する。
  - シドニー、釜山、日本の離島、国コード引継ぎ、地点不一致、JMA各クライアントの海外通信抑止を回帰テストへ追加。Pixel API 34でシドニー検索後に日本の警報が表示されないことと、海外用雨雲画面を確認。
- 2026-08-02 前日比、時間別気温幅、台風・熱帯低気圧の表示を修正。
  - 今日・明日のカードから冗長な`最高・最低とも前日比`を削除。前日比の`[+2]`、`[0]`、`[-1]`は各気温の横だけに表示。
  - 時間表の主気温とモデル予報幅を固定高の別行にし、予報幅の有無にかかわらず全列の高さと基準位置を統一。
  - モデル下限・上限を画面表示と同じ整数へ丸めてから比較し、両方が同じ場合は`28〜28°`のような重複幅を非表示に変更。
  - 気象庁`targetTc.json`の未採番熱帯低気圧が`typhoonNumber: "b"`で返ることを実APIで確認。英字識別子を台風番号として扱わず、`熱帯低気圧`と表示。
  - 数字の`2613`は`台風第13号`へ正規化。`TD`、`TS`、`STS`、`TY`など内部コードを利用者向け強度表現に誤変換せず、画面、通知、Google検索を共通整形関数へ統一。
  - 同値気温幅、逆転幅、未採番`b`、数字台風番号、既知・未知カテゴリを回帰テストへ追加。Android LintとAPKビルド、エミュレータ表示を確認。
- 2026-08-01 現在気温と気温予報の取得・評価経路を全面リファクタリング。
  - 現在気温は予報モデル値ではなく、気象庁アメダスの最新観測を優先。30km以内、品質正常、観測後20分以内の最寄り観測所だけを実況として採用。
  - 実況の観測所名、観測時刻、距離をホームへ表示。20分超は`前回実況`、海外・取得失敗時は`モデル推定`と明示。
  - 気温予報はJMA Seamless、ECMWF IFS 0.25°、GFS Seamlessを別取得し、外れ値除外と誤差重み付き統合を実装。天気コード・降水などはOpen-Meteo Best Matchを維持。
  - アメダス実況との差による直近18時間の減衰補正を追加。各モデルの予報と後日の実況を端末内DataStoreで比較し、地点・リード時間別のMAEとバイアスを3標本以上から重みへ反映。
  - 統合済み時間別気温から日次最高・最低を再集計し、ホーム、時間表、2週間、ウィジェットの気温経路を同じキャッシュへ統一。モデル間の予報幅も表示。
  - アプリ表示中は10分ごとに再取得し、バックグラウンドへ移ると停止。起動時、手動更新、30分WorkManager更新は維持。
  - 2×2を含む全ウィジェットへ実況時刻、前回実況、または推定モデル数を表示。
  - 気象庁レーダーの`validtime`をUTCとして共通解析し、日本時間表示と鮮度計算の基準を統一。ホームのレーダープレビューもスナップショット更新に追従。
  - 東京の実APIでアメダス観測、JMA/ECMWF/GFS、レーダー時刻を確認。ドイツ地点でも現地タイムゾーンと複数モデル取得を確認。
  - 41件の単体テスト、Android Lintエラー0、APKビルド、エミュレータへのアンインストールなし上書きインストール、画面表示、レーダー日本時刻を確認。
  - Pixelランチャーへ専用2×2ウィジェットを実際に追加し、今日/明日、天気、最高最低、降水確率、実況時刻、更新時刻が切れずに収まることを確認。
- 2026-07-30 豪雨中に晴れ表示となる現在天気の経路を修正。
  - 台東区座標の気象庁最新タイルを直接検査し、`80mm/h以上`の配色が取得できる一方、表示中の`CurrentConditionsPanel`と時間表がOpen-Meteoの天気コードを直接使っていたことを確認。
  - 最新レーダー降雨を優先する現在天気コード・ラベルをデータ層へ一本化。ホーム上部、いまの天気、現在時刻の時間表、時間タブ、日別詳細、ウィジェットで共通利用。
  - 降雨中はホーム先頭へ観測警告を表示し、現在時刻のアイコンを雨、降水欄を「観測」、雨量欄を`mm/h以上`へ変更。
  - レーダー取得失敗、期限切れ、対象地域外を降雨なしと区別し、予報値表示中であることを明記。
  - 豪雨レーダーが晴れ予報を上書きすること、失敗時に降雨なしと表示しないこと、期限切れで予報へ戻ること、0.1mm/h表示を回帰テストへ追加。
- 2026-07-29 今日・明日の前日比と2週間一覧の情報密度を変更。
  - Open-Meteo Forecast APIへ`past_days=1`を追加し、今日を昨日、明日を今日と比較できるようにした。
  - 最高・最低気温それぞれへ、表示上の丸め値に基づく前日比を`[+2]`、`[0]`、`[-1]`形式で追加。
  - 2週間一覧を日付、天気アイコン、最高、最低、最大降水確率の一行へ簡素化。天気名、AM/PM内訳、日次雨量の二段目を削除。
  - 正負、同値、比較元なしの前日比を回帰テストへ追加。
- 2026-07-29 同じ日の雨量・日付表示が違って見える問題を全面確認。
  - 画像の`7/31 0.3mm`は日次合計、案内の`0.1mm`は降り始め1時間の値だったが、案内に集計期間がなく同じ指標に見えていた。
  - 開始案内を「その15分/1時間」と「M/d一日合計」の併記へ変更。
  - 時間表のスクロール先が翌日でも上段が「今日」のため混同しやすかったので、「表示中 MM/dd(E)」を追加。
  - `today()`が日次配列の先頭を無条件に今日扱いしていた。端末地点のタイムゾーン上の現在日と一致する日だけを今日として返すよう修正。
  - 古い日を除外し、日付順・重複なしにする`forecastDays()`を追加。ホームの今日明日、2週間、週間画面、全ウィジェットで共通利用。
  - 詳細ダイアログは`DailyWeather`そのものを保持せず日付だけを保持し、更新後は最新スナップショットの同日データを再参照する。
  - ホームとウィジェットに重複していた日次降水集計をデータ層へ一本化。全時間0.0mmのときホームは0.0mm、ウィジェットは--mmになる差を解消。
  - 日付またぎ、現在日欠落、並び順・重複、15分/1時間雨量、開始枠と一日合計の区別を回帰テストへ追加。
- 2026-07-29 確率だけで折りたたみ傘を推奨する矛盾を修正。
  - `rainSignal()`が降水確率70%以上、今日の判断が30%以上だけで、予想雨量0.0mmでも「折りたたみ」と表示していた。
  - 傘の推奨条件を、最新レーダーで降雨中、または現在・将来の予想雨量が0.1mm以上の場合へ統一。
  - 確率70%以上・雨量0.0mmは「予報不一致 / 雨量予測なし」、40%以上は「確率のみ / 判断保留」と表示し、傘アイコンを出さない。
  - 今日の判断は雨量0.0mmなら「傘: 不要寄り」。雨量値そのものが未取得なら「判断不可」と区別。
  - 時間別の判断も雨量値未取得を0.0mm扱いせず、「雨量データなし / 判断不可」と表示。
  - 洗濯判断も確率だけで「外干し注意」とせず、雨量データなしの場合だけ「雨量待ち」と表示。
  - 確率100%・雨量0.0mmで傘を推奨しないこと、0.1mm以上なら推奨することを回帰テストへ追加。
- 2026-07-28 現在地へ戻る導線と現在降雨判定を改善。
  - ホーム上部へ常設の現在地ボタンを追加。地点ダイアログや横スクロールした保存地点タブを経由せず現在地へ切り替える。
  - 位置情報未許可時は権限確認を開き、許可済みなら更新中でも現在地への切替を開始して以前の地点更新をキャンセル。
  - Open-Meteoの`current.precipitation`はモデル値で局地豪雨の実況とずれるため、日本国内では気象庁高解像度降水ナウキャストを現在降雨の優先ソースに変更。
  - 最新の`targetTimes_N1.json`と`hrpns`タイルを取得し、ズーム10で地点周辺7×7ピクセル（約400m）の最大降水強度を解析。
  - 気象庁タイルの透明色と8段階のRGB色を、降雨なし、0.1、1、5、10、20、30、50、80mm/h以上へ変換。
  - レーダー値は15分以内だけ実況として利用し、観測時刻、強度下限、`mm/h以上`を表示。時間別の予報雨量`mm`と単位を分離。
  - 現在条件の天気アイコン・名称、雨の見通し、傘判断、次の雨、ウィジェット、バックグラウンド雨通知へ同じレーダー判定を反映。
  - 日本国外、レーダー取得失敗、未知のタイル色、古いレーダー値ではOpen-Meteoへフォールバック。
  - 全タイル色の変換、未知色、レーダー優先、鮮度判定の単体テストを追加。
- 2026-07-28 ウィジェットをアプリの選択地点へ確実に追従させるよう修正。
  - 従来はウィジェットが最後の天気キャッシュだけを読み、設定地点との一致を確認していなかったため、地点変更直後や通信失敗時に旧地点の天気を表示できた。
  - DataStoreから設定地点と天気キャッシュを同時に読み、地点が一致する場合だけ小・中・大・2×2ウィジェットへ表示。
  - 地点変更直後に全ウィジェットを更新し、新地点の取得完了までは地点名と「この地点の天気を取得中」を表示。旧地点の数値は表示しない。
  - 新地点の取得成功後にキャッシュを保存して全ウィジェットを再更新。
  - 地点取得中に別地点が選択された場合、先に開始した通信結果が後から設定地点とキャッシュを上書きしないよう一致確認を追加。
  - 連続した地点変更・更新では前の画面更新ジョブをキャンセルし、古い結果のエラーや完了状態を表示しない。
  - 選択地点を削除した場合は次の保存地点を選択し、ウィジェットを即時更新してその地点の予報を取得。
  - 起動時はViewModelの初期値ではなくDataStoreを直接確認し、保存済み地点が現在地へ意図せず戻る競合を防止。
  - 通信失敗時は同じ地点の成功済みキャッシュだけを継続表示し、別地点のキャッシュは表示しない。
  - ウィジェットには実際に表示している地点名を追加。
- 2026-07-28 現在地の重複保存と関連する保存・同期処理を修正。
  - GPS座標を小数4桁で比較していたため、測位の揺れごとに「現在地」が別地点として追加されていた。
  - 「現在地」を座標に依存しない1つの論理地点として扱い、再取得時は元の並び位置を保ったまま最新座標へ置換。
  - 旧バージョンで保存済みの複数の「現在地」はDataStore読み込み時に1件へ自動整理。完全一致する通常地点も重複除去。
  - 地点タブの選択表示、選択、削除、並べ替え、保存で同じ地点識別ロジックを使用。
  - 地点追加・削除・並べ替えをMutexで直列化し、同時操作による保存地点の取りこぼしを防止。
  - Open-Meteo検索結果で同一座標の重複を除去。
  - 雨通知の重複防止署名へ約1km単位の予報エリアIDを追加し、GPSの微小な揺れでは再通知せず、別都市の同時刻・同確率の予報は通知済み扱いにしない。
  - 永続化されるリストは保存地点のみであること、天気キャッシュと設定は上書き保存であることを確認。
  - WorkManagerは `enqueueUniquePeriodicWork` を使用しており、バックグラウンド更新ジョブが起動ごとに増殖しないことを確認。
  - 現在地の整理、最新GPS座標の採用、通常地点を誤って統合しないことを単体テストへ追加。
- 2026-07-28 世界の地点検索が動いていないように見える問題を修正。
  - 検索結果が長い保存地点リストの下に隠れていたため、入力欄の直下かつ保存地点より上へ移動。
  - 入力のたびの自動検索をやめ、検索ボタンとキーボードの検索キーで明示的に実行する方式へ変更。
  - 実行中表示、検索件数、0件、通信失敗をそれぞれ画面上部へ表示。
  - 前の検索をキャンセルし、応答順が前後して古い検索結果が新しい結果を上書きする競合を防止。
  - APIのHTTP失敗と0件を区別できるよう、Open-Meteoクライアントで通信・レスポンスエラーを握りつぶさないよう修正。
  - Open-Meteo実APIで `Heidelberg`、`Berlin`、`München` の取得を確認。`hiderberg` は0件のため、綴りや言語の変更を促す。
- 2026-07-28 ホーム導線、週間詳細、2×2ウィジェットを改善。
  - 重要気象情報カード全体をタップ可能にし、予報区、警報・注意報、台風、見出しを検索語に含めたGoogle検索を開くようにした。
  - ホーム上部は表示中地点を大きく明示し、更新時刻を併記。保存地点タブを高さ32dpの小型タブへ変更し、選択中だけ白背景と水色マーカーで識別。
  - 更新、設定、地点追加を表示中地点と同じ1行へ集約し、ヘッダー全体の高さを削減。
  - 2週間一覧の行を押した際、日別一覧と同じ情報を再掲するダイアログを廃止。その日の24時間分を共通の時間予報表で表示するよう変更。
  - 2×2ウィジェットの下余白へ今日/明日の天気名と最終更新時刻を追加し、選択画面プレビューも更新。
- 2026-07-28 降水時刻の矛盾、世界地点の時刻、2×2ウィジェットを修正。
  - 降水確率50%以上だけで雨量0.0mmの時刻を「雨開始」としていたロジックを廃止。0.1mm以上の予想雨量を共通の雨開始条件にした。
  - 15分予報の有効範囲では重複する1時間予報を使わず、範囲終了後だけ1時間予報へ接続する `PrecipitationRules` を追加。
  - 「傘を持つ」「次の雨」、雨の見通し、洗濯判断、通知、ウィジェットの降水文言を監査。確率だけ高く雨量0.0mmの場合は「確率高め」とし、雨が降るとは断定しない。
  - 今日の判断から過去時間の降水を除外し、現在時刻以降の最大確率と予想雨量だけで判定。
  - 雨量通知設定の0.0mmを禁止し、既存保存値も判定時に最低0.1mmへ補正。
  - Open-Meteoの `timezone=auto` とレスポンスのタイムゾーンを保存し、世界地点の現在時刻、今日/明日、時間表、通知、ウィジェットを地点現地時刻へ統一。
  - 専用2×2ウィジェットを参考画像に合わせ、地点名、今日/明日、大きい天気アイコン、最高/最低、降水確率の2列比較へ刷新。ウィジェット選択画面のプレビューも同構成に変更。
  - 降水確率だけ高い時刻を雨開始にしないこと、15分予報が重複時間の1時間予報より優先されることを単体テストに追加。
  - GitHub Actionsを `testDebugUnitTest assembleDebug` に変更し、単体テスト成功後だけAPKを公開するようにした。
- 2026-07-27 予報モデルの実測比較、時間別表示拡張、2×2ウィジェットを実装。
  - 東京の気象庁観測点47662について、2026-06-15から2026-07-26までの実測41日分をOpen-Meteo Previous Runs APIの過去予報と比較。
  - 最高気温MAEはBest Matchが1/3/5/7日前で1.46/1.95/2.02/2.74℃、JMA Seamlessが1.46/3.77/4.13/3.84℃。JMA Seamlessは3日後と5日後に約-3.7℃の低温バイアスがあった。
  - 通常予報をOpen-Meteo Best Match優先へ変更し、取得失敗時だけJMA Seamlessを使う構成に変更。選択された予報元をキャッシュとホーム表示に追加。
  - 時間別予報の固定48時間制限を廃止し、APIが返す最終時刻までを日付・時刻付きの単一横スクロール表で表示。`LazyRow`で長期間でも必要な列だけ描画。
  - Jetpack Glanceに専用 `WeatherSquareWidget` とReceiverを追加し、ウィジェット選択画面で「個人天気（2×2）」として追加可能にした。
  - 可変サイズ版と2×2版が同じDataStoreキャッシュを参照し、アプリ更新・WorkManager更新の両方で同時更新される。
- 2026-07-27 Yahoo!天気の画面構成を参考にホームデザインを刷新。
  - ホームの独立した「直近3時間」カードは不要との要望により削除。`minutely_15`データ取得自体は通知判定等のため維持。
  - 今日/明日、時間タブの日付見出し、2週間一覧で、土曜の曜日を青、日曜の曜日を赤に変更。
  - 保存地点を上部の横スクロールタブにし、地点切り替え、地点追加、更新、設定を同じ濃紺ヘッダーに集約。
  - 今日と明日を2列で比較し、日付、天気アイコン、天気名、最高/最低、降水確率を大きく表示。
  - 時間別を「時刻・天気・気温・降水確率・雨量」が同じ列で動く単一の横スクロール表に変更。
  - 追加参考画像に合わせ、時間表へ湿度と風向・風速を追加し、同じパネル内に日の出・日の入を表示。Open-Meteoのhourly取得項目とキャッシュモデルも拡張。
  - 「いまの天気」パネル内に現在地点の気象庁雨雲レーダープレビューを追加し、雨雲タブへ移動しなくてもホームで確認可能にした。
  - 絵文字中心だった天気表示を、グレーの雲、オレンジの太陽、青い雨滴、水色の雪、黄色い雷を重ねる共通ベクターアイコンへ変更。ホーム、48時間、直近雨、週間、日別詳細で共有。
  - 現在値を気温、気圧、湿度、体感、風、AQI、UVの高密度パネルに変更。
  - 2週間一覧は今日/明日と重複しないよう明後日以降を並べ、最高は赤、最低は青、降水は水色、詳細は右矢印で統一。
  - 全体の角丸を小さくし、黒・チャコール・濃紺中心の配色と下部ナビへ統一。
- 2026-06-14 UI/UX調査を踏まえてホームの直感性を改善。
  - Android App Widgetsの「at-a-glance」方針、iOS系天気アプリの「次の数時間を常に見せる」構成、天気アプリの最低要件としての現在地・現在気温・時間別/日別・日の出入・風・湿度を確認。
  - ホーム上部を「今すぐの判断」に変更し、傘・服装・暑さ/寒さ/蒸れ/風の行動ラベルを先に出す構成へ変更。
  - 雨の見通しを「雨具必要」「予定見直し」「身軽でOK」などの行動ラベル、次の雨、ピーク時刻、雨量に整理。
  - 直近3時間、今後48時間、2週間AM/PMカードで、数値だけでなく行動ラベルと雨の強さを併記。
  - 週間詳細ダイアログも判断ラベル、雨リスクバー、AM/PMの行動目安を表示。
- 2026-06-14 画像参考の時間別・2週間表示へ変更。
  - 時間別予報を、天気アイコン、気温折れ線、降水確率、降水量を同じ時刻列に縦揃えした横スクロールタイムラインへ変更。
  - 降水量は白い箱と数値で表示し、降水確率は水色の%で表示。
  - ホームの時間別と時間タブの表示を同じタイムライン部品に統一。
  - 2週間天気を大きいカード列から高密度リストへ変更し、今日/明日の概要、日付、天気、最高/最低、降水確率、雨量、AM/PM概況、詳細用の+を表示。
- 2026-06-28 時間別タイムラインの今日/明日バッジ、天気アイコン、時刻、降水確率、降水量の文字が高密度端末で小さすぎたため、Canvas文字をsp基準に変更し、列幅と高さを拡大。
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
  - models指定なしのBest Matchを優先。
  - 失敗時は `models=jma_seamless` へフォールバック。
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
  - `minutely_15` の15分ごとの雨予報は通知判定等に利用し、独立カードはホームから削除済み。
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
- `app/src/main/res/layout/weather_widget_square_preview.xml`
- `app/src/main/res/xml/weather_widget_info.xml`
- `app/src/main/res/xml/weather_widget_square_info.xml`

### アプリ基盤

- `app/src/main/java/com/example/weather/WeatherApplication.kt`
- `app/src/main/java/com/example/weather/AppServices.kt`
- `app/src/main/java/com/example/weather/MainActivity.kt`

### data

- `app/src/main/java/com/example/weather/data/api/OpenMeteoClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaAmedasClient.kt`
- `app/src/main/java/com/example/weather/data/api/AirQualityClient.kt`
- `app/src/main/java/com/example/weather/data/api/AppUpdateClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaRadarClient.kt`
- `app/src/main/java/com/example/weather/data/api/JmaDisasterClient.kt`
- `app/src/main/java/com/example/weather/data/cache/WeatherCache.kt`
- `app/src/main/java/com/example/weather/data/model/WeatherModels.kt`
- `app/src/main/java/com/example/weather/data/model/TemperatureModels.kt`
- `app/src/main/java/com/example/weather/data/model/RadarTime.kt`
- `app/src/main/java/com/example/weather/data/model/PrecipitationRules.kt`
- `app/src/main/java/com/example/weather/data/model/AppUpdateModels.kt`
- `app/src/main/java/com/example/weather/data/model/DisasterModels.kt`
- `app/src/main/java/com/example/weather/data/repository/WeatherRepository.kt`
- `app/src/main/java/com/example/weather/data/repository/TemperatureConsensusEngine.kt`

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
- `app/src/test/java/com/example/weather/data/model/PrecipitationRulesTest.kt`
- `app/src/test/java/com/example/weather/ForegroundRefreshTimingTest.kt`
- `app/src/test/java/com/example/weather/data/api/JmaAmedasClientTest.kt`
- `app/src/test/java/com/example/weather/data/api/JmaDisasterClientTest.kt`
- `app/src/test/java/com/example/weather/data/api/JmaRadarClientTest.kt`
- `app/src/test/java/com/example/weather/data/model/CurrentTemperatureSourceTest.kt`
- `app/src/test/java/com/example/weather/data/model/RadarPrecipitationRulesTest.kt`
- `app/src/test/java/com/example/weather/data/model/RadarTimeTest.kt`
- `app/src/test/java/com/example/weather/data/model/WeatherLocationTest.kt`
- `app/src/test/java/com/example/weather/data/repository/TemperatureConsensusEngineTest.kt`

### ドキュメント

- `README.md`
- `handoff.md`

## 未解決課題

- Pixel API 34エミュレータでは上書きインストール、表示、通信を確認済み。今回のAPKは物理Galaxyでの最終確認が必要。
- 端末内のモデル誤差重みは初回時点では未学習。選択地点で1時間以上前に保存した予報と後日のアメダス実況が各モデル3標本以上たまってから適用される。
- 現在気温は秒単位のライブ値ではない。アメダス発表とアプリ再取得は約10分間隔で、20分超の観測は実況扱いしない。
- 雨雲レーダーはMVP。
  - 気象庁タイル仕様変更に弱い。
  - パン/ズームはボタン式で最低限。ピンチズーム、慣性スクロール、スムーズな地図操作は未実装。
  - 地図タイル利用ポリシーは公開配布前に再確認が必要。
- 重要気象情報はMVP。
  - 警報・注意報の地点判定は府県予報区レベルの近似。
  - 日本の地点だけが対象。台風は一覧表示のみで、海外のサイクロン、進路図、暴風域確率、詳細諸元は未実装。
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
   - 時間画面のグラフとカードの時刻が横スクロール位置で一致すること。
   - 地点検索、保存、並べ替え、削除。
   - 海外地点で日本の警報・台風・アメダス・雨雲レーダーが混在しないこと。
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
