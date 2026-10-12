# 検証計画と記録
初期文書作成時点ではアプリのテスト・ビルド・実機確認は未実施。最新の実行報告は末尾のCodex Cloudセットアップ記録を参照。

## 環境確認
Flutter/Dart、JDK、Android SDK、Gradleの実際の版と利用可能性を記録。リポジトリの設定と整合させる。
候補コマンド：`flutter analyze`、`flutter test`、`flutter build apk --debug`。
Kotlin検証は実在するモジュール・テストを確認してGradleタスクを選ぶ。SDKがない環境では実行不能と記録する。

## アプリ横断の主シナリオ
1. 帰還点を「数学の宿題」に設定。
2. YouTubeで「二次関数の解説を見る」、予定時間を設定。
3. Chromeへ移動し「続き」を選択。
4. 同じセッションID、目的、帰還点、計測方式に沿った残り時間が維持される。
5. HOME→ロック→再開時に、決定した時間計測・表示仕様どおりに動く。
6. 時間終了後に目的を再提示。達成を選び、帰還点の提示からHOMEへ移動。
実際に勉強へ戻ったかは、本人の別報告として記録する。

## 境界シナリオ
別目的で開く／開かない／延長／脱線／イベント重複／除外アプリ／通話／通知拒否／権限解除／サービス再生成。
時間を短くして10回連続の起動・終了を確認。ロック中や通話中の割込み、二重表示、時間のリセットを確認する。
前面時間方式を採用した場合はHOME・ロック・除外・確認画面中の計測が決定と一致するかを重点確認。

## 記録テンプレート
- 日時・タスクID：
- コミット／未コミット差分：
- 端末・Android・APK版・ビルド元：
- 区分：自動／ビルド／実機／本人感想
- 手順またはコマンド：
- 期待結果：
- 実際の結果とログ：
- 判定：合格／不合格／未実施
- 残課題と次の対応：

## GitHub初回保存（2026-10-12／Codex）
- 対象：main、c094e0206a77c2c8e2e78148fb04be927116fe05。
- gh repo create moedann/Checkpoint --private：成功。
- git push -u origin main：成功。
- git rev-parse HEAD と git ls-remote origin refs/heads/main：同じコミットを確認。
- 保存対象の既知のトークン・秘密鍵パターン検索：該当なし。署名鍵・.env・*.hprofは除外。
- アプリの解析・テスト・APKビルド・実機確認：未実施（GitHub接続のみの作業）。

## Codex Cloudセットアップ検証（2026-10-12）
- 基準：main・2852d33f9bf40906a5a980ba728093e4b8e204d2。製品コード・ロックファイルの変更なしとのCodex報告。
- 担当：Codex Cloudが実行、Workが画面上の結果報告をレビュー。Workからの独立再実行はしていない。
- 環境：Flutter 3.44.0／Dart 3.12.0、JDK 17.0.20.1、Android SDK 36、Build Tools 35/36、NDK 28.2.13676358、Gradle 8.11.1。
- 保存用導入スクリプト再実行：成功。凍結した依存取得：成功、ロックファイル変更なし。
- Flutter既存テスト：14件すべて成功。
- Flutter解析：完走。既存コードの情報レベル指摘16件、終了コード1。解析成功とは扱わない。
- Android通常設定：Kotlin単体テスト32件成功、デバッグAPK生成・署名検証成功。Kotlinの環境側バージョン上書きは使用していない。
- 切り分け：初期Flutter 3.38.5はロック内のSDK依存に不一致→環境のFlutterを3.44.0へ変更。コンパイラ不足→JDK 17導入。追加JDKの証明書エラー→環境提供のシステム信頼ストアを指定。TLS検証は維持。
- 接続先：パッケージマネージャーの既定許可＋storage.googleapis.com・maven.google.com。環境公開後に接続反映を確認。
- 残課題：Kotlinの宣言1.8.22とログ上の実効2.0.0の差、互換性警告、解析指摘。修正は別タスク。
- Pixel 7a実機・権限・他アプリ上のオーバーレイ・ライフサイクル：未実施。
- 詳細報告：https://chatgpt.com/local/01a12d51-c03b-77c2-b3d5-5b41204840fb?hostId=local

