import 'package:flutter/services.dart';

class AppSnapshot {
  AppSnapshot({
    required this.introAccepted,
    this.checkpointEnabled = true,
    this.reviewCaptchaEnabled = true,
    this.versionName = '1.0.0',
    this.activeSession,
    required this.permissionSummary,
    required this.pauseSummary,
    required this.purposeCategories,
    required this.purposeItems,
    required this.activityCategories,
    required this.privacyConfiguration,
    this.targetApps = const [],
    this.reviewQuestions = const [],
    this.questionBooks = const [],
    this.selectedQuestionBookIds = const {},
    this.reviewQuestionCount = 1,
  });

  factory AppSnapshot.fromMap(Map<dynamic, dynamic> map) {
    return AppSnapshot(
      introAccepted: map['introAccepted'] as bool? ?? false,
      checkpointEnabled: map['checkpointEnabled'] as bool? ?? true,
      reviewCaptchaEnabled: map['reviewCaptchaEnabled'] as bool? ?? true,
      versionName: map['versionName'] as String? ?? '1.0.0',
      activeSession: map['activeSession'] is Map ? ActiveSessionSummary.fromMap(map['activeSession'] as Map<dynamic,dynamic>) : null,
      permissionSummary: PermissionSummary.fromMap(
        map['permissionSummary'] as Map<dynamic, dynamic>? ?? const {},
      ),
      pauseSummary: FeaturePauseSummary.fromMap(
        map['pauseSummary'] as Map<dynamic, dynamic>? ?? const {},
      ),
      purposeCategories:
          (map['purposeCategories'] as List<dynamic>? ?? const [])
              .map(
                (entry) =>
                    PurposeCategory.fromMap(entry as Map<dynamic, dynamic>),
              )
              .toList(),
      purposeItems:
          (map['purposeItems'] as List<dynamic>? ?? const [])
              .map(
                (entry) => PurposeItem.fromMap(entry as Map<dynamic, dynamic>),
              )
              .toList(),
      activityCategories:
          (map['activityCategories'] as List<dynamic>? ?? const [])
              .map(
                (entry) =>
                    ActivityCategory.fromMap(entry as Map<dynamic, dynamic>),
              )
              .toList(),
      privacyConfiguration: PrivacyConfiguration.fromMap(
        map['privacyConfiguration'] as Map<dynamic, dynamic>? ?? const {},
      ),
      targetApps:
          (map['targetApps'] as List<dynamic>? ?? const [])
              .map(
                (entry) => TargetApp.fromMap(entry as Map<dynamic, dynamic>),
              )
              .toList(),
      reviewQuestions:
          (map['reviewQuestions'] as List<dynamic>? ?? const [])
              .map(
                (entry) =>
                    ReviewQuestion.fromMap(entry as Map<dynamic, dynamic>),
              )
              .toList(),
      questionBooks: (map['questionBooks'] as List<dynamic>? ?? const []).map((e) => QuestionBook.fromMap(e as Map<dynamic, dynamic>)).toList(),
      selectedQuestionBookIds: (map['selectedQuestionBookIds'] as List<dynamic>? ?? const []).map((e) => e.toString()).toSet(),
      reviewQuestionCount: (map['reviewQuestionCount'] as num?)?.toInt() ?? 1,
    );
  }

  final bool introAccepted;
  final bool checkpointEnabled;
  final bool reviewCaptchaEnabled;
  final String versionName;
  final ActiveSessionSummary? activeSession;
  final PermissionSummary permissionSummary;
  final FeaturePauseSummary pauseSummary;
  final List<PurposeCategory> purposeCategories;
  final List<PurposeItem> purposeItems;
  final List<ActivityCategory> activityCategories;
  final PrivacyConfiguration privacyConfiguration;
  final List<TargetApp> targetApps;
  final List<ReviewQuestion> reviewQuestions;
  final List<QuestionBook> questionBooks;
  final Set<String> selectedQuestionBookIds;
  final int reviewQuestionCount;

  String categoryNameById(String id) {
    return purposeCategories
            .where((category) => category.id == id)
            .map((category) => category.name)
            .firstOrNull ??
        '未設定';
  }
}

