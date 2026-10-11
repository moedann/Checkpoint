import 'package:flutter/material.dart';

// Kept as compatibility aliases because existing widgets use these names.
// The visual brand is blue, matching the launcher icon.
const checkpointGreen = Color(0xFF0B6CFF);
const checkpointInk = Color(0xFF111827);
const checkpointMuted = Color(0xFF6B7280);
const checkpointBorder = Color(0xFFE5E7EB);
const checkpointSoftGreen = Color(0xFFEAF2FF);
const checkpointOrange = Color(0xFFB66A12);

ThemeData buildAppTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: checkpointGreen,
    brightness: Brightness.light,
    surface: Colors.white,
  ).copyWith(
    primary: checkpointGreen,
    onSurface: checkpointInk,
    outline: checkpointBorder,
    error: const Color(0xFFB42318),
  );
  final radius = BorderRadius.circular(14);
  return ThemeData(
    useMaterial3: true,
    fontFamily: 'ZenKakuGothicNew',
    colorScheme: scheme,
    scaffoldBackgroundColor: Colors.white,
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.white,
      foregroundColor: checkpointInk,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: checkpointInk),
    ),
    textTheme: const TextTheme(
      headlineMedium: TextStyle(fontSize: 28, height: 1.25, fontWeight: FontWeight.w700, color: checkpointInk),
      titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: checkpointInk),
      titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: checkpointInk),
      bodyLarge: TextStyle(fontSize: 16, height: 1.5, color: checkpointInk),
      bodyMedium: TextStyle(fontSize: 14, height: 1.5, color: checkpointInk),
      bodySmall: TextStyle(fontSize: 13, height: 1.4, color: checkpointMuted),
    ),
    navigationBarTheme: NavigationBarThemeData(
      height: 70,
      backgroundColor: Colors.white,
      indicatorColor: checkpointSoftGreen,
      elevation: 0,
      labelTextStyle: WidgetStateProperty.resolveWith((s) => TextStyle(
        fontSize: 12,
        fontWeight: s.contains(WidgetState.selected) ? FontWeight.w700 : FontWeight.w500,
        color: s.contains(WidgetState.selected) ? checkpointGreen : checkpointMuted,
      )),
      iconTheme: WidgetStateProperty.resolveWith((s) => IconThemeData(
        color: s.contains(WidgetState.selected) ? checkpointGreen : checkpointMuted,
      )),
    ),
    filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(
      minimumSize: const Size(48, 52), shape: RoundedRectangleBorder(borderRadius: radius),
      textStyle: const TextStyle(fontWeight: FontWeight.w700),
    )),
    outlinedButtonTheme: OutlinedButtonThemeData(style: OutlinedButton.styleFrom(
      minimumSize: const Size(48, 52), shape: RoundedRectangleBorder(borderRadius: radius),
      side: const BorderSide(color: checkpointBorder),
    )),
    inputDecorationTheme: InputDecorationTheme(
      filled: true, fillColor: const Color(0xFFF8FAFC),
      border: OutlineInputBorder(borderRadius: radius, borderSide: const BorderSide(color: checkpointBorder)),
      enabledBorder: OutlineInputBorder(borderRadius: radius, borderSide: const BorderSide(color: checkpointBorder)),
    ),
    dialogTheme: DialogThemeData(backgroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20))),
    bottomSheetTheme: const BottomSheetThemeData(backgroundColor: Colors.white, surfaceTintColor: Colors.transparent, showDragHandle: true),
    dividerTheme: const DividerThemeData(color: checkpointBorder, thickness: 1, space: 1),
    cardTheme: CardThemeData(color: Colors.white, elevation: 0, surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: const BorderSide(color: checkpointBorder))),
  );
}
