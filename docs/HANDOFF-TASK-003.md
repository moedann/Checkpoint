# Codex → Work 引き継ぎ：TASK-003
- 状態：基盤実装済／Dart自動検証済／Android追加検証・UI接続は未実施。
- 作業担当：Codex（Work内で直接実装）。
- リポジトリ・ブランチ：moedann/Checkpoint、task-003-question-core-20261012。
- 基準コミット：479cc4bcef750e475e3f228d58a4a150cb6e84a5。
- 結果コミット／PR：当該Draft PRのHEAD・Files changedを参照。mainのマージ・公開・配布はしていない。

## 結果
外部作問JSONを6方式＋自己採点として損失なく扱うDart基盤を追加。ユーザー画面はまだ変わらない。

| ファイル | 理由 |
|---|---|
| lib/question_engine/question_engine.dart、models.dart | 共通API・型別回答・不変モデル・採点結果 |
| lib/question_engine/codec.dart、schema_data.dart | オフライン形式検証と論理検証、ID維持のJSON往復 |
| lib/question_engine/grading.dart | UI非依存の採点、セル別結果、未回答チェック |
| lib/question_engine/repository.dart | 読み取り専用preview、専用保存、重複・競合拒否、原子rename |
| test/question_engine_test.dart | 31件の境界条件とfixtureテスト |
| formats/*、examples/*、tool/generate_question_schema.py | 外部作問の公開形式と10問サンプル、schema同期 |
| pubspec.yaml、pubspec.lock | NFKC用unorm_dart追加。他の依存変更なし |
| docs/SPEC・DECISIONS・STATUS・VALIDATION、QUESTION_ENGINE_SPEC、tasks/TASK-003 | 既存仕様との統合、実装の範囲・証拠・制約 |

## 検証
Flutter全45件成功、追加範囲の解析0件、全体は既存info16件のみ。公開schema／fixture／埋め込みschema一致も成功。詳細はVALIDATION。
Kotlin・APK・実機は未実施。Kotlinや既存画面のコード・DBは変更していない。

## 次の行動
TASK-004：単一選択から全方式のFlutterプレイヤー・結果画面を接続。ordering/groupingはボタン・選択UIの代替を提供。
TASK-005：標準JSONのファイル選択→プレビュー→利用者確認→保存を接続。外部Web作問／CSVはこの後。
TASK-006：答え合わせ確認・解放をゲート状態と結びつけ、Kotlin/目的セッションを統合してPixel 7aで検証。
採点のgradeは解放条件ではない。SelfReviewAnswer.ratingは模範解答表示後にUIが設定する。履歴DB・gate永続化は後続。
専用repositoryは単一Dart isolateで使用。アプリの保存ディレクトリはホスト側が指定する。ファイルflushとrenameはAndroid/Linux上で原子置換を構成するが、突然の電源断に対する保証は別検証。
