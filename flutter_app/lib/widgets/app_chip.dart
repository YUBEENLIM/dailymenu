import 'package:flutter/material.dart';
import '../core/theme.dart';

enum ChipVariant { normal, dislike }

class AppChip extends StatelessWidget {
  final String label;
  final bool selected;
  final ChipVariant variant;
  final VoidCallback? onTap;

  const AppChip({
    super.key,
    required this.label,
    this.selected = false,
    this.variant = ChipVariant.normal,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Color bgColor;
    Color textColor;

    if (selected) {
      if (variant == ChipVariant.dislike) {
        bgColor = AppColors.destructive;
        textColor = AppColors.white;
      } else {
        bgColor = AppColors.primary;
        textColor = AppColors.white;
      }
    } else {
      bgColor = AppColors.accent;
      textColor = AppColors.mutedForeground;
    }

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: textColor,
          ),
        ),
      ),
    );
  }
}
