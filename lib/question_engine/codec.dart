part of 'question_engine.dart';

/// Offline UTF-8 JSON codec. No network, asset loading, code or expression eval.
class QuestionPackCodec {
  const QuestionPackCodec({
    this.maxBytes = 10 * 1024 * 1024,
    this.maxQuestions = 2000,
  });
  final int maxBytes;
  final int maxQuestions;
  QuestionPack decode(String source) {
    if (source.length > maxBytes || utf8.encode(source).length > maxBytes) {
      _fail(r'$', 'size_limit', 'JSONの容量上限を超えています');
    }
    // Check nesting before jsonDecode (not after allocating a deeply nested tree).
    var depth = 0;
    var quoted = false;
    var escaped = false;
    for (final c in source.codeUnits) {
      if (quoted) {
        if (escaped) {
          escaped = false;
        } else if (c == 92) {
          escaped = true;
        } else if (c == 34) {
          quoted = false;
        }
      } else if (c == 34) {
        quoted = true;
      } else if (c == 91 || c == 123) {
        if (++depth > 32) _fail(r'$', 'depth_limit', 'JSONの入れ子が深すぎます');
      } else if (c == 93 || c == 125) {
        depth--;
      }
    }
    Object? value;
    try {
      value = jsonDecode(source);
    } on FormatException {
      _fail(r'$', 'invalid_json', 'JSONを読み込めません');
    }
    if (value is Map &&
        value['questions'] is List &&
        (value['questions'] as List).length > maxQuestions) {
      _fail(r'$.questions', 'question_limit', '問題数の上限を超えています');
    }
    final issues = <ValidationIssue>[];
    _validateSchema(value, _packSchema, r'$', issues);
    if (issues.isNotEmpty) throw PackValidationException(issues);
    final data = value as Map<String, dynamic>;
    final questions = data['questions'] as List;
    if (questions.length > maxQuestions) {
      _fail(r'$.questions', 'question_limit', '問題数の上限を超えています');
    }
    _uniqueIds(questions, r'$.questions', issues);
    for (var i = 0; i < questions.length; i++) {
      _logicalQuestion(
        questions[i] as Map<String, dynamic>,
        '\$.questions[$i]',
        issues,
      );
    }
    if (issues.isNotEmpty) throw PackValidationException(issues);
    return QuestionPack._validated(data);
  }

  String encode(QuestionPack pack) {
    // Revalidate before storing/exporting; metadata and keys are never rewritten.
    final source = const JsonEncoder.withIndent('  ').convert(pack.data);
    decode(source);
    return source;
  }
}

Never _fail(String path, String code, String message) =>
    throw PackValidationException([ValidationIssue(path, code, message)]);
void _issue(
  List<ValidationIssue> issues,
  String path,
  String code,
  String message,
) {
  if (issues.length < 50) issues.add(ValidationIssue(path, code, message));
}

