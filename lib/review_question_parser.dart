class ParsedReviewQuestion {
  const ParsedReviewQuestion({required this.question, this.answer});
  final String question;
  final String? answer;
  bool get isValid =>
      question.trim().isNotEmpty && answer?.trim().isNotEmpty == true;
}

class ReviewQuestionParser {
  static final _marker = RegExp(
    r'^(問題|問|Q\s*[:：]?|答え|答|回答|正解|A\s*[:：]?)\s*',
    caseSensitive: false,
    multiLine: true,
  );
  static List<ParsedReviewQuestion> parse(String source) {
    final text = source.replaceAll('\r\n', '\n').replaceAll('\r', '\n');
    final matches = _marker.allMatches(text).toList();
    final result = <ParsedReviewQuestion>[];
    String? question, answer;
    void flush() {
      if (question?.trim().isNotEmpty == true) {
        result.add(
          ParsedReviewQuestion(
            question: question!.trim(),
            answer: answer?.trim().isEmpty == true ? null : answer?.trim(),
          ),
        );
      }
      question = null;
      answer = null;
    }

    for (var i = 0; i < matches.length; i++) {
      final match = matches[i];
      final label = match.group(1)!.toLowerCase();
      final end = i + 1 < matches.length ? matches[i + 1].start : text.length;
      final content = text.substring(match.end, end).trim();
      if (label == '問' || label == '問題' || label.startsWith('q')) {
        flush();
        question = content;
      } else if (question != null) {
        answer = content;
      }
    }
    flush();
    return result;
  }
}
