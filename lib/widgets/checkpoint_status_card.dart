import 'package:flutter/material.dart';
import '../bridge.dart';
import 'app_ui.dart';

class CheckpointStatusCard extends StatelessWidget {
  const CheckpointStatusCard({
    super.key,
    required this.snapshot,
    this.onOpenSettings,
  });
  final AppSnapshot snapshot;
  final VoidCallback? onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final permission = snapshot.permissionSummary;
    final paused = snapshot.pauseSummary.isPaused;
    final healthy =
        permission.accessibilityEnabled &&
        permission.overlayEnabled &&
        permission.timerNotificationsEnabled &&
        permission.serviceRunning &&
        !paused;
    final needsSettings =
        !permission.accessibilityEnabled ||
        !permission.overlayEnabled ||
        !permission.timerNotificationsEnabled;
    final color = healthy ? appSuccess : appWarning;
    final background =
        healthy ? const Color(0xFFEAFBF4) : const Color(0xFFFFF7E8);
    final title =
        healthy
            ? '起動確認機能 稼働中'
            : paused
            ? '起動確認機能 一時停止中'
            : needsSettings
            ? '設定が必要です'
            : '起動確認機能を確認してください';
    final detail =
        healthy
            ? '正常に動作しています'
            : paused
            ? snapshot.pauseSummary.description
            : needsSettings
            ? '起動確認機能を利用するための設定が完了していません'
            : '設定画面で動作状態を確認してください';
    return InkWell(
      borderRadius: BorderRadius.circular(22),
      onTap: needsSettings ? onOpenSettings : null,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: color.withValues(alpha: .2)),
        ),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              child: Icon(
                healthy
                    ? Icons.check_rounded
                    : paused
                    ? Icons.pause_rounded
                    : Icons.priority_high_rounded,
                color: Colors.white,
                size: 30,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(
                      context,
                    ).textTheme.titleLarge?.copyWith(color: color),
                  ),
                  const SizedBox(height: 4),
                  Text(detail),
                ],
              ),
            ),
            if (needsSettings)
              const Icon(Icons.chevron_right_rounded, color: appWarning),
          ],
        ),
      ),
    );
  }
}
