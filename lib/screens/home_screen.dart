import 'package:flutter/material.dart';
import '../bridge.dart';
import '../theme/app_theme.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    required this.bridge,
    required this.snapshot,
    required this.onRefresh,
    required this.onOpenSettings,
    required this.onOpenAnalytics,
  });
  final NanishiniBridge bridge;
  final AppSnapshot snapshot;
  final Future<void> Function() onRefresh;
  final VoidCallback onOpenSettings;
  final VoidCallback onOpenAnalytics;
  @override
  State<HomeScreen> createState() => _HomeState();
}

class _HomeState extends State<HomeScreen> {
  late Future<TodayDashboard> today = widget.bridge.loadTodayDashboard();
  bool busy = false;
  @override
  void didUpdateWidget(covariant HomeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.snapshot != widget.snapshot) {
      today = widget.bridge.loadTodayDashboard();
    }
  }

  Future<void> refresh() async {
    await widget.onRefresh();
    final nextToday = widget.bridge.loadTodayDashboard();
    if (mounted) {
      setState(() {
        today = nextToday;
      });
    }
  }

  Future<void> run(Future<void> Function() action) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await action();
      await refresh();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('操作を完了できませんでした')));
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> pauseSheet() => showModalBottomSheet<void>(
    context: context,
    builder:
        (sheet) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('一時停止', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 8),
                for (final option in [(15, '15分'), (60, '1時間')])
                  ListTile(
                    title: Text(option.$2),
                    leading: const Icon(Icons.schedule_rounded),
                    onTap: () {
                      Navigator.pop(sheet);
                      run(() => widget.bridge.pauseForMinutes(option.$1));
                    },
                  ),
                ListTile(
                  title: const Text('次のアプリ起動まで'),
                  leading: const Icon(Icons.skip_next_rounded),
                  onTap: () {
                    Navigator.pop(sheet);
                    run(widget.bridge.pauseUntilNextAppOpen);
                  },
                ),
              ],
            ),
          ),
        ),
  );
  @override
  Widget build(BuildContext context) {
    final s = widget.snapshot;
    return Scaffold(
      appBar: AppBar(title: const Text('チェックポイント')),
      body: RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
          children: [
            _Status(
              snapshot: s,
              onSettings: widget.onOpenSettings,
              onResume:
                  s.pauseSummary.isPaused
                      ? () => run(widget.bridge.resumeFeature)
                      : null,
            ),
            if (s.activeSession != null) ...[
              const SizedBox(height: 28),
              _Active(session: s.activeSession!),
            ],
            const SizedBox(height: 36),
            Text('今日の振り返り', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 14),
            FutureBuilder<TodayDashboard>(
              future: today,
              builder: (c, v) {
                if (v.connectionState != ConnectionState.done) {
                  return const SizedBox(
                    height: 150,
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                if (v.hasError) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _quiet('今日の記録を読み込めませんでした'),
                      TextButton(
                        onPressed:
                            () => setState(() {
                              today = widget.bridge.loadTodayDashboard();
                            }),
                        child: const Text('もう一度読み込む'),
                      ),
                    ],
                  );
                }
                final d = v.data!;
                if (d.launchCount == 0) {
                  return _FirstUse(
                    snapshot: s,
                    onSettings: widget.onOpenSettings,
                  );
                }
                return _Today(data: d);
              },
            ),
            const SizedBox(height: 30),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('今日の記録を見る'),
              subtitle: const Text('利用の傾向と履歴を分析で確認できます'),
              trailing: const Icon(Icons.arrow_forward_rounded),
              onTap: widget.onOpenAnalytics,
            ),
            const Divider(),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: busy ? null : pauseSheet,
                icon: const Icon(Icons.pause_circle_outline_rounded),
                label: const Text('Checkpointを一時停止'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _quiet(String text) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 28),
    child: Text(text, style: Theme.of(context).textTheme.bodyLarge),
  );
}

