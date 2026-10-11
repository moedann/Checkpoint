import 'package:flutter/material.dart';
import 'bridge.dart';
import 'screens/app_shell.dart';
import 'screens/intro_screen.dart';
import 'theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const NanishiniApp());
}

class NanishiniApp extends StatefulWidget {
  const NanishiniApp({
    super.key,
    this.bridge = const MethodChannelNanishiniBridge(),
  });
  final NanishiniBridge bridge;
  @override
  State<NanishiniApp> createState() => _NanishiniAppState();
}

class _NanishiniAppState extends State<NanishiniApp>
    with WidgetsBindingObserver {
  late Future<AppSnapshot> future;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    future = widget.bridge.loadSnapshot();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _startRefresh();
  }

  void _startRefresh() => setState(() {
    future = widget.bridge.loadSnapshot();
  });
  Future<void> _refresh() async {
    _startRefresh();
    await future;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'チェックポイント',
      theme: buildAppTheme(),
      home: FutureBuilder<AppSnapshot>(
        future: future,
        builder: (context, value) {
          if (value.hasError) {
            return _ErrorScreen(
              error: value.error.toString(),
              onRetry: _refresh,
            );
          }
          if (!value.hasData) return const _LoadingScreen();
          final snapshot = value.data!;
          if (!snapshot.introAccepted) {
            return IntroScreen(bridge: widget.bridge, permissions: snapshot.permissionSummary, onAccepted: _refresh);
          }
          return AppShell(
            bridge: widget.bridge,
            snapshot: snapshot,
            onRefresh: _refresh,
          );
        },
      ),
    );
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();
  @override
  Widget build(BuildContext context) => const Scaffold(body: Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
    Icon(Icons.check_circle_outline_rounded, size: 48, color: checkpointGreen), SizedBox(height: 14),
    Text('Checkpoint', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w700)), SizedBox(height: 18),
    SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.5)),
  ])));
}

class _ErrorScreen extends StatelessWidget {
  const _ErrorScreen({required this.error, required this.onRetry});
  final String error;
  final Future<void> Function() onRetry;
  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              '読み込みに失敗しました',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            const Text('通信状態や端末の設定を確認して、もう一度お試しください。', textAlign: TextAlign.center),
            const SizedBox(height: 20),
            FilledButton(onPressed: onRetry, child: const Text('もう一度試す')),
          ],
        ),
      ),
    ),
  );
}
