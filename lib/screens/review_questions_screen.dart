import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../bridge.dart';
import '../review_question_parser.dart';

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}

String rate(int correct, int answers) =>
    answers == 0 ? '0%' : '${(correct * 100 / answers).toStringAsFixed(1)}%';

class ReviewQuestionsScreen extends StatefulWidget {
  const ReviewQuestionsScreen({super.key, required this.bridge});
  final NanishiniBridge bridge;
  @override
  State<ReviewQuestionsScreen> createState() => _ReviewQuestionsScreenState();
}

class _ReviewQuestionsScreenState extends State<ReviewQuestionsScreen> {
  AppSnapshot? data;
  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    final value = await widget.bridge.loadSnapshot();
    if (mounted) setState(() => data = value);
  }

  Future<void> addBook() async {
    final value = await textDialog(context, '問題冊子を追加', '冊子名');
    if (value != null) {
      await widget.bridge.addQuestionBook(value);
      await load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final d = data;
    if (d == null) return const Center(child: CircularProgressIndicator());
    final selectedBooks = d.questionBooks
        .where((book) => d.selectedQuestionBookIds.contains(book.id))
        .toList();
    final activeQuestions = d.reviewQuestions
        .where((q) => q.isEnabled && d.selectedQuestionBookIds.contains(q.bookId))
        .toList();
    final selectedBookNames = selectedBooks.map((book) => book.title).join('、');
    return Scaffold(
      appBar: AppBar(title: const Text('問題')),
      floatingActionButton: FloatingActionButton(
        tooltip: '追加',
        onPressed: () => _showAddMenu(d),
        child: const Icon(Icons.add_rounded),
      ),
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ListTile(
                leading: const Icon(Icons.school_outlined),
                title: const Text('復習CAPTCHA'),
                subtitle: Text(
                  selectedBooks.isEmpty
                      ? '問題冊子が選択されていません'
                      : '$selectedBookNames\n使用中 ${activeQuestions.length}問・1回 ${d.reviewQuestionCount}問',
                ),
                isThreeLine: selectedBooks.isNotEmpty,
                trailing: const Icon(Icons.chevron_right),
                onTap: () async {
                  await Navigator.push<void>(
                    context,
                    MaterialPageRoute(
                      builder:
                          (_) => ReviewCaptchaSettingsScreen(
                            bridge: widget.bridge,
                          ),
                    ),
                  );
                  await load();
                },
              ),
            const Divider(),
            const Padding(
              padding: EdgeInsets.fromLTRB(4, 20, 4, 8),
              child: Text(
                '問題冊子',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ),
            for (final book in d.questionBooks)
              _bookTile(
                book,
                d.reviewQuestions.where((q) => q.bookId == book.id).toList(),
                d.selectedQuestionBookIds.contains(book.id),
              ),
            const SizedBox(height: 72),
          ],
        ),
      ),
    );
  }

  Widget _bookTile(QuestionBook book, List<ReviewQuestion> qs, bool selected) {
    final a = qs.fold(0, (v, q) => v + q.answerCount),
        c = qs.fold(0, (v, q) => v + q.correctCount);
    return Card(
      child: ListTile(
        title: Row(
          children: [
            Expanded(child: Text(book.title)),
            if (selected) const Chip(label: Text('使用中')),
          ],
        ),
        subtitle: Text('${qs.length}問${a>0?'  ・  正答率 ${rate(c,a)}':''}'),
        trailing: const Icon(Icons.chevron_right),
        onTap: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder:
                  (_) => QuestionBookDetailScreen(
                    bridge: widget.bridge,
                    bookId: book.id,
                  ),
            ),
          );
          await load();
        },
      ),
    );
  }

  Future<void> _showAddMenu(AppSnapshot d) async {
    await showModalBottomSheet<void>(context: context,builder:(sheet)=>SafeArea(child:Padding(padding:const EdgeInsets.fromLTRB(16,0,16,16),child:Column(mainAxisSize:MainAxisSize.min,children:[
      ListTile(leading:const Icon(Icons.create_new_folder_outlined),title:const Text('新しい冊子'),onTap:(){Navigator.pop(sheet);addBook();}),
      ListTile(leading:const Icon(Icons.playlist_add_rounded),title:const Text('問題を一括登録'),enabled:d.questionBooks.isNotEmpty,onTap:(){Navigator.pop(sheet);Navigator.push(context,MaterialPageRoute(builder:(_)=>ReviewQuestionBulkScreen(bridge:widget.bridge,books:d.questionBooks)));}),
      ListTile(leading:const Icon(Icons.auto_awesome_outlined),title:const Text('AIで問題を作る'),onTap:(){Navigator.pop(sheet);Navigator.push(context,MaterialPageRoute(builder:(_)=>AiQuestionPromptScreen(bridge:widget.bridge)));}),
    ]))));
    await load();
  }

}