class ActiveSessionSummary {
  const ActiveSessionSummary(this.applicationLabel,this.purposeText,this.remainingMillis);
  factory ActiveSessionSummary.fromMap(Map<dynamic,dynamic> map)=>ActiveSessionSummary(map['applicationLabel'] as String? ?? '',map['purposeText'] as String? ?? '',(map['remainingMillis'] as num?)?.toInt()??0);
  final String applicationLabel,purposeText;final int remainingMillis;
}

class TargetApp {
  const TargetApp({
    required this.packageName,
    required this.label,
    required this.enabled,
    this.iconBytes,
  });

  factory TargetApp.fromMap(Map<dynamic, dynamic> map) => TargetApp(
    packageName: map['packageName'] as String? ?? '',
    label: map['label'] as String? ?? '',
    enabled: map['enabled'] as bool? ?? true,
    iconBytes: map['icon'] as Uint8List?,
  );

  final String packageName;
  final String label;
  final bool enabled;
  final Uint8List? iconBytes;
}

class PermissionSummary {
  const PermissionSummary({
    required this.accessibilityEnabled,
    required this.overlayEnabled,
    required this.serviceRunning,
    required this.lastDetectedApp,
    required this.exclusionStatus,
    this.timerNotificationsEnabled = false,
    this.liveUpdateStatus = 'UNAVAILABLE',
  });

  factory PermissionSummary.fromMap(Map<dynamic, dynamic> map) {
    return PermissionSummary(
      accessibilityEnabled: map['accessibilityEnabled'] as bool? ?? false,
      overlayEnabled: map['overlayEnabled'] as bool? ?? false,
      serviceRunning: map['serviceRunning'] as bool? ?? false,
      lastDetectedApp: map['lastDetectedApp'] as String? ?? '',
      exclusionStatus: map['exclusionStatus'] as String? ?? '未確認',
      timerNotificationsEnabled:
          map['timerNotificationsEnabled'] as bool? ?? false,
      liveUpdateStatus: map['liveUpdateStatus'] as String? ?? 'UNAVAILABLE',
    );
  }

  final bool accessibilityEnabled;
  final bool overlayEnabled;
  final bool serviceRunning;
  final String lastDetectedApp;
  final String exclusionStatus;
  final bool timerNotificationsEnabled;
  final String liveUpdateStatus;
}

class FeaturePauseSummary {
  const FeaturePauseSummary({
    required this.description,
    this.mode = 'NONE',
    this.untilElapsedRealtime,
  });

  factory FeaturePauseSummary.fromMap(Map<dynamic, dynamic> map) {
    return FeaturePauseSummary(
      description: map['description'] as String? ?? '一時停止はありません。',
      mode: map['mode'] as String? ?? 'NONE',
      untilElapsedRealtime: (map['untilElapsedRealtime'] as num?)?.toInt(),
    );
  }

  final String description;
  final String mode;
  final int? untilElapsedRealtime;
  bool get isPaused => mode != 'NONE';
}

class PrivacyConfiguration {
  const PrivacyConfiguration({required this.summary});

  factory PrivacyConfiguration.fromMap(Map<dynamic, dynamic> map) {
    return PrivacyConfiguration(
      summary: map['summary'] as String? ?? 'プライバシー設定を読み込めませんでした。',
    );
  }

  final String summary;
}

class TodayDashboard {
  const TodayDashboard({
    required this.launchCount,
    required this.purposeCount,
    required this.noPurposeCount,
    required this.topPurposes,
    required this.recent,
  });

  factory TodayDashboard.fromMap(Map<dynamic, dynamic> map) => TodayDashboard(
    launchCount: (map['launchCount'] as num?)?.toInt() ?? 0,
    purposeCount: (map['purposeCount'] as num?)?.toInt() ?? 0,
    noPurposeCount: (map['noPurposeCount'] as num?)?.toInt() ?? 0,
    topPurposes:
        (map['topPurposes'] as List<dynamic>? ?? const [])
            .map(
              (item) => UsageBreakdown.fromMap(item as Map<dynamic, dynamic>),
            )
            .toList(),
    recent:
        (map['recent'] as List<dynamic>? ?? const [])
            .map((item) => RecentUsage.fromMap(item as Map<dynamic, dynamic>))
            .toList(),
  );

