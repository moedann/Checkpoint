part of 'question_engine.dart';

/// All construction goes through QuestionPackCodec, after structural and logical
/// validation. Deeply immutable data keeps IDs, answer keys and metadata intact.
class QuestionPack {
  QuestionPack._validated(Map<String, dynamic> data)
    : data = freezeJson(data) as Map<String, dynamic>,
      questions = List.unmodifiable(
        (data['questions'] as List).map(
          (q) => Question._validated(q as Map<String, dynamic>),
        ),
      );
  final Map<String, dynamic> data;
  final List<Question> questions;
  String get packId => data['packId'] as String;
  int get revision => (data['revision'] as num?)?.toInt() ?? 1;
  String get title => data['title'] as String;
  Map<String, dynamic> toJson() =>
      jsonDecode(jsonEncode(data)) as Map<String, dynamic>;
}

class Question {
  Question._validated(Map<String, dynamic> data)
    : data = freezeJson(data) as Map<String, dynamic>;
  final Map<String, dynamic> data;
  String get id => data['id'] as String;
  String get type => data['type'] as String;
  String get prompt => data['prompt'] as String;
  String get explanation => data['explanation'] as String;
}

Object? freezeJson(Object? value) {
  if (value is Map<String, dynamic>) {
    return Map<String, dynamic>.unmodifiable(
      value.map((k, v) => MapEntry(k, freezeJson(v))),
    );
  }
  if (value is List) return List<Object?>.unmodifiable(value.map(freezeJson));
  return value;
}

class ValidationIssue {
  const ValidationIssue(this.path, this.code, this.message);
  final String path;
  final String code;
  final String message;
  @override
  String toString() => '$path: $message ($code)';
}

class PackValidationException implements Exception {
  PackValidationException(Iterable<ValidationIssue> issues)
    : issues = List.unmodifiable(issues);
  final List<ValidationIssue> issues;
  @override
  String toString() => issues.join('\n');
}

sealed class QuestionAnswer {
  const QuestionAnswer();
}

class SingleChoiceAnswer extends QuestionAnswer {
  const SingleChoiceAnswer(this.optionId);
  final String? optionId;
}

class MultipleChoiceAnswer extends QuestionAnswer {
  MultipleChoiceAnswer(Iterable<String> optionIds)
    : optionIds = List.unmodifiable(optionIds);
  final List<String> optionIds;
}

/// Entry: field ID -> raw input. Grid: JSON encoded [row ID, column ID] -> input,
/// generated with gridCellKey (IDs may themselves contain punctuation).
class InputAnswer extends QuestionAnswer {
  InputAnswer(Map<String, String> values) : values = Map.unmodifiable(values);
  final Map<String, String> values;
}

String gridCellKey(String rowId, String columnId) =>
    jsonEncode([rowId, columnId]);

class OrderingAnswer extends QuestionAnswer {
  OrderingAnswer(Iterable<String> ids) : ids = List.unmodifiable(ids);
  final List<String> ids;
}

/// Grouping: item ID -> group ID. Word bank: blank ID -> token ID.
class AssignmentAnswer extends QuestionAnswer {
  AssignmentAnswer(Map<String, String> values)
    : values = Map.unmodifiable(values);
  final Map<String, String> values;
}

class SelfReviewAnswer extends QuestionAnswer {
  const SelfReviewAnswer({required this.attempted, this.rating});
  final bool attempted;

  /// Only set by the UI after displaying the model answer/rubric.
  final SelfRating? rating;
}

enum SelfRating { correct, partial, incorrect }

enum Grade { correct, partial, incorrect, selfReviewed, ungraded }

class GradeDetail {
  const GradeDetail({
    required this.id,
    required this.correct,
    this.expected,
    this.actual,
  });
  final String id;
  final bool correct;
  final Object? expected;
  final Object? actual;
}

class GradeResult {
  GradeResult(
    this.grade, {
    Iterable<GradeDetail> details = const [],
    Iterable<ValidationIssue> issues = const [],
  }) : details = List.unmodifiable(details),
       issues = List.unmodifiable(issues);
  final Grade grade;
  final List<GradeDetail> details;
  final List<ValidationIssue> issues;
  bool get canSubmit => issues.isEmpty;
  // Deliberately no unlock flag: review acknowledgement is a later gate action.
}
