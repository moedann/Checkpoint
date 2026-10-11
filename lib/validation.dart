class ValidationResult {
  const ValidationResult.valid() : isValid = true, errorMessage = null;
  const ValidationResult.invalid(this.errorMessage) : isValid = false;

  final bool isValid;
  final String? errorMessage;
}

ValidationResult validatePurposeItemLabel(String value) {
  final trimmed = value.trim();
  if (trimmed.isEmpty) {
    return const ValidationResult.invalid('利用目的名を入力してください');
  }
  if (trimmed.length > 60) {
    return const ValidationResult.invalid('60文字以内で入力してください');
  }
  return const ValidationResult.valid();
}

ValidationResult validateActivityCategoryName(String value) {
  final trimmed = value.trim();
  if (trimmed.isEmpty) {
    return const ValidationResult.invalid('ジャンル名を入力してください');
  }
  if (trimmed.length > 40) {
    return const ValidationResult.invalid('40文字以内で入力してください');
  }
  return const ValidationResult.valid();
}

ValidationResult validatePurposeSelection(String? id) {
  if (id == null || id.trim().isEmpty) {
    return const ValidationResult.invalid('利用目的を選択してください');
  }
  return const ValidationResult.valid();
}

ValidationResult validateDurationMinutes(int? minutes) {
  if (minutes == null) {
    return const ValidationResult.invalid('利用時間を選択してください');
  }
  if (minutes < 1) {
    return const ValidationResult.invalid('1分以上を選択してください');
  }
  if (minutes > 180) {
    return const ValidationResult.invalid('180分以内で入力してください');
  }
  return const ValidationResult.valid();
}

ValidationResult validateOtherPurpose(String value) {
  final trimmed = value.trim();
  if (trimmed.isEmpty) {
    return const ValidationResult.invalid('その他の目的を入力してください');
  }
  if (trimmed.length > 60) {
    return const ValidationResult.invalid('60文字以内で入力してください');
  }
  return const ValidationResult.valid();
}

ValidationResult validateActivityDetail({
  required String genre,
  required String detail,
}) {
  final trimmed = detail.trim();
  if (genre != '特に何もしていない' && trimmed.isEmpty) {
    return const ValidationResult.invalid('詳細を入力してください');
  }
  if (trimmed.length > 100) {
    return const ValidationResult.invalid('100文字以内で入力してください');
  }
  return const ValidationResult.valid();
}
