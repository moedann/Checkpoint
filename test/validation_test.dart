import 'package:flutter_test/flutter_test.dart';

import 'package:nanisini/validation.dart';

void main() {
  test('purpose selection requires a value', () {
    expect(validatePurposeSelection(null).isValid, isFalse);
    expect(validatePurposeSelection('').isValid, isFalse);
    expect(validatePurposeSelection('a').isValid, isTrue);
  });

  test('duration boundaries are enforced', () {
    expect(validateDurationMinutes(null).isValid, isFalse);
    expect(validateDurationMinutes(0).isValid, isFalse);
    expect(validateDurationMinutes(181).isValid, isFalse);
    expect(validateDurationMinutes(30).isValid, isTrue);
  });

  test('other purpose cannot be blank', () {
    expect(validateOtherPurpose('   ').isValid, isFalse);
    expect(validateOtherPurpose('ギターの動画を調べる').isValid, isTrue);
  });

  test('activity details allow special no-activity case', () {
    expect(validateActivityDetail(genre: '開発・制作', detail: '').isValid, isFalse);
    expect(
      validateActivityDetail(genre: '特に何もしていない', detail: '').isValid,
      isTrue,
    );
    expect(
      validateActivityDetail(genre: '仕事', detail: 'x' * 101).isValid,
      isFalse,
    );
  });
}