class ReviewCaptchaSettingsScreen extends StatefulWidget {
  const ReviewCaptchaSettingsScreen({super.key, required this.bridge});
  final NanishiniBridge bridge;

  @override
  State<ReviewCaptchaSettingsScreen> createState() =>
      _ReviewCaptchaSettingsState();
}

class _ReviewCaptchaSettingsState extends State<ReviewCaptchaSettingsScreen> {
  AppSnapshot? data;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    final value = await widget.bridge.loadSnapshot();
    if (mounted) setState(() => data = value);
  }

  @override
  Widget build(BuildContext context) {
    final d = data;
    return Scaffold(
      appBar: AppBar(title: const Text('出題設定')),
      body:
          d == null
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  const Text(
                    '使用する問題冊子',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  if (d.questionBooks.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Text('問題冊子がありません。先に問題冊子を追加してください。'),
                    ),
                  for (final b in d.questionBooks)
                    CheckboxListTile(
                      title: Text(b.title),
                      value: d.selectedQuestionBookIds.contains(b.id),
                      onChanged: (value) async {
                        final ids = d.selectedQuestionBookIds.toSet();
                        value == true ? ids.add(b.id) : ids.remove(b.id);
                        await widget.bridge.setSelectedQuestionBookIds(ids);
                        await load();
                      },
                    ),
                  const Divider(),
                  const Text(
                    '1回のCheckpointで出題する問題数',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  for (final count in [1, 2, 3, 5])
                    RadioListTile<int>(
                      title: Text('$count問'),
                      value: count,
                      groupValue: d.reviewQuestionCount,
                      onChanged: (value) async {
                        await widget.bridge.setReviewQuestionCount(value!);
                        await load();
                      },
                    ),
                  if (d.selectedQuestionBookIds.isEmpty)
                    const Card(
                      child: Padding(
                        padding: EdgeInsets.all(16),
                        child: Text(
                          '使用する問題冊子が設定されていません。この場合も対象アプリは開けます。',
                        ),
                      ),
                    ),
                ],
              ),
    );
  }
}

class QuestionBookDetailScreen extends StatefulWidget {
  const QuestionBookDetailScreen({
    super.key,
    required this.bridge,
    required this.bookId,
  });
  final NanishiniBridge bridge;
  final String bookId;
  @override
  State<QuestionBookDetailScreen> createState() =>
      _QuestionBookDetailScreenState();
}