  final int launchCount;
  final int purposeCount;
  final int noPurposeCount;
  final List<UsageBreakdown> topPurposes;
  final List<RecentUsage> recent;

  int get purposeRate =>
      launchCount == 0 ? 0 : (purposeCount * 100 / launchCount).round();
}

class MonthlyDashboard {
  const MonthlyDashboard({
    required this.year,
    required this.month,
    required this.totalUsageSeconds,
    required this.launchCount,
    required this.previousUsageSeconds,
    required this.previousLaunchCount,
    required this.daily,
    required this.topByUsage,
    required this.topByLaunches,
    required this.purposeCategories,
    required this.purposes,
    required this.noPurposeCount,
    required this.previousNoPurposeCount,
    required this.noPurposeApps,
    required this.activityCategories,
    required this.activityApps,
    required this.recent,
    required this.earliestMonth,
  });

  factory MonthlyDashboard.fromMap(Map<dynamic, dynamic> map) {
    List<T> list<T>(String key, T Function(Map<dynamic, dynamic>) parse) =>
        (map[key] as List<dynamic>? ?? const [])
            .map((value) => parse(value as Map<dynamic, dynamic>))
            .toList();
    final activityMap =
        map['activityApps'] as Map<dynamic, dynamic>? ?? const {};
    return MonthlyDashboard(
      year: (map['year'] as num?)?.toInt() ?? DateTime.now().year,
      month: (map['month'] as num?)?.toInt() ?? DateTime.now().month,
      totalUsageSeconds: (map['totalUsageSeconds'] as num?)?.toInt() ?? 0,
      launchCount: (map['launchCount'] as num?)?.toInt() ?? 0,
      previousUsageSeconds: (map['previousUsageSeconds'] as num?)?.toInt() ?? 0,
      previousLaunchCount: (map['previousLaunchCount'] as num?)?.toInt() ?? 0,
      daily: list('daily', DailyUsage.fromMap),
      topByUsage: list('topByUsage', AppUsage.fromMap),
      topByLaunches: list('topByLaunches', AppUsage.fromMap),
      purposeCategories: list('purposeCategories', UsageBreakdown.fromMap),
      purposes: list('purposes', UsageBreakdown.fromMap),
      noPurposeCount: (map['noPurposeCount'] as num?)?.toInt() ?? 0,
      previousNoPurposeCount:
          (map['previousNoPurposeCount'] as num?)?.toInt() ?? 0,
      noPurposeApps: list('noPurposeApps', AppUsage.fromMap),
      activityCategories: list('activityCategories', UsageBreakdown.fromMap),
      activityApps: activityMap.map(
        (key, value) => MapEntry(
          key.toString(),
          (value as List<dynamic>)
              .map((item) => AppUsage.fromMap(item as Map<dynamic, dynamic>))
              .toList(),
        ),
      ),
      recent: list('recent', RecentUsage.fromMap),
      earliestMonth: map['earliestMonth'] as String?,
    );
  }

  final int year;
  final int month;
  final int totalUsageSeconds;
  final int launchCount;
  final int previousUsageSeconds;
  final int previousLaunchCount;
  final List<DailyUsage> daily;
  final List<AppUsage> topByUsage;
  final List<AppUsage> topByLaunches;
  final List<UsageBreakdown> purposeCategories;
  final List<UsageBreakdown> purposes;
  final int noPurposeCount;
  final int previousNoPurposeCount;
  final List<AppUsage> noPurposeApps;
  final List<UsageBreakdown> activityCategories;
  final Map<String, List<AppUsage>> activityApps;
  final List<RecentUsage> recent;
  final String? earliestMonth;
}

