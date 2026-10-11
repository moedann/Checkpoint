import 'package:flutter/material.dart';
import '../bridge.dart';
import '../widgets/app_ui.dart';
import 'management_screens.dart';
import 'review_questions_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    super.key,
    required this.bridge,
    required this.snapshot,
    required this.onRefresh,
  });
  final NanishiniBridge bridge;
  final AppSnapshot snapshot;
  final Future<void> Function() onRefresh;
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _busy = false;
  Future<void> _run(Future<void> Function() action) async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      await action();
      await widget.onRefresh();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('操作を完了できませんでした')));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _open(Widget screen) async {
    await Navigator.push<void>(
      context,
      MaterialPageRoute(builder: (_) => screen),
    );
    await widget.onRefresh();
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.snapshot.permissionSummary;
    return Scaffold(
      appBar: AppBar(title: const Text('設定')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
        children: [
          _SettingsGroup(
            title: 'Checkpoint',
            children: [
              SwitchListTile(
                secondary: const Icon(Icons.check_circle_outline_rounded),
                title: const Text('Checkpointを使用'),
                subtitle: Text(widget.snapshot.checkpointEnabled ? 'アプリを開く前に目的を確認します' : '確認画面を表示しません'),
                value: widget.snapshot.checkpointEnabled,
                onChanged: _busy ? null : (v) => _run(() => widget.bridge.setCheckpointEnabled(v)),
              ),
              _SettingsTile(
                icon: Icons.apps_rounded,
                title: 'Checkpointを使うアプリ',
                subtitle: widget.snapshot.targetApps.isEmpty
                    ? '対応アプリがインストールされていません'
                    : '${widget.snapshot.targetApps.where((app) => app.enabled).length}個を選択中',
                onTap: () => _showTargetApps(context),
              ),
              _SettingsTile(
                icon: Icons.pause_circle_outline_rounded,
                title: '一時停止',
                subtitle: widget.snapshot.pauseSummary.description,
                onTap:
                    () => _open(
                      PauseSettingsScreen(
                        bridge: widget.bridge,
                        snapshot: widget.snapshot,
                      ),
                    ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _SettingsGroup(
            title: '復習CAPTCHA',
            children: [
              SwitchListTile(
                secondary: const Icon(Icons.quiz_outlined),
                title: const Text('復習問題を使用'),
                subtitle: Text(widget.snapshot.reviewCaptchaEnabled ? '目的と時間を決めた後に出題します' : '復習問題をスキップします'),
                value: widget.snapshot.reviewCaptchaEnabled,
                onChanged: _busy ? null : (v) => _run(() => widget.bridge.setReviewCaptchaEnabled(v)),
              ),
              _SettingsTile(icon:Icons.library_books_outlined,title:'使用する問題冊子',subtitle:widget.snapshot.selectedQuestionBookIds.isEmpty?'選択なし':'${widget.snapshot.selectedQuestionBookIds.length}冊',onTap:()=>_open(ReviewCaptchaSettingsScreen(bridge:widget.bridge))),
              _SettingsTile(icon:Icons.format_list_numbered_rounded,title:'1回の出題数',subtitle:'${widget.snapshot.reviewQuestionCount}問',onTap:()=>_open(ReviewCaptchaSettingsScreen(bridge:widget.bridge))),
            ],
          ),
          const SizedBox(height: 20),
          _SettingsGroup(
            title: '入力',
            children: [
              _SettingsTile(
                icon: Icons.sell_outlined,
                title: '利用目的管理',
                onTap:
                    () => _open(PurposeManagementScreen(bridge: widget.bridge)),
              ),
              _SettingsTile(
                icon: Icons.format_list_bulleted_rounded,
                title: '行動ジャンル管理',
                onTap:
                    () =>
                        _open(ActivityManagementScreen(bridge: widget.bridge)),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _SettingsGroup(
            title: '権限と診断',
            children: [
              _SettingsTile(
                icon: Icons.accessibility_new_rounded,
                title: 'アクセシビリティ',
                subtitle: p.accessibilityEnabled ? '有効' : '設定が必要',
                warning: !p.accessibilityEnabled,
                onTap: () => _run(widget.bridge.openAccessibilitySettings),
              ),
              _SettingsTile(
                icon: Icons.layers_outlined,
                title: 'オーバーレイ',
                subtitle: p.overlayEnabled ? '有効' : '設定が必要',
                warning: !p.overlayEnabled,
                onTap: () => _run(widget.bridge.openOverlaySettings),
              ),
              _SettingsTile(
                icon: Icons.notifications_none_rounded,
                title: '通知',
                subtitle: p.timerNotificationsEnabled ? '有効' : '設定が必要',
                warning: !p.timerNotificationsEnabled,
                onTap: () => _run(p.timerNotificationsEnabled ? widget.bridge.openNotificationSettings : widget.bridge.requestNotificationPermission),
              ),
              _SettingsTile(
                icon: Icons.health_and_safety_outlined,
                title: '動作診断',
                onTap:
                    () => _open(
                      DiagnosticsScreen(
                        bridge: widget.bridge,
                        snapshot: widget.snapshot,
                      ),
                    ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _SettingsGroup(
            title: 'データ',
            children: [
              _SettingsTile(
                icon: Icons.shield_outlined,
                title: 'プライバシー',
                onTap:
                    () => _open(
                      PrivacyScreen(
                        summary: widget.snapshot.privacyConfiguration.summary,
                      ),
                    ),
              ),
              _SettingsTile(
                icon: Icons.delete_sweep_outlined,
                title: '利用履歴を削除',
                onTap:
                    () => _confirm(
                      '利用履歴をすべて削除しますか？',
                      widget.bridge.deleteUsageHistory,
                    ),
              ),
              _SettingsTile(
                icon: Icons.delete_forever_outlined,
                title: '全データを削除',
                danger: true,
                onTap:
                    () => _confirm(
                      '設定を含む全データを削除しますか？',
                      widget.bridge.deleteAllData,
                    ),
              ),
            ],
          ),
          const SizedBox(height: 28),
          Center(child: Text('Checkpoint\nv${widget.snapshot.versionName}',textAlign:TextAlign.center,style:Theme.of(context).textTheme.bodySmall)),
        ],
      ),
    );
  }

  Future<void> _confirm(String title, Future<void> Function() action) async {
    final ok = await showDialog<bool>(
      context: context,
      builder:
          (_) => AlertDialog(
            title: Text(title),
            content: const Text('この操作は元に戻せません。'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('キャンセル'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('削除する'),
              ),
            ],
          ),
    );
    if (ok == true) {
      await _run(action);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(title.startsWith('利用履歴') ? '利用履歴を削除しました' : '全データを削除しました')));
    }
  }

  Future<void> _showTargetApps(BuildContext context) async {
    final selected = widget.snapshot.targetApps
        .where((app) => app.enabled)
        .map((app) => app.packageName)
        .toSet();
    final saved = await showDialog<Set<String>>(
      context: context,
      builder:
          (dialogContext) => StatefulBuilder(
            builder:
                (context, update) => AlertDialog(
                  title: const Text('Checkpointを使うアプリ'),
                  content: SizedBox(
                    width: double.maxFinite,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Checkpointを表示するアプリを選択します。現在はSNSとYouTubeに対応しています。'),
                        const SizedBox(height: 12),
                        Flexible(
                          child: widget.snapshot.targetApps.isEmpty
                              ? const EmptyState('対応アプリがインストールされていません')
                              : ListView.builder(
                                  shrinkWrap: true,
                                  itemCount: widget.snapshot.targetApps.length,
                                  itemBuilder: (_, i) {
                                    final app = widget.snapshot.targetApps[i];
                                    return SwitchListTile(
                                      value: selected.contains(app.packageName),
                                      title: Text(app.label),
                                      subtitle: Text(
                                        app.packageName,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      secondary: app.iconBytes == null
                                          ? const Icon(Icons.apps_rounded)
                                          : Image.memory(
                                              app.iconBytes!,
                                              width: 38,
                                              height: 38,
                                            ),
                                      onChanged: (value) => update(
                                        () => value == true
                                            ? selected.add(app.packageName)
                                            : selected.remove(app.packageName),
                                      ),
                                    );
                                  },
                                ),
                        ),
                      ],
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(dialogContext),
                      child: const Text('キャンセル'),
                    ),
                    FilledButton(
                      onPressed: () => Navigator.pop(dialogContext, selected),
                      child: const Text('保存'),
                    ),
                  ],
                ),
          ),
    );
    if (saved != null) {
      await _run(() => widget.bridge.setEnabledTargetPackages(saved));
    }
  }
}

class _SettingsGroup extends StatelessWidget {
  const _SettingsGroup({required this.title, required this.children});
  final String title;
  final List<Widget> children;
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Padding(
        padding: const EdgeInsets.only(left: 4, bottom: 8),
        child: Text(title, style: Theme.of(context).textTheme.titleMedium),
      ),
      Column(
          children: [
            for (var i = 0; i < children.length; i++) ...[
              children[i],
              if (i < children.length - 1) const Divider(height: 1, indent: 58),
            ],
          ],
        ),
    ],
  );
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.title,
    this.subtitle,
    this.onTap,
    this.warning = false,
    this.danger = false,
  });
  final IconData icon;
  final String title;
  final String? subtitle;
  final VoidCallback? onTap;
  final bool warning, danger;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 3),
    leading: Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: danger ? const Color(0xFFFFEEEE) : const Color(0xFFE6F1FF),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(icon, color: danger ? Colors.red : appPrimary, size: 21),
    ),
    title: Text(title, style: TextStyle(color: danger ? Colors.red : null)),
    subtitle:
        subtitle == null
            ? null
            : Text(
              subtitle!,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: warning ? appWarning : null,
                fontWeight: warning ? FontWeight.w600 : null,
              ),
            ),
    trailing: onTap == null ? null : const Icon(Icons.chevron_right_rounded),
    onTap: onTap,
  );
}

