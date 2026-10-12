# Checkpoint：6形式問題エンジン・外部問題作成 設計セット v0.1

作成：2026-10-12／状態：**設計提案・実装未着手**（仕様とサンプル形式のみ）
対象：`moedann/Checkpoint` の Flutter + Kotlin / Android 製品

## ファイル
- `docs/QUESTION_ENGINE_SPEC.md`：製品体験、6回答方式、採点、学習ゲート、外部作問、制約とフェーズ。
- `docs/VALIDATION_PLAN.md`：形式別・インポート・Androidゲートの完成条件。
- `docs/DECISIONS_PROPOSED.md`：今回の要求から確定できる前提と仮置きの区分。
- `formats/checkpoint-question-pack.schema.json`：外部作問・AI生成・インポート共通のJSON Schema草案。
- `formats/FORMAT_GUIDE.md`：規格の説明、JSON/CSV/ZIP、改版方針。
- `examples/all-image-examples-plus-self.cpq.json`：6形式の全主要操作と、自己採点の代替形式を含むサンプル10問。
- `examples/basic-import.csv`：外部の表計算ソフトで作れる単一選択・複数選択・テキスト入力例。
- `tasks/TASK-003-question-core.md`：問題ドメインとインポート検証。
- `tasks/TASK-004-six-renderers.md`：6形式の解答画面と採点。
- `tasks/TASK-005-external-authoring.md`：外部作問・AI生成・取り込み。
- `tasks/TASK-006-checkpoint-integration.md`：起動時学習チェックポイントへの統合。

このセットは現行リポジトリの文書の上書き用ではありません。コミット前に最新の `AGENTS.md`、`docs/SPEC.md`、`docs/STATUS.md` と統合し、タスクIDの重複を確認してください。

## 今回守る製品ルール
1. 対象アプリの利用再開は「問題に取り組む→回答を提出→答え合わせを表示・確認」で成立し、正解は要求しない。
2. 学習ゲートは利用者が設定する学習支援機能。通常の目的セッションを消さない。
3. 端末外で問題を作成・生成でき、標準形式として取り込める。外部作問のためのログインをMVPの必須条件にしない。
4. 既存問題の著作権・正答確認・出所の明示を利用者に求め、AI回答は確定正答とみなさない。
5. 画像上の6方式を網羅するが、188資格の完全網羅を検証済みとは扱わない。

## 検証情報
サンプルJSONはJSON Schemaによる形式検証を実行する。この段階でAndroidアプリの解析・テスト・APKビルド・実機確認は**していない**。