class DailyUsage {
  const DailyUsage(this.day, this.usageSeconds, this.launchCount, [this.purposeCount = 0]);
  factory DailyUsage.fromMap(Map<dynamic, dynamic> map) => DailyUsage(
    (map['day'] as num?)?.toInt() ?? 0,
    (map['usageSeconds'] as num?)?.toInt() ?? 0,
    (map['launchCount'] as num?)?.toInt() ?? 0,
    (map['purposeCount'] as num?)?.toInt() ?? 0,
  );
  final int day;
  final int usageSeconds;
  final int launchCount;
  final int purposeCount;
}

class AppUsage {
  const AppUsage(
    this.packageName,
    this.applicationLabel,
    this.usageSeconds,
    this.launchCount,
  );
  factory AppUsage.fromMap(Map<dynamic, dynamic> map) => AppUsage(
    map['packageName'] as String? ?? '',
    map['applicationLabel'] as String? ?? '',
    (map['usageSeconds'] as num?)?.toInt() ?? 0,
    (map['launchCount'] as num?)?.toInt() ?? 0,
  );
  final String packageName;
  final String applicationLabel;
  final int usageSeconds;
  final int launchCount;
}

class UsageBreakdown {
  const UsageBreakdown(
    this.id,
    this.label,
    this.usageSeconds,
    this.launchCount,
  );
  factory UsageBreakdown.fromMap(Map<dynamic, dynamic> map) => UsageBreakdown(
    map['id'] as String? ?? '',
    map['label'] as String? ?? '未設定',
    (map['usageSeconds'] as num?)?.toInt() ?? 0,
    (map['launchCount'] as num?)?.toInt() ?? 0,
  );
  final String id;
  final String label;
  final int usageSeconds;
  final int launchCount;
}

class RecentUsage {
  const RecentUsage(
    this.id,
    this.startedAt,
    this.packageName,
    this.applicationLabel,
    this.purposeText,
    this.actualUsageSeconds,
  );
  factory RecentUsage.fromMap(Map<dynamic, dynamic> map) => RecentUsage(
    map['id'] as String? ?? '',
    (map['startedAt'] as num?)?.toInt() ?? 0,
    map['packageName'] as String? ?? '',
    map['applicationLabel'] as String? ?? '',
    map['purposeText'] as String? ?? '',
    (map['actualUsageSeconds'] as num?)?.toInt() ?? 0,
  );
  final String id;
  final int startedAt;
  final String packageName;
  final String applicationLabel;
  final String purposeText;
  final int actualUsageSeconds;
}

class PurposeCategory {
  const PurposeCategory({
    required this.id,
    required this.name,
    required this.displayOrder,
    required this.isDefault,
  });

  factory PurposeCategory.fromMap(Map<dynamic, dynamic> map) {
    return PurposeCategory(
      id: map['id'] as String? ?? '',
      name: map['name'] as String? ?? '',
      displayOrder: map['displayOrder'] as int? ?? 0,
      isDefault: map['isDefault'] as bool? ?? false,
    );
  }

  final String id;
  final String name;
  final int displayOrder;
  final bool isDefault;
}

class PurposeItem {
  const PurposeItem({
    required this.id,
    required this.categoryId,
    required this.label,
    required this.isDefault,
    required this.createdAt,
  });

  factory PurposeItem.fromMap(Map<dynamic, dynamic> map) {
    return PurposeItem(
      id: map['id'] as String? ?? '',
      categoryId: map['categoryId'] as String? ?? '',
      label: map['label'] as String? ?? '',
      isDefault: map['isDefault'] as bool? ?? false,
      createdAt: map['createdAt'] as int? ?? 0,
    );
  }

  final String id;
  final String categoryId;
  final String label;
  final bool isDefault;
  final int createdAt;