class PauseSettingsScreen extends StatefulWidget {
  const PauseSettingsScreen({
    super.key,
    required this.bridge,
    required this.snapshot,
  });
  final NanishiniBridge bridge;
  final AppSnapshot snapshot;
  @override
  State<PauseSettingsScreen> createState() => _PauseSettingsScreenState();
}

class _PauseSettingsScreenState extends State<PauseSettingsScreen> {
  late FeaturePauseSummary pause = widget.snapshot.pauseSummary;
  bool busy = false;
  Future<void> run(Future<void> Function() action) async {
    if (busy) return;
    setState(() => busy = true);
    await action();
    final value = await widget.bridge.loadSnapshot();
    if (mounted) {
      setState(() {
        pause = value.pauseSummary;
        busy = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('一時停止')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppSectionCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SectionTitle('現在の状態'),
              const SizedBox(height: 8),
              Text(pause.description),
            ],
          ),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed:
              busy ? null : () => run(() => widget.bridge.pauseForMinutes(15)),
          icon: const Icon(Icons.pause_rounded),
          label: const Text('15分停止'),
        ),
        OutlinedButton(
          onPressed:
              busy ? null : () => run(() => widget.bridge.pauseForMinutes(60)),
          child: const Text('1時間停止'),
        ),
        OutlinedButton(
          onPressed:
              busy ? null : () => run(widget.bridge.pauseUntilNextAppOpen),
          child: const Text('次のアプリ起動まで'),
        ),
        if (pause.isPaused)
          FilledButton.tonalIcon(
            onPressed: busy ? null : () => run(widget.bridge.resumeFeature),
            icon: const Icon(Icons.play_arrow_rounded),
            label: const Text('再開'),
          ),
      ],
    ),
  );
}

