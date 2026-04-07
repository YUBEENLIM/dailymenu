import 'package:flutter/material.dart';

class AppColors {
  static const primary = Color(0xFFFF6B00);
  static const primaryLight = Color(0xFFFFF5EB);
  static const background = Color(0xFFF5F5F5);
  static const white = Color(0xFFFFFFFF);
  static const border = Color(0xFFE5E5E5);
  static const mutedForeground = Color(0xFF737373);
  static const foreground = Color(0xFF171717);
  static const accent = Color(0xFFFAFAFA);
  static const destructive = Color(0xFFEF4444);
  static const inputBackground = Color(0xFFF5F5F5);
  static const switchBackground = Color(0xFFD4D4D4);
  static const kakaoYellow = Color(0xFFFEE500);
}

class AppTheme {
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        fontFamily: 'Pretendard',
        scaffoldBackgroundColor: AppColors.background,
        colorScheme: const ColorScheme.light(
          primary: AppColors.primary,
          surface: AppColors.white,
          error: AppColors.destructive,
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: AppColors.white,
          foregroundColor: AppColors.foreground,
          elevation: 0,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
            textStyle: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.inputBackground,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.border),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.primary, width: 2),
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        ),
      );
}
