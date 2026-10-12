# Checkpoint Question Pack 形式仕様 v0.1（試案）

**標準入口：** `*.cpq.json`。JSON Schema draft 2020-12：`checkpoint-question-pack.schema.json`。未信頼入力に含まれる動的コードは実行しない。

### 主なキー
- `formatVersion`：現時点は `1.0.0`。互換性判断に使う。
- `packId`、`title`、`authorName`、`locale`、`exam`、`questions`。
- `questions[].type`：`single_choice`、`multiple_choice`、`entry`、`grid`、`interaction`、`word_bank`、`self_review`。
- `questions[].prompt`：設問本文。`explanation`：答え合わせ画面の解説。`stimulus`：将来のテキスト／図・音声の教材ブロック（初期はテキスト中心）。
- 6方式で答えの持ち方が違う。`answerKey` と入力セルの期待値は作者が作成・レビューする。

### `type`別の主なキー
- `single_choice`: `options [{id,label}]` + `answerKey.optionId`
- `multiple_choice`: `options` + `minSelections` / `maxSelections` + `answerKey.optionIds[]`
- `entry`: `fields [{id,label,mode:'text',acceptedAnswers[]}|{id,label,mode:'number',expectedValue,absoluteTolerance?}]`
- `grid`: `columns[]` / `rows[].cells[]` のうち `mode='text'|'number'` のセルを入力
- `interaction`: `mode='ordering'|'grouping'`、`items[]`、正解の `answerKey.orderedIds[]` または `answerKey.assignments[]`
- `word_bank`: `segments[]`、`tokens[]`、`reuseTokens`、`answerKey.matches[]`
- `self_review`: 紙／PC／実技タスクと `modelAnswer`／`reviewRubric`。

### 表計算（CSV）入口
`examples/basic-import.csv` は著作者がPC上のExcel/Google Sheets等で問題を作る例。
CSVは仕様の中心ではなく、取り込み時に標準JSONへ正規化する。選択肢は `option_id:表示名;...`、複数正解は `A|C` とする。**CSVの直接受け入れUIは実装タスクに含まれ、現在は存在しない。**

### 画像・音声を含む問題集（後続）
`.cpqpack` = ZIP (`manifest.json`, `questions.json`, `assets/*`)。全アセットはmanifestでSHA-256とサイズを宣言する予定。相対パスだけ許可し、HTML、Script、外部URL自動取得、ZIPパス走査は許可しない。音声の使用許可・再配布許可も別途管理。

### 外部AIに渡す作問指示の例
```
以下のJSON Schemaとサンプルに厳密に従い、オリジナルの練習問題を作ってください。
対象：ITパスポート、ストラテジ系。問題数：5問。難易度：基礎。
全問に一意のID、正答、誤答、短い解説、出典の区分（オリジナル）を記載。
実在の過去問の転載や不明確な出典の引用は禁止。
出力はJSONのみ。最初にスキーマに合うか、正答が選択肢内に存在するか、
説明と答えが一致するか、重複IDがないか自己点検してください。

[checkpoint-question-pack.schema.json の全文]
[examples/all-image-examples-plus-self.cpq.json の適切な例]
```
AIが「自己点検」と書いていても人のレビューおよびアプリのインポート検証は省略できない。
