# 引き継ぎ：コンテキスト運用ルールの導入
- タスクID・状態：D-009（ユーザー依頼）／完了・文書のみ
- 作業担当：Claude Code（2026-11-14）
- リポジトリ・ブランチ：moedann/Checkpoint／claude/context-engineering-dev-rules-aa31e0
- 基準コミット／結果コミット：479cc4b／このファイルを含むコミット
- 未コミット差分の有無：なし。PRリンク：未作成（push・PRは依頼されていない）

## 結果
ユーザーのどの体験をどう変えたか：アプリの動作は変更なし。AIが作業開始時に読む量を減らし、古い情報を読む誤りを防ぐ。
変更ファイルとその理由：
- `docs/CONTEXT_RULES.md`（新規）：読込層、一事実一か所、文書の大きさ、セッション運用、終了時の確認。
- `CLAUDE.md`（新規）：Claude CodeがAGENTS.mdを読むための入口（`@AGENTS.md`）と固有の補足。
- `tasks/README.md`（新規）：tasks/とdesign/に分かれたTASK-001〜006の索引。
- `docs/STATUS.md`：93行→43行。現在地の表・直近の順序は維持し、日付付きの記録を`docs/log/STATUS_LOG.md`へ原文のまま移動。
- `docs/VALIDATION.md`：計画とテンプレートだけに。実行記録を`docs/log/VALIDATION_LOG.md`へ移動。
- `AGENTS.md`・`WORKFLOW.md`・`WORK_PROJECT_INSTRUCTIONS.md`・テンプレート：新しいルールと置き場所を参照。
- `checkpoint_work_codex_context/README.md`：現行版ではないという注記だけを追加。
仕様・設計判断の変更：DECISIONSにD-009を追加。製品仕様（SPEC）の変更はなし。
コード調査で分かった文書との相違：なし（コードは読込対象外）。

## 検証
| 検証 | 実行内容 | 結果 | 証拠・環境 |
|---|---|---|---|
| 文書の整合 | 移動前の非空行の残存確認、参照パスの実在確認 | 合格 | docs/log/VALIDATION_LOG.md |
| 静的解析・自動テスト・APK・実機 | — | 未実施 | 製品コード変更なし |

## 次の行動
次のセッションが最初に読むもの：`CLAUDE.md`→`docs/STATUS.md`→`tasks/README.md`→TASK-001。
Workで判断すること：ChatGPTプロジェクト指示へ`WORK_PROJECT_INSTRUCTIONS.md`の更新を貼り直すか。`checkpoint_work_codex_context/`を削除するか残すか。
次のタスク：TASK-001（変更なし）。
