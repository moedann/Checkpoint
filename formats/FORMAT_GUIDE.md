
## TASK-003の実装範囲（2026-10-12）
- `lib/question_engine/question_engine.dart` が共通モデル・codec・純粋採点の入口。
- `.cpq.json`のみ。`.cpqpack`、画像・音声参照は明確なエラーで拒否。数式ブロックは文字列として保持し、評価・描画はしない。
- 暫定上限：UTF-8 10MiB、2000問、32階層、各配列10000要素、各文字列65536文字、エラーレポート50件。画像素材の100MB案は今回適用しない。
- 数値：NFKCと前後空白除去、小数点`.`、正負・指数表記、有限値のみ。桁区切りや暗黙の単位換算は不可。`unit`と同じ末尾単位は省略・付記とも可。数値の書式不正は提出前エラー。
- テキスト：NFKC／trimは既定ON、大文字小文字の区別は既定ON。各fieldのフラグで変更。grid textセルの草案にないフラグは追加せず、同じ既定動作に固定。
- multiple_choiceの空集合正答は1.0.0スキーマが禁止するため未対応。仕様の例外記述より具体的な公開形式を優先し、将来の改版で扱う。
- `interaction`の草案スキーマにはモード別必須キーの条件がないため論理検証で補う。orderingはorderedIdsのみ、groupingはgroupsとassignmentsのみ。
- grid回答キーは`gridCellKey(rowId,columnId)`（2要素JSON配列文字列）。区切り文字を含むIDでも衝突しない。
- repositoryは専用のアプリ内部ディレクトリを呼び出し側から受け取る。実アプリの保存場所の接続は後続。
- 同じpackIdは自動置換しない。`expectedRevision`が一致し、取り込むrevisionが大きいときだけ明示置換可。単一のDart isolateで使用し、同じディレクトリのインスタンスは直列化。別プロセスはファイルロックで排他。

```dart
import 'question_engine/question_engine.dart';
import 'question_engine/repository.dart';
final pack = const QuestionPackCodec().decode(jsonText);
final result = gradeQuestion(pack.questions.first, const SingleChoiceAnswer('a'));
// UIで作者・出典・内容のプレビュー確認後に repository.save(pack) を呼ぶ。
// result.grade は利用解放条件ではない。答え合わせ確認はTASK-006で実装。
```

スキーマ変更時：`python3 tool/generate_question_schema.py` → `dart format lib/question_engine/schema_data.dart` → テスト再実行。
