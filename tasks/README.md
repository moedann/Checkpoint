# タスク一覧
更新：2026-11-14。全タスクの所在と状態の索引（docs/CONTEXT_RULES.md第3節）。状態の詳細は各タスク文書が正。新規タスクは`TASK_TEMPLATE.md`で作り、ここへ一行追加する。

| ID | 名前 | 状態 | 依存 | 所在 |
|---|---|---|---|---|
| TASK-001 | 現在のコード・仕様・環境を照合する | 入力待ち（文書上）。コードはGitHub mainに保存済み | なし | `tasks/TASK-001-current-state-audit.md` |
| TASK-002 | アプリをまたいで調べものを続ける | 草案 | TASK-001、D-005〜008 | `tasks/TASK-002-cross-app-session-draft.md` |
| TASK-003 | 6形式共通の問題モデル・採点・JSON取り込み | 設計草案 | なし | `design/question-engine/v0.1/tasks/TASK-003-question-core.md` |
| TASK-004 | 6方式の回答UI＋答え合わせ | 設計草案 | TASK-003 | `design/question-engine/v0.1/tasks/TASK-004-six-renderers.md` |
| TASK-005 | 外部作問の入口とインポートUI | 設計草案 | TASK-003 | `design/question-engine/v0.1/tasks/TASK-005-external-authoring.md` |
| TASK-006 | 学習問題→答え合わせ→対象アプリ利用再開 | 設計草案 | TASK-002、003、004 | `design/question-engine/v0.1/tasks/TASK-006-checkpoint-integration.md` |
| TASK-FLOW-001 | ブラウザ経由のCodex依頼・受領確認 | 完了（2026-10-12） | なし | 記録：`docs/log/STATUS_LOG.md`、`docs/log/VALIDATION_LOG.md` |

## 書き方
- 状態は`TASK_TEMPLATE.md`の区分を使う。
- 次に着手するものは`docs/STATUS.md`の「直近の順序」に書く。この一覧では順序を決めない。
