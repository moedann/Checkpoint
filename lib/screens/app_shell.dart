import 'package:flutter/material.dart';
import '../bridge.dart';
import 'analytics_screen.dart';
import 'home_screen.dart';
import 'review_questions_screen.dart';
import 'settings_screen.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.bridge, required this.snapshot, required this.onRefresh});
  final NanishiniBridge bridge; final AppSnapshot snapshot; final Future<void> Function() onRefresh;
  @override State<AppShell> createState()=>_AppShellState();
}
class _AppShellState extends State<AppShell>{int index=0;@override Widget build(BuildContext context){final screens=[HomeScreen(bridge:widget.bridge,snapshot:widget.snapshot,onRefresh:widget.onRefresh,onOpenSettings:()=>setState(()=>index=3),onOpenAnalytics:()=>setState(()=>index=1)),AnalyticsScreen(bridge:widget.bridge),ReviewQuestionsScreen(bridge:widget.bridge),SettingsScreen(bridge:widget.bridge,snapshot:widget.snapshot,onRefresh:widget.onRefresh)];return Scaffold(body:IndexedStack(index:index,children:screens),bottomNavigationBar:NavigationBar(selectedIndex:index,onDestinationSelected:(v)=>setState(()=>index=v),destinations:const[NavigationDestination(icon:Icon(Icons.home_outlined),selectedIcon:Icon(Icons.home_rounded),label:'ホーム'),NavigationDestination(icon:Icon(Icons.bar_chart_outlined),selectedIcon:Icon(Icons.bar_chart_rounded),label:'分析'),NavigationDestination(icon:Icon(Icons.menu_book_outlined),selectedIcon:Icon(Icons.menu_book_rounded),label:'問題'),NavigationDestination(icon:Icon(Icons.settings_outlined),selectedIcon:Icon(Icons.settings_rounded),label:'設定')]));}}