class DiagnosticsScreen extends StatelessWidget {
  const DiagnosticsScreen({
    super.key,
    required this.bridge,
    required this.snapshot,
  });
  final NanishiniBridge bridge;
  final AppSnapshot snapshot;
  @override
  Widget build(BuildContext context) {
    final p = snapshot.permissionSummary;
    return Scaffold(
      appBar: AppBar(title: const Text('動作診断')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppSectionCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                _DiagnosticRow(
                  'アクセシビリティ',
                  p.accessibilityEnabled ? '有効' : '設定が必要',
                  onTap: bridge.openAccessibilitySettings,
                ),
                _DiagnosticRow(
                  'オーバーレイ',
                  p.overlayEnabled ? '有効' : '設定が必要',
                  onTap: bridge.openOverlaySettings,
                ),
                _DiagnosticRow(
                  '通知',
                  p.timerNotificationsEnabled ? '有効' : '設定が必要',
                  onTap:
                      p.timerNotificationsEnabled
                          ? bridge.openNotificationSettings
                          : bridge.requestNotificationPermission,
                ),
                _DiagnosticRow('Live Update', switch (p.liveUpdateStatus) {
                  'AVAILABLE' => '利用可能',
                  'USER_DISABLED' => 'ユーザー設定で無効',
                  _ => '利用不可',
                }),
                _DiagnosticRow('サービス', p.serviceRunning ? '稼働中' : '停止中'),
                _DiagnosticRow(
                  '最後に検知したアプリ',
                  p.lastDetectedApp.isEmpty ? '未検知' : p.lastDetectedApp,
                ),
                _DiagnosticRow('除外判定', p.exclusionStatus, last: true),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DiagnosticRow extends StatelessWidget {
  const _DiagnosticRow(this.label, this.value, {this.onTap, this.last = false});
  final String label, value;
  final Future<void> Function()? onTap;
  final bool last;
  @override
  Widget build(BuildContext context) => Column(
    children: [
      ListTile(
        title: Text(label),
        subtitle: Text(value),
        trailing:
            onTap == null
                ? null
                : const Icon(Icons.open_in_new_rounded, size: 18),
        onTap: onTap == null ? null : () => onTap!(),
      ),
      if (!last) const Divider(height: 1, indent: 16),
    ],
  );
}

class PrivacyScreen extends StatelessWidget {
  const PrivacyScreen({super.key, required this.summary});
  final String summary;
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('プライバシー')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppSectionCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.shield_outlined, color: appPrimary, size: 38),
              const SizedBox(height: 16),
              Text(summary, style: Theme.of(context).textTheme.bodyLarge),
              const SizedBox(height: 16),
              const Text('保存期間：無期限（端末内のみ）'),
            ],
          ),
        ),
      ],
    ),
  );
}
