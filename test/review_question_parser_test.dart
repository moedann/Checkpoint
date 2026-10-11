import 'package:flutter_test/flutter_test.dart';
import 'package:nanisini/review_question_parser.dart';

void main() {
  test('supported labels are parsed', () {
    for (final text in ['問 x\n答 y','問題 x\n答え y','Q: x\nA: y','Q： x\n回答 y','問 x\n正解 y']) {
      final result=ReviewQuestionParser.parse(text); expect(result.single.question,'x'); expect(result.single.answer,'y');
    }
  });
  test('valid and missing answers are previewed',(){final r=ReviewQuestionParser.parse('問 1+1\n\n問 2+2\n答 4');expect(r,hasLength(2));expect(r.first.isValid,isFalse);expect(r.last.isValid,isTrue);});
  test('acceptance batch parses',(){final r=ReviewQuestionParser.parse('問 achieve の意味は？\n答 達成する\n\n問 increase の意味は？\n答 増加する');expect(r,hasLength(2));expect(r.every((e)=>e.isValid),isTrue);});
  test('three-choice format keeps all options in the question',(){final r=ReviewQuestionParser.parse('問 日本の首都は？\n選択肢A 東京\n選択肢B 大阪\n選択肢C 京都\n答 東京');expect(r.single.question,contains('選択肢C 京都'));expect(r.single.answer,'東京');});
}
