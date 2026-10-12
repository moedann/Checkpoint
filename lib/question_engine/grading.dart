part of 'question_engine.dart';

/// Pure grading. A result says whether an answer can be submitted; it never
/// authorizes app access or represents review acknowledgement.
GradeResult gradeQuestion(Question question, QuestionAnswer answer) {
  final q = question.data;
  final issues = <ValidationIssue>[];
  final details = <GradeDetail>[];
  void invalid(String path, String code, String message) =>
      _issue(issues, path, code, message);
  GradeResult finish([Grade? override]) {
    if (issues.isNotEmpty) return GradeResult(Grade.ungraded, issues: issues);
    final count = details.where((d) => d.correct).length;
    return GradeResult(
      override ??
          (count == details.length
              ? Grade.correct
              : count > 0
              ? Grade.partial
              : Grade.incorrect),
      details: details,
    );
  }

  switch (question.type) {
    case 'single_choice':
      if (answer is! SingleChoiceAnswer) {
        invalid('answer', 'answer_type', '単一選択の回答が必要です');
        break;
      }
      final valid = _ids(q['options'] as List, 'id');
      if (answer.optionId == null) {
        invalid('answer.optionId', 'not_answered', '選択してください');
        break;
      }
      if (!valid.contains(answer.optionId)) {
        invalid('answer.optionId', 'unknown_reference', '選択肢IDがありません');
        break;
      }
      details.add(
        GradeDetail(
          id: question.id,
          correct: answer.optionId == q['answerKey']['optionId'],
          expected: q['answerKey']['optionId'],
          actual: answer.optionId,
        ),
      );
    case 'multiple_choice':
      if (answer is! MultipleChoiceAnswer) {
        invalid('answer', 'answer_type', '複数選択の回答が必要です');
        break;
      }
      final ids = answer.optionIds.toSet(),
          valid = _ids(q['options'] as List, 'id');
      final expected =
          (q['answerKey']['optionIds'] as List).cast<String>().toSet();
      final min = (q['minSelections'] as num?) ?? 1,
          max = (q['maxSelections'] as num?) ?? valid.length;
      if (ids.length != answer.optionIds.length || !valid.containsAll(ids)) {
        invalid('answer.optionIds', 'invalid_selection', '選択肢IDが不正または重複しています');
      }
      if (ids.length < min || ids.length > max) {
        invalid('answer.optionIds', 'selection_bounds', '選択数を確認してください');
      }
      for (final id in valid) {
        details.add(
          GradeDetail(
            id: id,
            correct: ids.contains(id) == expected.contains(id),
            expected: expected.contains(id),
            actual: ids.contains(id),
          ),
        );
      }
      final exact = ids.length == expected.length && ids.containsAll(expected);
      return finish(
        exact
            ? Grade.correct
            : ids.intersection(expected).isNotEmpty
            ? Grade.partial
            : Grade.incorrect,
      );
    case 'entry':
    case 'grid':
      if (answer is! InputAnswer) {
        invalid('answer', 'answer_type', '入力回答が必要です');
        break;
      }
      final fields = <String, Map<String, dynamic>>{};
      if (question.type == 'entry') {
        for (final f in q['fields'] as List) {
          fields[f['id'] as String] = f as Map<String, dynamic>;
        }
      } else {
        for (final row in q['rows'] as List) {
          for (final c in row['cells'] as List) {
            if (c['mode'] != 'static') {
              fields[gridCellKey(
                    row['id'] as String,
                    c['columnId'] as String,
                  )] =
                  c as Map<String, dynamic>;
            }
          }
        }
      }
      if (!fields.keys.toSet().containsAll(answer.values.keys)) {
        invalid('answer', 'unknown_reference', '未定義のフィールドがあります');
      }
      for (final e in fields.entries) {
        final raw = answer.values[e.key];
        if (raw == null || raw.trim().isEmpty) {
          invalid('answer.${e.key}', 'not_answered', '回答を入力してください');
          continue;
        }
        final f = e.value;
        bool correct;
        Object expected;
        if (f['mode'] == 'text') {
          String normalize(String s) {
            if (f['normalizeNFKC'] != false) s = unorm.nfkc(s);
            if (f['trimWhitespace'] != false) s = s.trim();
            if (f['caseSensitive'] == false) s = s.toLowerCase();
            return s;
          }

          expected = f['acceptedAnswers'];
          correct = (f['acceptedAnswers'] as List)
              .cast<String>()
              .map(normalize)
              .contains(normalize(raw));
        } else {
          var text = unorm.nfkc(raw).trim();
          final unit = f['unit'] as String?;
          if (unit != null && unit.isNotEmpty) {
            final suffix = unorm.nfkc(unit);
            if (text.endsWith(suffix)) {
              text = text.substring(0, text.length - suffix.length).trim();
            }
          }
          // v1 uses dot decimal, no implicit thousands separator or conversion.
          final number =
              RegExp(
                    r'^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$',
                  ).hasMatch(text)
                  ? double.tryParse(text)
                  : null;
          if (number == null || !number.isFinite) {
            invalid('answer.${e.key}', 'invalid_number', '数値・単位の書式を確認してください');
            continue;
          }
          final target = (f['expectedValue'] as num).toDouble();
          final tolerance = math.max(
            (f['absoluteTolerance'] as num?)?.toDouble() ?? 0,
            ((f['relativeTolerance'] as num?)?.toDouble() ?? 0) * target.abs(),
          );
          // Nonfinite tolerance products must not make all values correct.
          final delta = (number - target).abs();
          correct = delta.isFinite && delta <= tolerance && tolerance.isFinite;
          expected = target;
        }
        details.add(
          GradeDetail(
            id: e.key,
            correct: correct,
            expected: expected,
            actual: raw,
          ),
        );
      }
    case 'interaction':
      final items = _ids(q['items'] as List, 'id');
      if (q['mode'] == 'ordering') {
        if (answer is! OrderingAnswer) {
          invalid('answer', 'answer_type', '並べ替えの回答が必要です');
          break;
        }
        final ids = answer.ids;
        if (ids.length != items.length ||
            ids.toSet().length != ids.length ||
            !items.containsAll(ids)) {
          invalid(
            'answer.orderedIds',
            'incomplete_mapping',
            'すべての項目を1回ずつ配置してください',
          );
          break;
        }
        final expected = (q['answerKey']['orderedIds'] as List).cast<String>();
        for (var i = 0; i < ids.length; i++) {
          details.add(
            GradeDetail(
              id: '$i',
              correct: ids[i] == expected[i],
              expected: expected[i],
              actual: ids[i],
            ),
          );
        }
      } else {
        if (answer is! AssignmentAnswer) {
          invalid('answer', 'answer_type', '分類の回答が必要です');
          break;
        }
        _sameSet(answer.values.keys, items, 'answer', issues);
        final groups = _ids(q['groups'] as List, 'id');
        if (!groups.containsAll(answer.values.values)) {
          invalid('answer', 'unknown_reference', 'グループIDがありません');
        }
        for (final a in q['answerKey']['assignments'] as List) {
          details.add(
            GradeDetail(
              id: a['itemId'] as String,
              correct: answer.values[a['itemId']] == a['groupId'],
              expected: a['groupId'],
              actual: answer.values[a['itemId']],
            ),
          );
        }
      }
    case 'word_bank':
      if (answer is! AssignmentAnswer) {
        invalid('answer', 'answer_type', '語群選択の回答が必要です');
        break;
      }
      final blanks = _ids(
        (q['segments'] as List).where((s) => s['kind'] == 'blank').toList(),
        'id',
      );
      _sameSet(answer.values.keys, blanks, 'answer', issues);
      final tokens = _ids(q['tokens'] as List, 'id');
      if (!tokens.containsAll(answer.values.values)) {
        invalid('answer', 'unknown_reference', '語群IDがありません');
      }
      if (q['reuseTokens'] != true &&
          answer.values.values.toSet().length != answer.values.length) {
        invalid('answer', 'token_reuse', '同じ語群IDは再利用できません');
      }
      for (final m in q['answerKey']['matches'] as List) {
        details.add(
          GradeDetail(
            id: m['blankId'] as String,
            correct: answer.values[m['blankId']] == m['tokenId'],
            expected: m['tokenId'],
            actual: answer.values[m['blankId']],
          ),
        );
      }
    case 'self_review':
      if (answer is! SelfReviewAnswer) {
        invalid('answer', 'answer_type', '自己採点の回答が必要です');
        break;
      }
      if (!answer.attempted) {
        invalid('answer.attempted', 'not_answered', '取り組んだことを確認してください');
      }
      return finish(
        answer.rating == null ? Grade.ungraded : Grade.selfReviewed,
      );
  }
  return finish();
}
