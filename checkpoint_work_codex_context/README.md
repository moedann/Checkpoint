# チェックポイント：Work × Codex 開発コンテキスト
作成日：2026-10-12（日本時間）／版：1.0

## 使い始める手順
1. このフォルダを展開する。これはアプリ本体ではなく、開発用の文書セット。
2. `WORK_PROJECT_INSTRUCTIONS.md`の内容をChatGPTプロジェクトの指示へ貼る。
3. Workにはこのセットと最新のコード・文書を渡す。接続で取得できない場合は最新のZIPを添付する。
4. アプリのGitリポジトリへ`AGENTS.md`、`docs/`、`tasks/`を追加する。既存の同名ファイルがあれば差分を確認して統合し、一括上書きしない。
5. 最初は`tasks/TASK-001-current-state-audit.md`で実装調査を行う。
6. 以後、Workで仕様を具体化→Codexで実装・検証→Workでレビュー→Pixel 7aで実機確認、を繰り返す。

## 読む順序
`AGENTS.md` → `docs/STATUS.md` → 今回のタスク → 関連する`docs/SPEC.md`と`docs/DECISIONS.md` → 該当コード。
全履歴を毎回投入せず、現在のタスクに必要な情報を読む。

## 状態と制約
このセットは添付された要件書とPRODUCT_PLAN.mdを元に作成した新規案。以前作ったコンテキストキットの内容は読み込んでいない。
アプリの`lib/`、`android/`、`test/`、Git履歴は未提供。実装・テスト成功の記述は計画書による報告であり、この作成時に再検証していない。
GitHubへの追加、プロジェクト指示の設定、Codex環境の構築、APK作成はまだ行っていない。

## 文書の役割
- `WORK_PROJECT_INSTRUCTIONS.md`：相談・仕様化・レビューを担うWorkの指示。
- `AGENTS.md`：コードを編集するAIの共通ルール。
- `docs/SPEC.md`：要件の整理。確定事項・候補・未決定を区別。
- `docs/STATUS.md`：実装状況と証拠。
- `docs/DECISIONS.md`：採用理由と変更履歴。
- `docs/WORKFLOW.md`：作業と同期の手順。
- `docs/VALIDATION.md`：検証の方法と結果。
- `tasks/`：今回の依頼とテンプレート。
- `docs/HANDOFF_TEMPLATE.md`：CodexからWorkへの引き継ぎ。
- `sources/`：提供された資料のスナップショット。現行仕様として直接適用しない。

## 最小運用
同じ箇所を両方で同時編集しない。コードと状態文書を同じ変更単位に含める。引き継ぎにはブランチとコミットIDを添える。
文書管理と同期はAIが担当し、もえだんには製品判断と実機確認を中心に依頼する。
