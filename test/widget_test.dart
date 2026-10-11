import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:nanisini/bridge.dart';
import 'package:nanisini/main.dart';

class FakeBridge extends NanishiniBridge {
  FakeBridge({required this.snapshot});

  @override
  Future<void> setEnabledTargetPackages(Set<String> packageNames) async {}

  AppSnapshot snapshot;
  int introAcceptedCount = 0;

  @override
  Future<AppSnapshot> loadSnapshot() async => snapshot;

  @override
  Future<void> acceptIntro() async {
    introAcceptedCount += 1;
    snapshot = AppSnapshot(
      introAccepted: true,
      permissionSummary: snapshot.permissionSummary,
      pauseSummary: snapshot.pauseSummary,
      purposeCategories: snapshot.purposeCategories,
      purposeItems: snapshot.purposeItems,
      activityCategories: snapshot.activityCategories,
      privacyConfiguration: snapshot.privacyConfiguration,
    );
  }

  @override
  Future<void> addActivityCategory(ActivityCategoryDraft draft) async {}

  @override
  Future<void> addPurposeItem(PurposeItemDraft draft) async {}

  @override
  Future<void> deleteActivityCategory(String id) async {}

  @override
  Future<void> deletePurposeItem(String id) async {}

  @override
  Future<void> deleteAllData() async {}

  @override
  Future<void> deleteUsageHistory() async {}

  @override
  Future<MonthlyDashboard> loadMonthlyDashboard(int year, int month) async =>
      MonthlyDashboard(
        year: year,
        month: month,
        totalUsageSeconds: 0,
        launchCount: 0,
        previousUsageSeconds: 0,
        previousLaunchCount: 0,
        daily: const [],
        topByUsage: const [],
        topByLaunches: const [],
        purposeCategories: const [],
        purposes: const [],
        noPurposeCount: 0,
        previousNoPurposeCount: 0,
        noPurposeApps: const [],
        activityCategories: const [],
        activityApps: const {},
        recent: const [],
        earliestMonth: null,
      );

  @override
  Future<void> openAccessibilitySettings() async {}

  @override
  Future<void> openOverlaySettings() async {}

  @override
  Future<void> requestNotificationPermission() async {}

  @override
  Future<void> openNotificationSettings() async {}

  @override
  Future<void> pauseForMinutes(int minutes) async {}

  @override
  Future<void> pauseUntilNextAppOpen() async {}

  @override
  Future<void> resumeFeature() async {}

  @override
  Future<void> runOverlayTest() async {}

  @override
  Future<void> updateActivityCategory(ActivityCategory item) async {}

  @override
  Future<void> updatePurposeItem(PurposeItem item) async {}
}

