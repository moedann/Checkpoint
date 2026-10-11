import 'package:flutter/material.dart';
import '../bridge.dart';
import '../theme/app_theme.dart';

class IntroScreen extends StatelessWidget {
  const IntroScreen({
    super.key,
    required this.bridge,
    required this.permissions,
    required this.onAccepted,
  });
  final NanishiniBridge bridge;
  final PermissionSummary permissions;
  final Future<void> Function() onAccepted;

  @override
  Widget build(BuildContext context) {
    final completed =
        [
          permissions.accessibilityEnabled,
          permissions.overlayEnabled,
          permissions.timerNotificationsEnabled,
        ].where((value) => value).length;
    final button =
        !permissions.accessibilityEnabled
            ? 'アプリの起動検知を設定する'
            : !permissions.overlayEnabled
            ? '目的確認の表示を設定する'
            : !permissions.timerNotificationsEnabled
            ? 'タイマー通知を設定する'
            : 'Checkpointをはじめる';
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 560),
            child: ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const SizedBox(height: 20),
                const Icon(
                  Icons.check_circle_outline_rounded,
                  size: 52,
                  color: checkpointGreen,
                ),
                const SizedBox(height: 18),
                Text(
                  'Checkpoint',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 12),
                Text(
                  '調べたら、勉強に戻ろう。',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 12),
                Text(
                  '解説動画や調べものの前に、目的と時間を決める。\n時間が来たら、やるはずだった勉強を思い出す。',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 28),
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: checkpointSoftGreen,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'たとえば、数学の勉強中なら',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      const _Step('1', '二次関数の解説を見るために開く'),
                      const _Step('2', '今回は10分と決める'),
                      const _Step('3', '時間が来たら「数学の宿題」を思い出す'),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                const Text('自分の問題集から復習問題を出すこともできます。設定でオン・オフを選べます。'),
                const SizedBox(height: 28),
                Text(
                  'はじめる準備  $completed / 3',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 8),
                const Text(
                  '以下の権限で、対象アプリの起動を検知し、目的確認の画面とタイマー通知を表示します。Androidの設定からいつでも解除できます。',
                ),
                const SizedBox(height: 12),
                _Permission(
                  title: 'アクセシビリティ',
                  reason: '対象アプリの起動を検知します。画面内の文章・メッセージ・パスワードは読み取りません。',
                  enabled: permissions.accessibilityEnabled,
                  onTap: bridge.openAccessibilitySettings,
                ),
                _Permission(
                  title: '他のアプリの上に表示',
                  reason: '目的確認の画面を表示します。',
                  enabled: permissions.overlayEnabled,
                  onTap: bridge.openOverlaySettings,
                ),
                _Permission(
                  title: '通知',
                  reason: '利用中の残り時間を通知で表示します。',
                  enabled: permissions.timerNotificationsEnabled,
                  onTap:
                      permissions.timerNotificationsEnabled
                          ? bridge.openNotificationSettings
                          : bridge.requestNotificationPermission,
                ),
                const SizedBox(height: 16),
                const Text(
                  '入力した目的や利用履歴、復習問題は端末内に保存します。外部サーバーには送信しません。',
                  style: TextStyle(color: checkpointMuted, height: 1.5),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  key: const ValueKey('accept_intro_button'),
                  onPressed: () async {
                    try {
                      if (!permissions.accessibilityEnabled) {
                        await bridge.openAccessibilitySettings();
                        return;
                      }
                      if (!permissions.overlayEnabled) {
                        await bridge.openOverlaySettings();
                        return;
                      }
                      if (!permissions.timerNotificationsEnabled) {
                        await bridge.requestNotificationPermission();
                        return;
                      }
                      await bridge.acceptIntro();
                      await onAccepted();
                    } catch (_) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('設定を完了できませんでした。もう一度お試しください。'),
                          ),
                        );
                      }
                    }
                  },
                  child: Text(button),
                ),
                const SizedBox(height: 12),
                const Text(
                  '設定から戻ると、許可の状態を確認します。',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: checkpointMuted),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Step extends StatelessWidget {
  const _Step(this.number, this.text);
  final String number, text;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 9),
    child: Row(
      children: [
        CircleAvatar(
          radius: 16,
          backgroundColor: Colors.white,
          foregroundColor: checkpointGreen,
          child: Text(number),
        ),
        const SizedBox(width: 14),
        Expanded(child: Text(text)),
      ],
    ),
  );
}

class _Permission extends StatelessWidget {
  const _Permission({
    required this.title,
    required this.reason,
    required this.enabled,
    required this.onTap,
  });
  final String title, reason;
  final bool enabled;
  final Future<void> Function() onTap;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(
      enabled ? Icons.check_circle_rounded : Icons.circle_outlined,
      color: enabled ? checkpointGreen : checkpointMuted,
    ),
    title: Text(title),
    subtitle: Text(reason),
    trailing:
        enabled
            ? const Text('許可済み')
            : TextButton(
              onPressed: () async {
                try {
                  await onTap();
                } catch (_) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('設定を開けませんでした。もう一度お試しください。')),
                    );
                  }
                }
              },
              child: const Text('設定'),
            ),
  );
}
