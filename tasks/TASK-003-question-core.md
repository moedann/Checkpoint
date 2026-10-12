# TASK-003：6形式共通の問題モデル・採点・JSON取り込み
状態：基盤実装済・Dart自動検証済／Android追加検証未実施／担当：Codex（Work内で直接実装）
基準：moedann/Checkpoint main 479cc4bcef750e475e3f228d58a4a150cb6e84a5
作業ブランチ：task-003-question-core-20261012

## 目的・範囲
外部AI・Webエディタで作成する *.cpq.json を損失なく検証・採点・保存するドメイン層。
設計セットのQUESTION_ENGINE_SPEC、FORMAT_GUIDE、スキーマ、10問サンプルを参照。
今回のユーザー指示「設計セットを参考に実装を始めてください」に基づく最初の変更単位。
Flutter管理画面・Kotlinオーバーレイ・既存問題DBへの統合はTASK-004〜006で扱う。

## 完成条件
- 単一／複数／テキスト数値／表／並べ替え・分類／語群穴埋め＋自己採点。
- 10問fixture、誤答・部分正解、選択肢入替、NFKC、数値許容差、未回答・不正参照をテスト。
- JSON検証→読み取り専用プレビュー→呼び出し側の明示確認→保存。重複は拒否し、置換には期待版番号が必要。
- 保存は一時ファイルflush＋同一ファイルシステムrename。入力の不正で既存ファイルを消さない。
- ID、作者、出典、利用許諾、試験情報、正答キーをexportでも保持。
- 新権限・ネットワーク送信・外部コード実行を追加しない。
- 既存Flutterテストを再実行。Kotlin/実機の検証有無を区別し記録。

## 検証
flutter test／flutter analyze lib/question_engine test/question_engine_test.dart。
Python jsonschemaによるfixture検証と埋め込みスキーマ一致確認。
詳細はdocs/VALIDATION.mdとdocs/HANDOFF-TASK-003.md。
