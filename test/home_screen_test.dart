import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nanisini/bridge.dart';
import 'package:nanisini/screens/home_screen.dart';
import 'widget_test.dart' show FakeBridge;

AppSnapshot snapshot({bool running = true}) => AppSnapshot(
  introAccepted: true,
  permissionSummary: PermissionSummary(
    accessibilityEnabled: true,
    overlayEnabled: true,
    serviceRunning: running,
    lastDetectedApp: '',
    exclusionStatus: '成功',
  ),
  pauseSummary: const FeaturePauseSummary(description: ''),
  purposeCategories: const [],
  purposeItems: const [],
  activityCategories: const [],
  privacyConfiguration: const PrivacyConfiguration(summary: ''),
  targetApps: const [
    TargetApp(packageName: 'browser', label: 'Chrome', enabled: true),
  ],
);

class DashboardBridge extends FakeBridge {
  DashboardBridge() : super(snapshot: snapshot());
  bool fail = false;
  int launches = 0;
  @override
  Future<TodayDashboard> loadTodayDashboard() async {
    if (fail) throw StateError('unavailable');
    return TodayDashboard(
      launchCount: launches,
      purposeCount: launches,
      noPurposeCount: 0,
      topPurposes: const [],
      recent: const [],
    );
  }
}

void main() {
  testWidgets('stopped service is not shown as enabled and links to settings', (
    tester,
  ) async {
    var settingsOpened = false;
    final bridge = DashboardBridge();
    await tester.pumpWidget(
      MaterialApp(
        home: HomeScreen(
          bridge: bridge,
          snapshot: snapshot(running: false),
          onRefresh: () async {},
          onOpenSettings: () => settingsOpened = true,
          onOpenAnalytics: () {},
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('動作の確認が必要です'), findsOneWidget);
    expect(find.text('Checkpointは有効です'), findsNothing);
    await tester.tap(find.text('動作の確認が必要です'));
    expect(settingsOpened, isTrue);
  });

  testWidgets(
    'first use suggests selected app and returning refreshes records',
    (tester) async {
      final bridge = DashboardBridge();
      Widget screen(AppSnapshot state) => MaterialApp(
        home: HomeScreen(
          bridge: bridge,
          snapshot: state,
          onRefresh: () async {},
          onOpenSettings: () {},
          onOpenAnalytics: () {},
        ),
      );
      await tester.pumpWidget(screen(snapshot()));
      await tester.pumpAndSettle();
      expect(find.text('Chromeを開くと、目的の確認が始まります。'), findsOneWidget);
      bridge.launches = 1;
      await tester.pumpWidget(screen(snapshot()));
      await tester.pumpAndSettle();
      expect(find.text('今日 1回中 1回\n目的を決めて開きました'), findsOneWidget);
      expect(find.text('今日はまだCheckpointを通っていません'), findsNothing);
    },
  );

  testWidgets('failed dashboard can be retried', (tester) async {
    final bridge = DashboardBridge()..fail = true;
    await tester.pumpWidget(
      MaterialApp(
        home: HomeScreen(
          bridge: bridge,
          snapshot: snapshot(),
          onRefresh: () async {},
          onOpenSettings: () {},
          onOpenAnalytics: () {},
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('今日の記録を読み込めませんでした'), findsOneWidget);
    bridge.fail = false;
    await tester.tap(find.text('もう一度読み込む'));
    await tester.pumpAndSettle();
    expect(find.text('今日はまだCheckpointを通っていません'), findsOneWidget);
  });
}
