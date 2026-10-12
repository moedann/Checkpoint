import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:nanisini/question_engine/question_engine.dart';
import 'package:nanisini/question_engine/repository.dart';

void main() {
  const codec = QuestionPackCodec();
  final source =
      File('examples/all-image-examples-plus-self.cpq.json').readAsStringSync();
  final pack = codec.decode(source);
  Question question(String id) => pack.questions.firstWhere((q) => q.id == id);
  Map<String, dynamic> raw() => jsonDecode(source) as Map<String, dynamic>;
  Question changed(String id, void Function(Map<String, dynamic>) edit) {
    final data = raw();
    edit(
      (data['questions'] as List).cast<Map<String, dynamic>>().firstWhere(
        (q) => q['id'] == id,
      ),
    );
    return codec
        .decode(jsonEncode(data))
        .questions
        .firstWhere((q) => q.id == id);
  }

  void rejects(void Function(Map<String, dynamic>) edit, String code) {
    final data = raw();
    edit(data);
    expect(
      () => codec.decode(jsonEncode(data)),
      throwsA(
        isA<PackValidationException>().having(
          (e) => e.issues.map((i) => i.code),
          'issue codes',
          contains(code),
        ),
      ),
    );
  }

  final correct = <String, QuestionAnswer>{
    'Q01': const SingleChoiceAnswer('a'),
    'Q02': MultipleChoiceAnswer(['hydro', 'solar', 'wind']),
    'Q03': InputAnswer({'capital': '東京'}),
    'Q03B': InputAnswer({'calculation': '56'}),
    'Q04': InputAnswer({
      gridCellKey('sales', 'mar'): '150000',
      gridCellKey('cost', 'mar'): '90000',
      gridCellKey('profit', 'mar'): '60000',
    }),
    'Q04B': InputAnswer({
      gridCellKey('A', 'amount'): '120000',
      gridCellKey('B', 'amount'): '300000',
      gridCellKey('C', 'amount'): '50000',
    }),
    'Q05': OrderingAnswer(['planning', 'design', 'production', 'sales']),
    'Q06': AssignmentAnswer({
      'apple': 'fruit',
      'mandarin': 'fruit',
      'cucumber': 'vegetable',
      'tomato': 'vegetable',
    }),
    'Q07': AssignmentAnswer({'A': 'tokyo', 'B': 'osaka'}),
    'Q08': const SelfReviewAnswer(
      attempted: true,
      rating: SelfRating.incorrect,
    ),
  };
  test('sample has 10 questions, seven types and both interaction modes', () {
    expect(pack.questions.length, 10);
    expect(pack.questions.map((q) => q.type).toSet().length, 7);
    expect(question('Q05').data['mode'], 'ordering');
    expect(question('Q06').data['mode'], 'grouping');
  });
  for (final entry in correct.entries) {
    test(
      'fixture ${entry.key}: correct answer, independently of self rating',
      () {
        final r = gradeQuestion(question(entry.key), entry.value);
        expect(r.canSubmit, true, reason: r.issues.toString());
        expect(
          r.grade,
          entry.key == 'Q08' ? Grade.selfReviewed : Grade.correct,
        );
      },
    );
  }
  test('round trip preserves IDs, answer keys and all metadata', () {
    expect(jsonDecode(codec.encode(pack)), jsonDecode(source));
    expect(codec.decode(codec.encode(pack)).toJson(), pack.toJson());
  });
  test('validated model is deeply immutable and toJson is a detached copy', () {
    expect(() => pack.data['title'] = 'changed', throwsUnsupportedError);
    expect(
      () => question('Q01').data['options'][0]['id'] = 'x',
      throwsUnsupportedError,
    );
    final copy = pack.toJson();
    copy['questions'][0]['answerKey']['optionId'] = 'b';
    expect(question('Q01').data['answerKey']['optionId'], 'a');
  });
  test(
    'choice order never changes grading and unselected answer cannot submit',
    () {
      final q = changed(
        'Q01',
        (q) => q['options'] = (q['options'] as List).reversed.toList(),
      );
      expect(
        gradeQuestion(q, const SingleChoiceAnswer('a')).grade,
        Grade.correct,
      );
      expect(
        gradeQuestion(q, const SingleChoiceAnswer('b')).grade,
        Grade.incorrect,
      );
      expect(gradeQuestion(q, const SingleChoiceAnswer(null)).canSubmit, false);
      expect(
        gradeQuestion(q, const SingleChoiceAnswer('missing')).canSubmit,
        false,
      );
    },
  );
  test('multiple: order independent, partial/excess/empty/duplicates', () {
    final q = question('Q02');
    expect(
      gradeQuestion(q, MultipleChoiceAnswer(['wind', 'solar', 'hydro'])).grade,
      Grade.correct,
    );
    expect(
      gradeQuestion(q, MultipleChoiceAnswer(['wind'])).grade,
      Grade.partial,
    );
    expect(
      gradeQuestion(q, MultipleChoiceAnswer(['oil'])).grade,
      Grade.incorrect,
    );
    final extra = gradeQuestion(
      q,
      MultipleChoiceAnswer(['wind', 'solar', 'hydro', 'oil']),
    );
    expect(extra.grade, Grade.partial);
    expect(extra.details.firstWhere((d) => d.id == 'oil').correct, false);
    expect(gradeQuestion(q, MultipleChoiceAnswer([])).canSubmit, false);
    expect(
      gradeQuestion(q, MultipleChoiceAnswer(['wind', 'wind'])).canSubmit,
      false,
    );
    final limited = changed('Q02', (q) => q['maxSelections'] = 3);
    expect(
      gradeQuestion(
        limited,
        MultipleChoiceAnswer(['wind', 'solar', 'hydro', 'oil']),
      ).canSubmit,
      false,
    );
  });
  test('text: true NFKC, trim, aliases and explicit case rules', () {
    var q = changed('Q03', (q) {
      q['fields'][0]['acceptedAnswers'] = ['ガ', 'Tokyo'];
    });
    expect(
      gradeQuestion(q, InputAnswer({'capital': '　ｶﾞ　'})).grade,
      Grade.correct,
    );
    expect(
      gradeQuestion(q, InputAnswer({'capital': 'tokyo'})).grade,
      Grade.incorrect,
    );
    q = changed('Q03', (q) {
      q['fields'][0]['acceptedAnswers'] = ['Tokyo'];
      q['fields'][0]['caseSensitive'] = false;
    });
    expect(
      gradeQuestion(q, InputAnswer({'capital': 'ｔｏｋｙｏ'})).grade,
      Grade.correct,
    );
    q = changed('Q03', (q) {
      q['fields'][0]['acceptedAnswers'] = ['ガ'];
      q['fields'][0]['normalizeNFKC'] = false;
    });
    expect(
      gradeQuestion(q, InputAnswer({'capital': 'ｶﾞ'})).grade,
      Grade.incorrect,
    );
    q = changed('Q03', (q) {
      q['fields'][0]['trimWhitespace'] = false;
    });
    expect(
      gradeQuestion(q, InputAnswer({'capital': ' 東京 '})).grade,
      Grade.incorrect,
    );
  });
  test('number: zero prefix, decimal, NFKC, negative, finite and units', () {
    final q = question('Q03B');
    for (final s in ['56', '056', '56.0', '５６']) {
      expect(
        gradeQuestion(q, InputAnswer({'calculation': s})).grade,
        Grade.correct,
      );
    }
    for (final s in ['NaN', 'Infinity', '1e309', '5,6', '56cm', '']) {
      expect(
        gradeQuestion(q, InputAnswer({'calculation': s})).canSubmit,
        false,
      );
    }
    final unit = changed('Q03B', (q) {
      q['fields'][0]['expectedValue'] = -56;
      q['fields'][0]['unit'] = 'kg';
    });
    expect(
      gradeQuestion(unit, InputAnswer({'calculation': '-56 kg'})).grade,
      Grade.correct,
    );
    expect(
      gradeQuestion(unit, InputAnswer({'calculation': '-56cm'})).canSubmit,
      false,
    );
  });
  test('absolute/relative tolerance uses max, no hidden rounding', () {
    final q = changed('Q03B', (q) {
      q['fields'][0]['expectedValue'] = 100;
      q['fields'][0]['absoluteTolerance'] = 1;
      q['fields'][0]['relativeTolerance'] = 0.02;
    });
    expect(
      gradeQuestion(q, InputAnswer({'calculation': '102'})).grade,
      Grade.correct,
    );
    expect(
      gradeQuestion(q, InputAnswer({'calculation': '102.001'})).grade,
      Grade.incorrect,
    );
    final zero = changed('Q03B', (q) {
      q['fields'][0]['expectedValue'] = 0;
      q['fields'][0]['relativeTolerance'] = 0.1;
    });
    expect(
      gradeQuestion(zero, InputAnswer({'calculation': '0.001'})).grade,
      Grade.incorrect,
    );
  });
  test('grid: per-cell result, partial/incorrect and unknown/static cells', () {
    final values = Map<String, String>.from(
      (correct['Q04'] as InputAnswer).values,
    );
    values[gridCellKey('profit', 'mar')] = '0';
    final r = gradeQuestion(question('Q04'), InputAnswer(values));
    expect(r.grade, Grade.partial);
    expect(r.details.length, 3);
    expect(r.details.last.correct, false);
    expect(
      gradeQuestion(
        question('Q04'),
        InputAnswer(values.map((k, v) => MapEntry(k, '0'))),
      ).grade,
      Grade.incorrect,
    );
    values[gridCellKey('sales', 'jan')] = '100000';
    expect(
      gradeQuestion(question('Q04'), InputAnswer(values)).canSubmit,
      false,
    );
    expect(gradeQuestion(question('Q04'), InputAnswer({})).canSubmit, false);
  });
  test(
    'ordering/grouping rejects missing IDs and gives partial or wrong grades',
    () {
      expect(
        gradeQuestion(
          question('Q05'),
          OrderingAnswer(['design', 'planning', 'production', 'sales']),
        ).grade,
        Grade.partial,
      );
      expect(
        gradeQuestion(
          question('Q05'),
          OrderingAnswer(['sales', 'production', 'design', 'planning']),
        ).grade,
        Grade.incorrect,
      );
      expect(
        gradeQuestion(
          question('Q05'),
          OrderingAnswer(['planning', 'planning', 'sales', 'production']),
        ).canSubmit,
        false,
      );
      expect(
        gradeQuestion(
          question('Q06'),
          AssignmentAnswer({'apple': 'fruit'}),
        ).canSubmit,
        false,
      );
      expect(
        gradeQuestion(
          question('Q06'),
          AssignmentAnswer({
            'apple': 'fruit',
            'mandarin': 'fruit',
            'cucumber': 'fruit',
            'tomato': 'vegetable',
          }),
        ).grade,
        Grade.partial,
      );
      expect(
        gradeQuestion(
          question('Q06'),
          AssignmentAnswer({
            'apple': 'vegetable',
            'mandarin': 'vegetable',
            'cucumber': 'fruit',
            'tomato': 'fruit',
          }),
        ).grade,
        Grade.incorrect,
      );
    },
  );
  test('word bank IDs preserve duplicate labels and token reuse policy', () {
    final q = changed('Q07', (q) {
      q['tokens'][1]['label'] = '東京';
    });
    expect(
      gradeQuestion(q, AssignmentAnswer({'A': 'osaka', 'B': 'tokyo'})).grade,
      Grade.incorrect,
    );
    expect(
      gradeQuestion(
        q,
        AssignmentAnswer({'A': 'tokyo', 'B': 'tokyo'}),
      ).canSubmit,
      false,
    );
    final reuse = changed('Q07', (q) {
      q['reuseTokens'] = true;
      q['answerKey']['matches'][1]['tokenId'] = 'tokyo';
    });
    expect(
      gradeQuestion(
        reuse,
        AssignmentAnswer({'A': 'tokyo', 'B': 'tokyo'}),
      ).grade,
      Grade.correct,
    );
    expect(gradeQuestion(q, AssignmentAnswer({'A': 'tokyo'})).canSubmit, false);
  });
  test('self review: attempted != automatic correct, no unlock property', () {
    expect(
      gradeQuestion(
        question('Q08'),
        const SelfReviewAnswer(attempted: false),
      ).canSubmit,
      false,
    );
    expect(
      gradeQuestion(
        question('Q08'),
        const SelfReviewAnswer(attempted: true),
      ).grade,
      Grade.ungraded,
    );
    expect(
      gradeQuestion(question('Q08'), const SingleChoiceAnswer('a')).canSubmit,
      false,
    );
  });
  test(
    'broken JSON, version/type/extra properties and empty required fields',
    () {
      expect(() => codec.decode('{'), throwsA(isA<PackValidationException>()));
      rejects((p) => p['formatVersion'] = '2.0.0', 'const');
      rejects((p) => p['questions'][0]['type'] = 'html', 'unknown_type');
      rejects(
        (p) => p['questions'][0]['url'] = 'https://example.org',
        'unknown_property',
      );
      rejects((p) => p['questions'][0].remove('explanation'), 'required');
      rejects((p) => p['questions'][0]['explanation'] = '   ', 'blank');
      rejects((p) => p['packId'] = '../escape', 'invalid_id');
      rejects((p) => p['packId'] = 'valid\n', 'invalid_id');
    },
  );
  test('duplicate IDs, missing key and invalid selection bounds', () {
    rejects((p) => p['questions'][1]['id'] = 'Q01', 'duplicate_id');
    rejects((p) => p['questions'][0]['options'][1]['id'] = 'a', 'duplicate_id');
    rejects(
      (p) => p['questions'][0]['answerKey']['optionId'] = 'z',
      'unknown_reference',
    );
    rejects((p) => p['questions'][1]['maxSelections'] = 1, 'selection_bounds');
  });
  test('grid/ordering/grouping/word-bank logical consistency', () {
    rejects(
      (p) => p['questions'][4]['rows'][0]['cells'][0]['columnId'] = 'bad',
      'incomplete_mapping',
    );
    rejects(
      (p) =>
          p['questions'][6]['answerKey']['orderedIds'] = ['planning', 'design'],
      'incomplete_mapping',
    );
    rejects((p) => p['questions'][7].remove('groups'), 'grouping_key');
    rejects(
      (p) =>
          p['questions'][7]['answerKey']['assignments'][0]['groupId'] = 'bad',
      'unknown_reference',
    );
    rejects(
      (p) => p['questions'][8]['answerKey']['matches'][1]['blankId'] = 'A',
      'incomplete_mapping',
    );
    rejects(
      (p) => p['questions'][8]['answerKey']['matches'][1]['tokenId'] = 'tokyo',
      'token_reuse',
    );
  });
  test(
    'unimplemented assets, oversized JSON and excessive nesting are rejected',
    () {
      rejects(
        (p) =>
            p['questions'][0]['stimulus'] = [
              {'kind': 'image', 'assetId': 'img', 'alt': 'image'},
            ],
        'unsupported_asset',
      );
      expect(
        () => const QuestionPackCodec(maxBytes: 10).decode(source),
        throwsA(isA<PackValidationException>()),
      );
      expect(
        () => const QuestionPackCodec(maxQuestions: 9).decode(source),
        throwsA(isA<PackValidationException>()),
      );
      expect(
        () => codec.decode(
          '${List.filled(33, '[').join()}0${List.filled(33, ']').join()}',
        ),
        throwsA(isA<PackValidationException>()),
      );
    },
  );

  group('local repository', () {
    late Directory dir;
    late QuestionPackRepository repo;
    setUp(() async {
      dir = await Directory.systemTemp.createTemp('checkpoint-pack-test-');
      repo = QuestionPackRepository(dir);
    });
    tearDown(() async {
      await dir.delete(recursive: true);
    });
    test('preview is read only; save/load preserves every key', () async {
      final preview = await repo.preview(
        File('examples/all-image-examples-plus-self.cpq.json'),
      );
      expect(await repo.load(preview.packId), null);
      await repo.save(preview);
      expect((await repo.load(pack.packId))!.toJson(), pack.toJson());
    });
    test(
      'duplicate/stale imports preserve original; explicit new revision replaces',
      () async {
        await repo.save(pack);
        await expectLater(repo.save(pack), throwsStateError);
        var data = raw();
        data['revision'] = 2;
        data['title'] = 'new';
        final updated = codec.decode(jsonEncode(data));
        await expectLater(
          repo.save(updated, expectedRevision: 0),
          throwsStateError,
        );
        expect((await repo.load(pack.packId))!.title, pack.title);
        await repo.save(updated, expectedRevision: 1);
        expect((await repo.load(pack.packId))!.revision, 2);
      },
    );
    test('concurrent imports across instances allow one write only', () async {
      final second = QuestionPackRepository(dir);
      final results = await Future.wait(
        [
          repo.save(pack),
          second.save(pack),
        ].map((f) => f.then((_) => true, onError: (Object e) => false)),
      );
      expect(results.where((v) => v).length, 1);
      expect((await repo.load(pack.packId))!.toJson(), pack.toJson());
    });
    test(
      'failed bounded/UTF8 reads and bad IDs do not change saved pack',
      () async {
        await repo.save(pack);
        final broken = File('${dir.path}/broken.json');
        await broken.writeAsBytes([0xff]);
        await expectLater(
          repo.preview(broken),
          throwsA(isA<PackValidationException>()),
        );
        await expectLater(repo.load('../escape'), throwsArgumentError);
        expect((await repo.load(pack.packId))!.toJson(), pack.toJson());
      },
    );
    test('maximum-length and colon IDs work as safe filenames', () async {
      final data = raw();
      data['packId'] = 'A${List.filled(126, 'a').join()}:';
      final long = codec.decode(jsonEncode(data));
      await repo.save(long);
      expect((await repo.load(long.packId))!.packId, long.packId);
    });
  });
}