class _QuestionBookDetailScreenState extends State<QuestionBookDetailScreen> {
  AppSnapshot? data;
  Future<void> load() async {
    final d = await widget.bridge.loadSnapshot();
    if (mounted) setState(() => data = d);
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> editQuestion([ReviewQuestion? q]) async {
    final result = await questionDialog(context, q);
    if (result == null) return;
    if (q == null)
      await widget.bridge.addReviewQuestion(
        widget.bookId,
        result.$1,
        result.$2,
      );
    else
      await widget.bridge.updateReviewQuestion(
        q.copyWith(
          question: result.$1,
          answer: result.$2,
          isEnabled: result.$3,
        ),
      );
    await load();
  }

  @override
  Widget build(BuildContext context) {
    final d = data;
    if (d == null)
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    final book =
        d.questionBooks.where((b) => b.id == widget.bookId).firstOrNull;
    if (book == null)
      return const Scaffold(body: Center(child: Text('冊子がありません')));
    final qs = d.reviewQuestions.where((q) => q.bookId == book.id).toList(),
        a = qs.fold(0, (v, q) => v + q.answerCount),
        c = qs.fold(0, (v, q) => v + q.correctCount);
    return Scaffold(
      appBar: AppBar(
        title: Text(book.title),
        actions: [
          PopupMenuButton<String>(
            onSelected: (v) => action(v, book, qs.length),
            itemBuilder:
                (_) => const [
                  PopupMenuItem(value: 'edit', child: Text('冊子名を編集')),
                  PopupMenuItem(value: 'delete', child: Text('冊子を削除')),
                ],
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => editQuestion(),
        icon: const Icon(Icons.add),
        label: const Text('問題を追加'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
        children: [
          Text('${qs.length}問　回答 $a回　正答 $c回　正答率 ${rate(c, a)}'),
          const Divider(height: 28),
          for (final q in qs)
            Card(
              child: ListTile(
                title: Text(StructuredQuestion.from(q.question,q.answer).stem),
                subtitle: Text(
                  '正解：${q.answer}\n${q.answerCount>0?'正答率 ${rate(q.correctCount, q.answerCount)}':''}',
                ),
                isThreeLine: true,
                onTap: () => editQuestion(q),
                trailing: Row(mainAxisSize:MainAxisSize.min,children:[if(!q.isEnabled)const Chip(label:Text('無効')),IconButton(tooltip:'削除',icon: const Icon(Icons.delete_outline),onPressed: () => deleteQuestion(q))]),
              ),
            ),
          OutlinedButton.icon(
            onPressed: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(
                  builder:
                      (_) => ReviewQuestionBulkScreen(
                        bridge: widget.bridge,
                        books: d.questionBooks,
                        initialBookId: book.id,
                      ),
                ),
              );
              await load();
            },
            icon: const Icon(Icons.playlist_add),
            label: const Text('問題を一括登録'),
          ),
        ],
      ),
    );
  }

  Future<void> action(String v, QuestionBook b, int count) async {
    if (v == 'edit') {
      final s = await textDialog(context, '冊子名を編集', '冊子名', b.title);
      if (s != null) await widget.bridge.updateQuestionBook(b.id, s);
    } else {
      final ok = await confirm(
        context,
        '「${b.title}」を削除しますか？',
        'この冊子に含まれる$count問も削除されます。',
      );
      if (ok) {
        await widget.bridge.deleteQuestionBook(b.id);
        if (mounted) Navigator.pop(context);
        return;
      }
    }
    await load();
  }

  Future<void> deleteQuestion(ReviewQuestion q) async {
    if (await confirm(context, '問題を削除しますか？', q.question)) {
      await widget.bridge.deleteReviewQuestion(q.id);
      await load();
    }
  }
}

class ReviewQuestionBulkScreen extends StatefulWidget {
  const ReviewQuestionBulkScreen({
    super.key,
    required this.bridge,
    required this.books,
    this.initialBookId,
  });
  final NanishiniBridge bridge;
  final List<QuestionBook> books;
  final String? initialBookId;
  @override
  State<ReviewQuestionBulkScreen> createState() => _BulkState();
}

class _BulkState extends State<ReviewQuestionBulkScreen> {
  String input = '';
  List<ParsedReviewQuestion>? parsed;
  late String bookId = widget.initialBookId ?? widget.books.first.id;

  @override
  Widget build(BuildContext context) {
    final valid = parsed?.where((e) => e.isValid).toList() ?? [];
    return Scaffold(
      appBar: AppBar(title: const Text('問題を一括登録')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          DropdownButtonFormField(
            value: bookId,
            decoration: const InputDecoration(labelText: '追加先の冊子'),
            items: [
              for (final b in widget.books)
                DropdownMenuItem(value: b.id, child: Text(b.title)),
            ],
            onChanged: (v) => setState(() => bookId = v!),
          ),
          const SizedBox(height: 12),
          TextField(
            minLines: 10,
            maxLines: 18,
            onChanged: (value) => input = value,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              hintText: '問 1+1\n答 2',
            ),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed:
                () =>
                    setState(() => parsed = ReviewQuestionParser.parse(input)),
            child: const Text('解析してプレビュー'),
          ),
          if (parsed != null) ...[
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Text('${parsed!.length}問を検出しました（登録可能 ${valid.length}問）'),
            ),
            for (final p in parsed!)
              ListTile(
                leading: Icon(
                  p.isValid ? Icons.check_circle : Icons.warning,
                  color: p.isValid ? Colors.green : Colors.orange,
                ),
                title: Text(p.question),
                subtitle: Text(p.isValid ? '答：${p.answer}' : '正解がありません'),
              ),
            FilledButton(
              onPressed:
                  valid.isEmpty
                      ? null
                      : () async {
                        final now = DateTime.now().millisecondsSinceEpoch;
                        await widget.bridge.addReviewQuestions(bookId, [
                          for (final p in valid)
                            ReviewQuestion(
                              id: '',
                              bookId: bookId,
                              question: p.question,
                              answer: p.answer!,
                              createdAt: now,
                            ),
                        ]);
                        if (mounted) Navigator.pop(context);
                      },
              child: Text('${valid.length}問を登録'),
            ),
          ],
        ],
      ),
    );
  }
}