/// Interpreter for the checked-in schema's fixed keyword set. It never accepts
/// schemas/regular expressions supplied by the imported pack, nor resolves URLs.
void _validateSchema(
  Object? value,
  Map<String, dynamic> schema,
  String path,
  List<ValidationIssue> errors,
) {
  if (errors.length >= 50) return;
  if (schema.containsKey(r'$ref')) {
    final key = (schema[r'$ref'] as String).split('/').last;
    _validateSchema(
      value,
      (_packSchema[r'$defs'] as Map)[key] as Map<String, dynamic>,
      path,
      errors,
    );
    return;
  }
  if (schema.containsKey('oneOf')) {
    final branches = (schema['oneOf'] as List).cast<Map<String, dynamic>>();
    final candidates = <List<ValidationIssue>>[];
    for (var branch in branches) {
      if (branch.containsKey(r'$ref')) {
        branch =
            (_packSchema[r'$defs'] as Map)[(branch[r'$ref'] as String)
                    .split('/')
                    .last]
                as Map<String, dynamic>;
      }
      // Discriminated branches give useful paths instead of a generic oneOf error.
      final props = branch['properties'] as Map?;
      var matches = true;
      if (value is Map && props != null) {
        for (final key in ['type', 'kind', 'mode']) {
          if (props[key] is Map &&
              (props[key] as Map).containsKey('const') &&
              value[key] != props[key]['const']) {
            matches = false;
          }
        }
      }
      if (!matches) continue;
      final local = <ValidationIssue>[];
      _validateSchema(value, branch, path, local);
      candidates.add(local);
    }
    if (candidates.where((e) => e.isEmpty).length == 1) return;
    if (candidates.isEmpty) {
      _issue(errors, path, 'unknown_type', '対応していない形式です');
      return;
    }
    candidates.sort((a, b) => a.length.compareTo(b.length));
    if (candidates.first.isEmpty) {
      _issue(errors, path, 'ambiguous_type', '形式を一意に判断できません');
    } else {
      errors.addAll(candidates.first.take(50 - errors.length));
    }
    return;
  }
  if (schema.containsKey('const') && value != schema['const']) {
    _issue(errors, path, 'const', '対応する値は ${schema['const']} です');
  }
  if (schema['enum'] is List && !(schema['enum'] as List).contains(value)) {
    _issue(errors, path, 'enum', '対応していない値です');
  }
  final type = schema['type'];
  final valid = switch (type) {
    'object' => value is Map<String, dynamic>,
    'array' => value is List,
    'string' => value is String,
    'boolean' => value is bool,
    'integer' =>
      value is num && value.isFinite && value == value.roundToDouble(),
    'number' => value is num && value.isFinite,
    null => true,
    _ => false,
  };
  if (!valid) {
    _issue(errors, path, 'type', '$type 型で指定してください');
    return;
  }
  if (value is Map<String, dynamic>) {
    final props = (schema['properties'] as Map<String, dynamic>?) ?? {};
    for (final key in (schema['required'] as List?) ?? []) {
      if (!value.containsKey(key)) {
        _issue(errors, '$path.$key', 'required', '必須項目がありません');
      }
    }
    for (final e in value.entries) {
      if (!props.containsKey(e.key)) {
        if (schema['additionalProperties'] == false) {
          _issue(errors, '$path.${e.key}', 'unknown_property', '未定義の項目です');
        }
      } else {
        _validateSchema(
          e.value,
          props[e.key] as Map<String, dynamic>,
          '$path.${e.key}',
          errors,
        );
      }
    }
  } else if (value is List) {
    if (value.length < ((schema['minItems'] as num?) ?? 0)) {
      _issue(errors, path, 'min_items', '要素が不足しています');
    }
    if (value.length > 10000) {
      _issue(errors, path, 'item_limit', '配列が大きすぎます');
      return;
    }
    if (schema['uniqueItems'] == true &&
        value.map(jsonEncode).toSet().length != value.length) {
      _issue(errors, path, 'duplicate', '要素が重複しています');
    }
    if (schema['items'] is Map) {
      for (var i = 0; i < value.length; i++) {
        _validateSchema(
          value[i],
          schema['items'] as Map<String, dynamic>,
          '$path[$i]',
          errors,
        );
      }
    }
  } else if (value is String) {
    if (value.length > 65536) {
      _issue(errors, path, 'text_limit', '文字数の上限を超えています');
    }
    if (value.runes.length < ((schema['minLength'] as num?) ?? 0) ||
        (schema['minLength'] != null && value.trim().isEmpty)) {
      _issue(errors, path, 'blank', '空白だけの値は使えません');
    }
    if (schema['pattern'] is String &&
        RegExp(schema['pattern'] as String).matchAsPrefix(value)?.end !=
            value.length) {
      _issue(errors, path, 'invalid_id', 'IDに使用できない文字があります');
    }
  } else if (value is num &&
      schema['minimum'] is num &&
      value < (schema['minimum'] as num)) {
    _issue(errors, path, 'minimum', '値が下限を下回っています');
  }
}

Set<String> _ids(List values, String key) =>
    values.map((x) => x[key] as String).toSet();
void _uniqueIds(
  List values,
  String path,
  List<ValidationIssue> errors, [
  String key = 'id',
]) {
  final seen = <String>{};
  for (var i = 0; i < values.length; i++) {
    if (!seen.add(values[i][key] as String)) {
      _issue(errors, '$path[$i].$key', 'duplicate_id', 'IDが重複しています');
    }
  }
}

void _sameSet(
  Iterable<String> actual,
  Set<String> expected,
  String path,
  List<ValidationIssue> errors,
) {
  final list = actual.toList(), set = actual.toSet();
  if (list.length != set.length ||
      set.length != expected.length ||
      !set.containsAll(expected)) {
    _issue(errors, path, 'incomplete_mapping', '各IDを重複なく1回ずつ指定してください');
  }
}

