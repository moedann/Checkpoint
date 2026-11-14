# Claude Code用の入口
共通ルールはAGENTS.mdに一本化している。このファイルにはClaude Code固有の補足だけを書く。

@AGENTS.md

## Claude Code固有
- コンテキストの読み方は`docs/CONTEXT_RULES.md`に従う。最初に`docs/STATUS.md`と`tasks/README.md`を読む。
- 自動メモリ（`~/.claude/projects/.../memory/`）は作業の好みの記録に限る。仕様・状態・決定はリポジトリの文書へ書く。
- 広い検索は調査用サブエージェントに任せ、結論と根拠のファイル・行だけを受け取る。
- 会話が要約された後は、STATUSと対象タスクを読み直してから続ける。