class AiQuestionPromptScreen extends StatefulWidget {
  const AiQuestionPromptScreen({super.key, required this.bridge});
  final NanishiniBridge bridge;
  @override
  State<AiQuestionPromptScreen> createState() => _AiState();
}

class _AiState extends State<AiQuestionPromptScreen> {
  int count = 5;
  String topic = '';
  String instruction = '';
  String get prompt => '''以下の内容から復習問題を作成してください。

問題にする内容：
$topic

問題数：$count問

以下の三択形式を厳密に守って出力してください。

問 問題文
選択肢A 選択肢1
選択肢B 選択肢2
選択肢C 選択肢3
答 正解の選択肢本文

【重要】
1問につき必ず5行で出力してください。

1行目：
「問 半角スペース 問題文」

2〜4行目：
「選択肢A 半角スペース 選択肢」「選択肢B 半角スペース 選択肢」「選択肢C 半角スペース 選択肢」

5行目：
「答 半角スペース 正解の選択肢本文」

「問」と問題文は必ず同じ行にしてください。
正解はA/B/Cの記号ではなく、該当する選択肢の本文をそのまま書いてください。

正解の位置はA・B・Cに偏らないようランダムに配置してください。
誤答は明確に不正解でありつつ、もっともらしい内容にしてください。
選択肢同士が重複しないようにしてください。
問題と問題の間に空行は入れないでください。

正しい形式：
問 日本の首都はどこですか。
選択肢A 東京
選択肢B 大阪
選択肢C 京都
答 東京
問 1+1はいくつですか。
選択肢A 1
選択肢B 3
選択肢C 2
答 2

以下の形式は禁止です。

問 問題文 答 正解

また、以下の形式も禁止です。

問
問題文
答
正解

説明文、番号、見出し、Markdownコードブロック、補足文章、前置き、後書きは一切出力しないでください。

Checkpointへそのまま貼り付けて一括登録するため、出力には問題データ以外を含めないでください。

1問につき正解は1つにしてください。
数秒〜20秒程度で回答できる復習向けの問題にしてください。

追加条件：
$instruction

出力前に内部で以下を確認してください。
・問題数が$count問である
・各問題が必ず5行である
・各問にA、B、Cの3つの選択肢がある
・答が選択肢のいずれかと完全に一致する
・正解位置が偏っていない
・空行が存在しない

確認結果は出力せず、問題だけを出力してください。''';

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('AIで問題を作る')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text('ChatGPTなどのAIにこのプロンプトを送ると、一括登録できる形式で問題を作成できます。'),
        DropdownButtonFormField(
          value: count,
          decoration: const InputDecoration(labelText: '問題数'),
          items:
              [5, 10, 20, 30]
                  .map((v) => DropdownMenuItem(value: v, child: Text('$v問')))
                  .toList(),
          onChanged: (v) => setState(() => count = v!),
        ),
        const SizedBox(height: 12),
        TextField(
          minLines: 3,
          maxLines: 8,
          onChanged: (value) => setState(() => topic = value),
          decoration: const InputDecoration(
            labelText: '問題にする内容（必須）',
            hintText: '例：日本史の鎌倉時代について',
            alignLabelWithHint: true,
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          maxLines: 4,
          onChanged: (value) => setState(() => instruction = value),
          decoration: const InputDecoration(labelText: '追加指示'),
        ),
        const SizedBox(height: 16),
        SelectableText(prompt),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed:
              topic.trim().isEmpty
                  ? null
                  : () async {
                    await Clipboard.setData(ClipboardData(text: prompt));
                    if (mounted)
                      ScaffoldMessenger.of(
                        context,
                      ).showSnackBar(const SnackBar(content: Text('コピーしました')));
                  },
          icon: const Icon(Icons.copy),
          label: const Text('プロンプトをコピー'),
        ),
      ],
    ),
  );
}

Future<String?> textDialog(
  BuildContext c,
  String title,
  String label, [
  String initial = '',
]) => showDialog<String>(
  context: c,
  builder:
      (_) => _TextInputDialog(title: title, label: label, initial: initial),
);
Future<bool> confirm(BuildContext c, String title, String body) async =>
    (await showDialog<bool>(
      context: c,
      builder:
          (d) => AlertDialog(
            title: Text(title),
            content: Text(body),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(d, false),
                child: const Text('キャンセル'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(d, true),
                child: const Text('削除'),
              ),
            ],
          ),
    )) ??
    false;