void _logicalQuestion(
  Map<String, dynamic> q,
  String p,
  List<ValidationIssue> errors,
) {
  for (var i = 0; i < ((q['stimulus'] as List?) ?? []).length; i++) {
    final block = q['stimulus'][i];
    if (block['kind'] == 'image' || block['kind'] == 'audio') {
      _issue(
        errors,
        '$p.stimulus[$i]',
        'unsupported_asset',
        '画像・音声付きパックは後続対応です',
      );
    }
  }
  switch (q['type']) {
    case 'single_choice':
    case 'multiple_choice':
      final options = q['options'] as List;
      _uniqueIds(options, '$p.options', errors);
      final ids = _ids(options, 'id');
      final key = q['answerKey'] as Map;
      final answers =
          q['type'] == 'single_choice'
              ? [key['optionId']]
              : key['optionIds'] as List;
      for (final id in answers) {
        if (!ids.contains(id)) {
          _issue(errors, '$p.answerKey', 'unknown_reference', '正答の選択肢IDがありません');
        }
      }
      if (q['type'] == 'multiple_choice') {
        final min = (q['minSelections'] as num?) ?? 1,
            max = (q['maxSelections'] as num?) ?? options.length;
        if (min > max ||
            max > options.length ||
            answers.length < min ||
            answers.length > max) {
          _issue(errors, p, 'selection_bounds', '選択数の範囲と正答数が一致しません');
        }
      }
    case 'entry':
      _uniqueIds(q['fields'] as List, '$p.fields', errors);
    case 'grid':
      final cols = q['columns'] as List, rows = q['rows'] as List;
      _uniqueIds(cols, '$p.columns', errors);
      _uniqueIds(rows, '$p.rows', errors);
      final ids = _ids(cols, 'id');
      var inputs = 0;
      for (var i = 0; i < rows.length; i++) {
        final cells = rows[i]['cells'] as List;
        _sameSet(
          cells.map((c) => c['columnId'] as String),
          ids,
          '$p.rows[$i].cells',
          errors,
        );
        inputs += cells.where((c) => c['mode'] != 'static').length;
      }
      if (inputs == 0) _issue(errors, p, 'no_inputs', '表に入力セルがありません');
    case 'interaction':
      final items = q['items'] as List, key = q['answerKey'] as Map;
      _uniqueIds(items, '$p.items', errors);
      final ids = _ids(items, 'id');
      if (q['mode'] == 'ordering') {
        if (key['orderedIds'] is! List ||
            key.containsKey('assignments') ||
            q.containsKey('groups')) {
          _issue(
            errors,
            '$p.answerKey',
            'ordering_key',
            '並べ替えにはorderedIdsのみ指定してください',
          );
        } else {
          _sameSet(
            (key['orderedIds'] as List).cast<String>(),
            ids,
            '$p.answerKey.orderedIds',
            errors,
          );
        }
      } else {
        if (q['groups'] is! List ||
            key['assignments'] is! List ||
            key.containsKey('orderedIds')) {
          _issue(
            errors,
            '$p.answerKey',
            'grouping_key',
            '分類にはgroupsとassignmentsが必要です',
          );
        } else {
          final groups = q['groups'] as List,
              assignments = key['assignments'] as List;
          _uniqueIds(groups, '$p.groups', errors);
          _sameSet(
            assignments.map((a) => a['itemId'] as String),
            ids,
            '$p.answerKey.assignments',
            errors,
          );
          for (var i = 0; i < assignments.length; i++) {
            if (!_ids(groups, 'id').contains(assignments[i]['groupId'])) {
              _issue(
                errors,
                '$p.answerKey.assignments[$i].groupId',
                'unknown_reference',
                'グループIDがありません',
              );
            }
          }
        }
      }
    case 'word_bank':
      final segments = q['segments'] as List, tokens = q['tokens'] as List;
      final blanks = segments.where((s) => s['kind'] == 'blank').toList();
      _uniqueIds(blanks, '$p.segments', errors);
      _uniqueIds(tokens, '$p.tokens', errors);
      if (blanks.isEmpty) {
        _issue(errors, '$p.segments', 'no_blanks', '空欄がありません');
      }
      final matches = q['answerKey']['matches'] as List;
      _sameSet(
        matches.map((m) => m['blankId'] as String),
        _ids(blanks, 'id'),
        '$p.answerKey.matches',
        errors,
      );
      final ids = _ids(tokens, 'id');
      final used = <String>{};
      for (var i = 0; i < matches.length; i++) {
        final id = matches[i]['tokenId'] as String;
        if (!ids.contains(id)) {
          _issue(
            errors,
            '$p.answerKey.matches[$i].tokenId',
            'unknown_reference',
            '語群IDがありません',
          );
        }
        if (!used.add(id) && q['reuseTokens'] != true) {
          _issue(
            errors,
            '$p.answerKey.matches[$i]',
            'token_reuse',
            '語群の再利用は禁止されています',
          );
        }
      }
  }
}