## TASK-FLOW-001：ブラウザ経由の依頼・受領確認（2026-10-12）
- 依頼基準：8bb2c26143b21606116da3a448b14d1e6db5098f。Codex実行HEAD：27599e572e70e7fb2c2371e2b505589f1664682f、ブランチwork。
- 成功：新規タスク送信→Codex起動→シェル読み取り→結果報告。Workから基準文書の抜粋を追加送信し、Codexが受領してローカル版と区別できたことも確認。
- Codex報告：開始・終了ともgit status --shortは空。Flutterの保持済み実行ファイルと版情報は3.44.0。java／javacは17.0.20.1。FlutterのPATH設定は別途保持された起動手順の有効化が必要。
- 失敗：git ls-remoteの通常Git接続でproxy port8080へ接続できず、リモート照合・fetchは未実施。Git同期成功とは扱わない。
- Work独立確認：GitHub接続で8bb2c26のWORKFLOW・STATUSを取得。GitHubのコミット比較で27599e5から8bb2c26は祖先関係あり、変更はSTATUS・WORKFLOWのみ。製品コードの差分なし。
- ソースの受け渡し・適用、実装後差分の保存、最新HEADでのテストは今回の検証範囲外。解析・テスト・APKビルドの再実行なし。
- 実行報告：https://chatgpt.com/local/01a12e80-b499-7233-922c-dc17127190fa

## TASK-003（2026-10-12）
対象：main 479cc4bcef750e475e3f228d58a4a150cb6e84a5からのtask-003-question-core-20261012差分。
環境：Linux x64、Flutter 3.44.0（559ffa3f75）、Dart 3.12.0。SDKは作業用一時領域に導入。
Flutterのフォント3個はGoogle Fontsの同名公開ファイルを取得し、Git blob SHAが基準コミットの各ファイルと一致することを確認。元ファイルは変更しない。

| 検証 | 実行 | 結果 |
|---|---|---|
| Flutter依存解決 | flutter pub get | 成功。unorm_dart 0.3.3のみ追加 |
| Flutter全テスト | flutter test --no-pub --reporter expanded | 成功、45件（既存14＋新規31） |
| 追加範囲の解析 | flutter analyze --no-pub lib/question_engine test/question_engine_test.dart | 成功、No issues found |
| 全体解析 | flutter analyze --no-pub | info16件で終了1。全件が未変更review_questions_screen.dartに所在。errors/warningsなし |
| JSON形式 | Python jsonschema Draft202012Validator.check_schema＋fixture validate | 成功、10問 |
| schema同期 | Pythonでschema_data.dartのraw JSONと公開schemaを比較 | 成功 |
| 差分空白 | git diff --check | 成功 |
| Kotlin/Gradle | 未実行 | Android SDK・Gradle実行環境なし。Kotlinは未変更 |
| APK・実機 | 未実行 | UI／ゲートは未接続、実機なし |

新規31件は10問の採点、正答IDの順序独立、誤答・部分正解、NFKC・trim・case設定、数値形式・絶対/相対許容差、表のセル別採点、順序/分類/語群対応、自己採点と未評価の区別、JSON往復・不変性、不正データ・ID/キー参照・上限、保存の重複/競合・版更新/同時インスタンス/読込失敗/最大長IDを含む。
最初の試験でfixtureのfield IDをcalcとしたテスト側の誤りを検出し、正しいcalculationに訂正。最終実行は45件成功。
Dart 3.7.2でも同じ31件をpackage:testの一時ハーネスで先行実行したが、上表の正式結果は元のFlutterテストファイルを使った3.44.0での実行。

注意：取り組み・採点成功と答え合わせ確認、他アプリ利用解放、学習効果を分ける。今回の結果から6方式の実機操作や188資格への対応を検証済みとは扱わない。