Future<(String, String, bool)?> questionDialog(
  BuildContext c,
  ReviewQuestion? q,
) => showDialog<(String, String, bool)>(
  context: c,
  builder: (_) => _QuestionInputDialog(question: q),
);

class _TextInputDialog extends StatefulWidget {
  const _TextInputDialog({
    required this.title,
    required this.label,
    required this.initial,
  });
  final String title, label, initial;
  @override
  State<_TextInputDialog> createState() => _TextInputDialogState();
}

class _TextInputDialogState extends State<_TextInputDialog> {
  late String value;
  @override
  void initState() {
    super.initState();
    value = widget.initial;
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(widget.title),
    content: TextFormField(
      initialValue: widget.initial,
      onChanged: (text) => value = text,
      autofocus: true,
      decoration: InputDecoration(labelText: widget.label),
      keyboardType: TextInputType.text,
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('キャンセル'),
      ),
      FilledButton(
        onPressed:
            () =>
                value.trim().isEmpty
                    ? null
                    : Navigator.pop(context, value.trim()),
        child: const Text('保存'),
      ),
    ],
  );
}

class _QuestionInputDialog extends StatefulWidget {
  const _QuestionInputDialog({required this.question});
  final ReviewQuestion? question;
  @override
  State<_QuestionInputDialog> createState() => _QuestionInputDialogState();
}

class _QuestionInputDialogState extends State<_QuestionInputDialog> {
  late String question, choiceA, choiceB, choiceC;
  late int correctChoice;
  late bool enabled;
  @override
  void initState() {
    super.initState();
    final parsed = StructuredQuestion.from(widget.question?.question ?? '', widget.question?.answer ?? '');
    question = parsed.stem; choiceA=parsed.a; choiceB=parsed.b; choiceC=parsed.c; correctChoice=parsed.correctIndex;
    enabled = widget.question?.isEnabled ?? true;
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(widget.question == null ? '問題を追加' : '問題を編集'),
    content: SingleChildScrollView(child:Column(mainAxisSize: MainAxisSize.min,children: [
        TextFormField(
          initialValue: question,
          onChanged: (value) => question = value,
          maxLines: 3,
          decoration: const InputDecoration(
            labelText: '問題文',
          ),
        ),
        const SizedBox(height:10),
        for(final entry in [('A',choiceA),('B',choiceB),('C',choiceC)])Padding(padding:const EdgeInsets.only(bottom:10),child:TextFormField(initialValue:entry.$2,onChanged:(v){if(entry.$1=='A')choiceA=v;else if(entry.$1=='B')choiceB=v;else choiceC=v;},decoration:InputDecoration(labelText:'選択肢${entry.$1}'))),
        const Align(alignment:Alignment.centerLeft,child:Text('正解')),
        SegmentedButton<int>(segments:const[ButtonSegment(value:0,label:Text('A')),ButtonSegment(value:1,label:Text('B')),ButtonSegment(value:2,label:Text('C'))],selected:{correctChoice},onSelectionChanged:(v)=>setState(()=>correctChoice=v.first)),
        SwitchListTile(
          value: enabled,
          onChanged: (value) => setState(() => enabled = value),
          title: const Text('有効'),
        ),
      ])),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('キャンセル'),
      ),
      FilledButton(
        onPressed:
            () =>
                [question,choiceA,choiceB,choiceC].any((v)=>v.trim().isEmpty)
                    ? null
                    : Navigator.pop(context, (
                      '${question.trim()}\n選択肢A ${choiceA.trim()}\n選択肢B ${choiceB.trim()}\n選択肢C ${choiceC.trim()}',
                      [choiceA,choiceB,choiceC][correctChoice].trim(),
                      enabled,
                    )),
        child: const Text('保存'),
      ),
    ],
  );
}

class StructuredQuestion {
  const StructuredQuestion(this.stem,this.a,this.b,this.c,this.correctIndex);
  final String stem,a,b,c; final int correctIndex;
  static StructuredQuestion from(String raw,String answer){
    final options=<String>[];final stem=<String>[];final re=RegExp(r'^(?:選択肢\s*)?[A-CＡ-Ｃ][.．、:：)）\s]+(.+)$');
    for(final line in raw.split('\n')){final m=re.firstMatch(line.trim());if(m==null)stem.add(line);else options.add(m.group(1)!.trim());}
    if(options.length!=3)return StructuredQuestion(raw,'','','',0);
    final index=options.indexWhere((v)=>v.trim()==answer.trim());return StructuredQuestion(stem.join('\n').trim(),options[0],options[1],options[2],index<0?0:index);
  }
}