void main() {
  testWidgets('first launch explains the value before entering the app', (
    tester,
  ) async {
    final bridge = FakeBridge(
      snapshot: AppSnapshot(
        introAccepted: false,
        permissionSummary: const PermissionSummary(
          accessibilityEnabled: true,
          overlayEnabled: true,
          serviceRunning: false,
          lastDetectedApp: '',
          exclusionStatus: '未確認',
        ),
        pauseSummary: const FeaturePauseSummary(description: '一時停止はありません。'),
        purposeCategories: const [
          PurposeCategory(
            id: 'contact',
            name: '連絡',
            displayOrder: 1,
            isDefault: true,
          ),
        ],
        purposeItems: const [
          PurposeItem(
            id: 'purpose-1',
            categoryId: 'contact',
            label: 'メッセージを確認する',
            isDefault: true,
            createdAt: 0,
          ),
        ],
        activityCategories: const [
          ActivityCategory(
            id: 'activity-1',
            name: '仕事',
            displayOrder: 1,
            isDefault: true,
          ),
        ],
        privacyConfiguration: const PrivacyConfiguration(
          summary: '利用履歴は端末内に保存し、外部サーバーには送信しません。',
        ),
      ),
    );

    await tester.pumpWidget(NanishiniApp(bridge: bridge));
    await tester.pumpAndSettle();

    expect(find.byType(NavigationBar), findsNothing);
    expect(find.text('調べたら、勉強に戻ろう。'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('accept_intro_button')),
      300,
    );
    await tester.tap(find.byKey(const ValueKey('accept_intro_button')));
    await tester.pumpAndSettle();
    expect(bridge.introAcceptedCount, 0);
    bridge.snapshot = AppSnapshot(
      introAccepted: false,
      permissionSummary: const PermissionSummary(
        accessibilityEnabled: true,
        overlayEnabled: true,
        serviceRunning: true,
        lastDetectedApp: '',
        exclusionStatus: '成功',
        timerNotificationsEnabled: true,
      ),
      pauseSummary: bridge.snapshot.pauseSummary,
      purposeCategories: bridge.snapshot.purposeCategories,
      purposeItems: bridge.snapshot.purposeItems,
      activityCategories: bridge.snapshot.activityCategories,
      privacyConfiguration: bridge.snapshot.privacyConfiguration,
    );
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('accept_intro_button')),
      300,
    );
    await tester.tap(find.byKey(const ValueKey('accept_intro_button')));
    await tester.pumpAndSettle();
    expect(bridge.introAcceptedCount, 1);
    expect(find.byType(NavigationBar), findsOneWidget);
  });

  test('snapshot resolves category names', () {
    final snapshot = AppSnapshot(
      introAccepted: true,
      permissionSummary: const PermissionSummary(
        accessibilityEnabled: true,
        overlayEnabled: true,
        serviceRunning: true,
        lastDetectedApp: 'Instagram',
        exclusionStatus: '成功',
      ),
      pauseSummary: const FeaturePauseSummary(description: '15分停止中'),
      purposeCategories: const [
        PurposeCategory(
          id: 'contact',
          name: '連絡',
          displayOrder: 1,
          isDefault: true,
        ),
      ],
      purposeItems: const [
        PurposeItem(
          id: 'purpose-1',
          categoryId: 'contact',
          label: 'メッセージを確認する',
          isDefault: true,
          createdAt: 0,
        ),
      ],
      activityCategories: const [
        ActivityCategory(
          id: 'activity-1',
          name: '仕事',
          displayOrder: 1,
          isDefault: true,
        ),
      ],
      privacyConfiguration: const PrivacyConfiguration(
        summary: '利用履歴は端末内に保存し、外部サーバーには送信しません。',
      ),
    );

    expect(snapshot.categoryNameById('contact'), '連絡');
    expect(snapshot.categoryNameById('missing'), '未設定');
  });

  testWidgets('four-tab shell renders home status and navigation', (
    tester,
  ) async {
    final bridge = FakeBridge(
      snapshot: AppSnapshot(
        introAccepted: true,
        permissionSummary: const PermissionSummary(
          accessibilityEnabled: false,
          overlayEnabled: false,
          serviceRunning: false,
          lastDetectedApp: '',
          exclusionStatus: '未確認',
        ),
        pauseSummary: const FeaturePauseSummary(description: '一時停止はありません。'),
        purposeCategories: const [],
        purposeItems: const [],
        activityCategories: const [],
        privacyConfiguration: const PrivacyConfiguration(
          summary: '利用履歴は端末内に保存します。',
        ),
      ),
    );

    await tester.pumpWidget(NanishiniApp(bridge: bridge));
    await tester.pumpAndSettle();

    expect(find.text('今日の振り返り'), findsOneWidget);
    expect(find.text('今日はまだCheckpointを通っていません'), findsOneWidget);
    expect(find.text('問題'), findsOneWidget);
    expect(find.text('設定が必要です'), findsOneWidget);
    expect(find.text('ホーム'), findsOneWidget);
    expect(find.text('分析'), findsOneWidget);
    expect(find.text('設定'), findsOneWidget);
  });
}
