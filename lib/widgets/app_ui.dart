import 'package:flutter/material.dart';

const appBackground = Color(0xFFF6F8FC);
const appPrimary = Color(0xFF0877F9);
const appNavy = Color(0xFF17213C);
const appSuccess = Color(0xFF18A875);
const appWarning = Color(0xFFF59E0B);

class AppSectionCard extends StatelessWidget {
  const AppSectionCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(18),
  });
  final Widget child;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) => Material(
    color: Colors.white,
    elevation: 1,
    shadowColor: const Color(0x1417213C),
    surfaceTintColor: Colors.transparent,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(20),
      side: const BorderSide(color: Color(0xFFE7EAF1)),
    ),
    clipBehavior: Clip.antiAlias,
    child: Padding(padding: padding, child: child),
  );
}

class SectionTitle extends StatelessWidget {
  const SectionTitle(this.text, {super.key, this.trailing});
  final String text;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Expanded(
        child: Text(text, style: Theme.of(context).textTheme.titleLarge),
      ),
      if (trailing != null) trailing!,
    ],
  );
}

class EmptyState extends StatelessWidget {
  const EmptyState(this.text, {super.key});
  final String text;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 16),
    child: Row(
      children: [
        const Icon(Icons.inbox_outlined, color: Color(0xFF8A94A8)),
        const SizedBox(width: 10),
        Expanded(
          child: Text(text, style: Theme.of(context).textTheme.bodyMedium),
        ),
      ],
    ),
  );
}

String formatDuration(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  if (hours > 0) return '$hours時間$minutes分';
  final remaining = seconds % 60;
  return minutes > 0 ? '$minutes分$remaining秒' : '$remaining秒';
}

String formatClock(int epochMillis) {
  final value = DateTime.fromMillisecondsSinceEpoch(epochMillis);
  return '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
}

String percentChange(num current, num previous) {
  if (previous == 0) return current == 0 ? '前月比 0%' : '前月データなし';
  final value = (current - previous) * 100 / previous;
  return '前月比 ${value >= 0 ? '+' : ''}${value.toStringAsFixed(1)}%';
}