  PurposeItem copyWith({
    String? id,
    String? categoryId,
    String? label,
    bool? isDefault,
    int? createdAt,
  }) {
    return PurposeItem(
      id: id ?? this.id,
      categoryId: categoryId ?? this.categoryId,
      label: label ?? this.label,
      isDefault: isDefault ?? this.isDefault,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}

class ActivityCategory {
  const ActivityCategory({
    required this.id,
    required this.name,
    required this.displayOrder,
    required this.isDefault,
  });

  factory ActivityCategory.fromMap(Map<dynamic, dynamic> map) {
    return ActivityCategory(
      id: map['id'] as String? ?? '',
      name: map['name'] as String? ?? '',
      displayOrder: map['displayOrder'] as int? ?? 0,
      isDefault: map['isDefault'] as bool? ?? false,
    );
  }

  final String id;
  final String name;
  final int displayOrder;
  final bool isDefault;

  ActivityCategory copyWith({
    String? id,
    String? name,
    int? displayOrder,
    bool? isDefault,
  }) {
    return ActivityCategory(
      id: id ?? this.id,
      name: name ?? this.name,
      displayOrder: displayOrder ?? this.displayOrder,
      isDefault: isDefault ?? this.isDefault,
    );
  }
}

class PurposeItemDraft {
  const PurposeItemDraft({required this.label, required this.categoryId});

  final String label;
  final String categoryId;
}

class ActivityCategoryDraft {
  const ActivityCategoryDraft({required this.name});

  final String name;
}

class ReviewQuestion {
  const ReviewQuestion({
    required this.id,
    required this.bookId,
    required this.question,
    required this.answer,
    required this.createdAt,
    this.updatedAt = 0,
    this.isEnabled = true,
    this.answerCount = 0,
    this.correctCount = 0,
  });

  factory ReviewQuestion.fromMap(Map<dynamic, dynamic> map) => ReviewQuestion(
    id: map['id'] as String? ?? '',
    bookId: map['bookId'] as String? ?? '',
    question: map['question'] as String? ?? '',
    answer: map['answer'] as String? ?? '',
    createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
    updatedAt: (map['updatedAt'] as num?)?.toInt() ?? 0,
    isEnabled: map['isEnabled'] as bool? ?? true,
    answerCount: (map['answerCount'] as num?)?.toInt() ?? 0,
    correctCount: (map['correctCount'] as num?)?.toInt() ?? 0,
  );

  final String id;
  final String bookId;
  final String question;
  final String answer;
  final int createdAt;
  final int updatedAt;
  final bool isEnabled;
  final int answerCount, correctCount;
  double get accuracy => answerCount == 0 ? 0 : correctCount * 100 / answerCount;

  Map<String, Object?> toMap() => {
    'id': id,
    'bookId': bookId,
    'question': question,
    'answer': answer,
    'createdAt': createdAt,
    'updatedAt': updatedAt,
    'isEnabled': isEnabled,
    'answerCount': answerCount,
    'correctCount': correctCount,
  };

  ReviewQuestion copyWith({
    String? question,
    String? answer,
    bool? isEnabled,
  }) => ReviewQuestion(
    id: id,
    bookId: bookId,
    question: question ?? this.question,
    answer: answer ?? this.answer,
    createdAt: createdAt,
    updatedAt: updatedAt,
    isEnabled: isEnabled ?? this.isEnabled,
    answerCount: answerCount,
    correctCount: correctCount,
  );
}

class QuestionBook {
  const QuestionBook({required this.id, required this.title, required this.createdAt, required this.updatedAt});
  factory QuestionBook.fromMap(Map<dynamic, dynamic> map) => QuestionBook(id: map['id'] as String? ?? '', title: map['title'] as String? ?? '', createdAt: (map['createdAt'] as num?)?.toInt() ?? 0, updatedAt: (map['updatedAt'] as num?)?.toInt() ?? 0);
  final String id, title;
  final int createdAt, updatedAt;
}

abstract class NanishiniBridge {
  const NanishiniBridge();

  Future<AppSnapshot> loadSnapshot();
  Future<void> acceptIntro();
  Future<void> openAccessibilitySettings();
  Future<void> openOverlaySettings();
  Future<void> requestNotificationPermission();
  Future<void> openNotificationSettings();
  Future<void> pauseForMinutes(int minutes);
  Future<void> pauseUntilNextAppOpen();
  Future<void> resumeFeature();
  Future<void> setEnabledTargetPackages(Set<String> packageNames);
  Future<void> runOverlayTest();
  Future<void> addPurposeItem(PurposeItemDraft draft);
  Future<void> updatePurposeItem(PurposeItem item);
  Future<void> deletePurposeItem(String id);
  Future<void> addActivityCategory(ActivityCategoryDraft draft);
  Future<void> updateActivityCategory(ActivityCategory item);
  Future<void> deleteActivityCategory(String id);
  Future<void> addReviewQuestion(String bookId, String question, String answer) =>
      throw UnimplementedError();
  Future<int> addReviewQuestions(String bookId, List<ReviewQuestion> questions) =>
      throw UnimplementedError();
  Future<void> updateReviewQuestion(ReviewQuestion question) =>
      throw UnimplementedError();
  Future<void> deleteReviewQuestion(String id) => throw UnimplementedError();
  Future<void> addQuestionBook(String title) => throw UnimplementedError();
  Future<void> updateQuestionBook(String id, String title) => throw UnimplementedError();
  Future<void> deleteQuestionBook(String id) => throw UnimplementedError();
  Future<void> setSelectedQuestionBookIds(Set<String> ids) => throw UnimplementedError();
  Future<void> setReviewQuestionCount(int count) => throw UnimplementedError();
  Future<void> setCheckpointEnabled(bool enabled) => throw UnimplementedError();
  Future<void> setReviewCaptchaEnabled(bool enabled) => throw UnimplementedError();
  Future<MonthlyDashboard> loadMonthlyDashboard(int year, int month);
  Future<TodayDashboard> loadTodayDashboard() async {
    final now = DateTime.now();
    final month = await loadMonthlyDashboard(now.year, now.month);
    final today = month.daily.where((item) => item.day == now.day).firstOrNull;
    return TodayDashboard(
      launchCount: today?.launchCount ?? 0,
      purposeCount: 0,
      noPurposeCount: 0,
      topPurposes: const [],
      recent: month.recent.take(3).toList(),
    );
  }

  Future<void> deleteUsageHistory();
  Future<void> deleteAllData();
}

class MethodChannelNanishiniBridge extends NanishiniBridge {
  const MethodChannelNanishiniBridge({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel('nanishini/bridge');

  final MethodChannel _channel;

  @override
  Future<AppSnapshot> loadSnapshot() async {
    final result = await _channel.invokeMapMethod<String, dynamic>(
      'getAppState',
    );
    if (result == null) {
      throw PlatformException(
        code: 'empty_state',
        message: 'ネイティブ状態を取得できませんでした',
      );
    }
    return AppSnapshot.fromMap(result);
  }

  @override
  Future<void> acceptIntro() => _channel.invokeMethod<void>('acceptIntro');

  @override
  Future<void> openAccessibilitySettings() =>
      _channel.invokeMethod<void>('openAccessibilitySettings');

  @override
  Future<void> openOverlaySettings() =>
      _channel.invokeMethod<void>('openOverlaySettings');

  @override
  Future<void> requestNotificationPermission() =>
      _channel.invokeMethod<void>('requestNotificationPermission');

  @override
  Future<void> openNotificationSettings() =>
      _channel.invokeMethod<void>('openNotificationSettings');

  @override
  Future<void> pauseForMinutes(int minutes) =>
      _channel.invokeMethod<void>('pauseForMinutes', {'minutes': minutes});

  @override
  Future<void> pauseUntilNextAppOpen() =>
      _channel.invokeMethod<void>('pauseUntilNextAppOpen');

  @override
  Future<void> resumeFeature() => _channel.invokeMethod<void>('resumeFeature');

  @override
  Future<void> setEnabledTargetPackages(Set<String> packageNames) =>
      _channel.invokeMethod<void>('setEnabledTargetPackages', {
        'packageNames': packageNames.toList(),
      });

  @override
  Future<void> runOverlayTest() =>
      _channel.invokeMethod<void>('runOverlayTest');

  @override
  Future<void> addPurposeItem(PurposeItemDraft draft) =>
      _channel.invokeMethod<void>('addPurposeItem', draft.toMap());

  @override
  Future<void> updatePurposeItem(PurposeItem item) =>
      _channel.invokeMethod<void>('updatePurposeItem', item.toMap());

  @override
  Future<void> deletePurposeItem(String id) =>
      _channel.invokeMethod<void>('deletePurposeItem', {'id': id});

  @override
  Future<void> addActivityCategory(ActivityCategoryDraft draft) =>
      _channel.invokeMethod<void>('addActivityCategory', draft.toMap());

  @override
  Future<void> updateActivityCategory(ActivityCategory item) =>
      _channel.invokeMethod<void>('updateActivityCategory', item.toMap());

  @override
  Future<void> deleteActivityCategory(String id) =>
      _channel.invokeMethod<void>('deleteActivityCategory', {'id': id});

  @override
  Future<void> addReviewQuestion(String bookId, String question, String answer) =>
      _channel.invokeMethod<void>('addReviewQuestion', {
        'bookId': bookId,
        'question': question,
        'answer': answer,
      });

  @override
  Future<int> addReviewQuestions(String bookId, List<ReviewQuestion> questions) async =>
      (await _channel.invokeMethod<int>('addReviewQuestions', {
        'bookId': bookId,
        'items':
            questions
                .map(
                  (item) => {'question': item.question, 'answer': item.answer},
                )
                .toList(),
      })) ??
      0;

  @override
  Future<void> updateReviewQuestion(ReviewQuestion question) =>
      _channel.invokeMethod<void>('updateReviewQuestion', question.toMap());

  @override
  Future<void> deleteReviewQuestion(String id) =>
      _channel.invokeMethod<void>('deleteReviewQuestion', {'id': id});

  @override Future<void> addQuestionBook(String title) => _channel.invokeMethod<void>('addQuestionBook', {'title': title});
  @override Future<void> updateQuestionBook(String id, String title) => _channel.invokeMethod<void>('updateQuestionBook', {'id': id, 'title': title});
  @override Future<void> deleteQuestionBook(String id) => _channel.invokeMethod<void>('deleteQuestionBook', {'id': id});
  @override Future<void> setSelectedQuestionBookIds(Set<String> ids) => _channel.invokeMethod<void>('setSelectedQuestionBookIds', {'ids': ids.toList()});
  @override Future<void> setReviewQuestionCount(int count) => _channel.invokeMethod<void>('setReviewQuestionCount', {'count': count});
  @override Future<void> setCheckpointEnabled(bool enabled) => _channel.invokeMethod<void>('setCheckpointEnabled', {'enabled': enabled});
  @override Future<void> setReviewCaptchaEnabled(bool enabled) => _channel.invokeMethod<void>('setReviewCaptchaEnabled', {'enabled': enabled});

  @override
  Future<MonthlyDashboard> loadMonthlyDashboard(int year, int month) async {
    final result = await _channel.invokeMapMethod<String, dynamic>(
      'getMonthlyDashboard',
      {'year': year, 'month': month},
    );
    if (result == null) {
      throw PlatformException(
        code: 'empty_dashboard',
        message: '分析データを取得できませんでした',
      );
    }
    return MonthlyDashboard.fromMap(result);
  }

  @override
  Future<TodayDashboard> loadTodayDashboard() async {
    final result = await _channel.invokeMapMethod<String, dynamic>(
      'getTodayDashboard',
    );
    if (result == null) {
      throw PlatformException(
        code: 'empty_today',
        message: '今日の利用状況を取得できませんでした',
      );
    }
    return TodayDashboard.fromMap(result);
  }

  @override
  Future<void> deleteUsageHistory() =>
      _channel.invokeMethod<void>('deleteUsageHistory');

  @override
  Future<void> deleteAllData() => _channel.invokeMethod<void>('deleteAllData');
}

extension on PurposeItemDraft {
  Map<String, dynamic> toMap() => {'label': label, 'categoryId': categoryId};
}

extension on ActivityCategoryDraft {
  Map<String, dynamic> toMap() => {'name': name};
}

extension on PurposeItem {
  Map<String, dynamic> toMap() => {
    'id': id,
    'categoryId': categoryId,
    'label': label,
    'isDefault': isDefault,
    'createdAt': createdAt,
  };
}

extension on ActivityCategory {
  Map<String, dynamic> toMap() => {
    'id': id,
    'name': name,
    'displayOrder': displayOrder,
    'isDefault': isDefault,
  };
}

extension<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