class _Status extends StatelessWidget {
  const _Status({
    required this.snapshot,
    required this.onSettings,
    this.onResume,
  });
  final AppSnapshot snapshot;
  final VoidCallback onSettings;
  final VoidCallback? onResume;
  @override
  Widget build(BuildContext context) {
    final p = snapshot.permissionSummary;
    final missing = <String>[
      if (!p.accessibilityEnabled) 'アクセシビリティが無効です',
      if (!p.overlayEnabled) '他のアプリの上に表示する許可が必要です',
    ];
    final off = !snapshot.checkpointEnabled;
    final paused = snapshot.pauseSummary.isPaused;
    final healthy = !off && !paused && missing.isEmpty && p.serviceRunning;
    final title =
        off
            ? 'Checkpointはオフです'
            : paused
            ? '一時停止中'
            : missing.isNotEmpty
            ? '設定が必要です'
            : !p.serviceRunning
            ? '動作の確認が必要です'
            : 'Checkpointは有効です';
    final detail =
        off
            ? '設定から再びオンにできます'
            : paused
            ? snapshot.pauseSummary.description
            : missing.isNotEmpty
            ? missing.join('\n')
            : !p.serviceRunning
            ? '設定でアクセシビリティの状態を確認してください'
            : 'アプリを開く前に目的を確認します';
    return Semantics(
      button: !healthy,
      child: InkWell(
        onTap: healthy ? null : onSettings,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 16),
          decoration: const BoxDecoration(
            border: Border(bottom: BorderSide(color: checkpointBorder)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 10,
                height: 10,
                margin: const EdgeInsets.only(top: 7),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: healthy ? checkpointGreen : checkpointOrange,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 4),
                    Text(detail),
                  ],
                ),
              ),
              if (onResume != null)
                TextButton(onPressed: onResume, child: const Text('再開'))
              else if (!healthy)
                const Icon(Icons.chevron_right_rounded),
            ],
          ),
        ),
      ),
    );
  }
}

class _Today extends StatelessWidget {
  const _Today({required this.data});
  final TodayDashboard data;
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        '${data.purposeRate}%',
        style: Theme.of(context).textTheme.headlineMedium?.copyWith(
          fontSize: 40,
          color: checkpointGreen,
        ),
      ),
      const SizedBox(height: 6),
      Text(
        '今日 ${data.launchCount}回中 ${data.purposeCount}回\n目的を決めて開きました',
        style: Theme.of(context).textTheme.titleMedium?.copyWith(height: 1.5),
      ),
      const SizedBox(height: 10),
      Text(
        '目的を決めずに開いた回数 ${data.noPurposeCount}回',
        style: Theme.of(context).textTheme.bodySmall,
      ),
    ],
  );
}

class _Active extends StatelessWidget {
  const _Active({required this.session});
  final ActiveSessionSummary session;
  @override
  Widget build(BuildContext context) {
    final total = (session.remainingMillis / 1000).ceil().clamp(0, 99999);
    final clock =
        '${(total ~/ 60).toString().padLeft(2, '0')}:${(total % 60).toString().padLeft(2, '0')}';
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: checkpointSoftGreen,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            session.applicationLabel,
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 6),
          Text(
            session.purposeText.isEmpty ? '目的未設定' : session.purposeText,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 12),
          Text('残り $clock', style: Theme.of(context).textTheme.titleMedium),
        ],
      ),
    );
  }
}

class _FirstUse extends StatelessWidget {
  const _FirstUse({required this.snapshot, required this.onSettings});
  final AppSnapshot snapshot;
  final VoidCallback onSettings;

  @override
  Widget build(BuildContext context) {
    final selected = snapshot.targetApps.where((app) => app.enabled).toList();
    final permission = snapshot.permissionSummary;
    final ready =
        snapshot.checkpointEnabled &&
        !snapshot.pauseSummary.isPaused &&
        permission.accessibilityEnabled &&
        permission.overlayEnabled &&
        permission.serviceRunning;
    final nextStep =
        !ready
            ? '上の動作状態を確認して、設定を整えましょう。'
            : selected.isEmpty
            ? '設定の「Checkpointを使うアプリ」で、確認したいアプリを選びましょう。'
            : '${selected.first.label}を開くと、目的の確認が始まります。';
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: checkpointSoftGreen,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('調べたら、勉強に戻ろう。', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          const Text('今日はまだCheckpointを通っていません'),
          const SizedBox(height: 18),
          const Text(
            '1. 戻りたいことを決める\n   例：資格試験の問題集を解く\n2. 対象アプリで、目的と時間を選ぶ\n   例：用語を調べる・3分\n3. 時間になったら、戻り先を確認する',
          ),
          const SizedBox(height: 18),
          Text(nextStep, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: onSettings,
            icon: const Icon(Icons.tune_rounded),
            label: Text(!ready ? '動作設定を確認する' : '対象アプリを確認する'),
          ),
          const SizedBox(height: 8),
          const Text('復習問題は、設定でオン・オフを選べます。'),
        ],
      ),
    );
  }
}
