import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/api_client.dart';
import '../core/auth_provider.dart';
import '../core/category_map.dart';
import '../widgets/primary_button.dart';
import '../widgets/secondary_button.dart';
import '../widgets/app_chip.dart';


class OnboardingPage extends StatefulWidget {
  const OnboardingPage({super.key});

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  final List<String> _selectedEnums = []; // 백엔드 enum 값으로 관리
  double _priceRange = 20000;
  bool _saving = false;

  void _toggleCategory(String enumValue) {
    setState(() {
      if (_selectedEnums.contains(enumValue)) {
        _selectedEnums.remove(enumValue);
      } else {
        _selectedEnums.add(enumValue);
      }
    });
  }

  Future<void> _handleComplete() async {
    setState(() => _saving = true);

    final response = await ApiClient.put('/users/me/preferences', body: {
      'preferredCategories': _selectedEnums,
      'maxPrice': _priceRange.toInt(),
      'minPrice': 0,
      'preferSolo': false,
    });

    setState(() => _saving = false);

    if (!mounted) return;
    if (response.statusCode == 200) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('취향 설정 완료!')),
      );
    }
    await context.read<AuthProvider>().checkAuth();
    if (!mounted) return;
    context.go('/');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 32),
              const Text(
                '취향을 알려주세요',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                '더 정확한 추천을 위해\n좋아하는 음식 카테고리를 선택해주세요',
                style: TextStyle(
                  fontSize: 16,
                  color: AppColors.mutedForeground,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 32),

              // 카테고리 선택
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: categoryChips
                    .map((c) => AppChip(
                          label: c['label']!,
                          selected: _selectedEnums.contains(c['value']),
                          onTap: () => _toggleCategory(c['value']!),
                        ))
                    .toList(),
              ),
              const SizedBox(height: 32),

              // 가격 범위
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('가격 범위', style: TextStyle(fontSize: 16)),
                  Text(
                    '~${_priceRange.toInt().toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (m) => '${m[1]},')}원',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Slider(
                value: _priceRange,
                min: 5000,
                max: 50000,
                divisions: 45,
                activeColor: AppColors.primary,
                onChanged: (v) => setState(() => _priceRange = v),
              ),

              const Spacer(),

              PrimaryButton(
                text: '완료',
                fullWidth: true,
                loading: _saving,
                onPressed: _handleComplete,
              ),
              const SizedBox(height: 12),
              SecondaryButton(
                text: '건너뛰기',
                fullWidth: true,
                onPressed: () async {
                  await context.read<AuthProvider>().checkAuth();
                  if (!mounted) return;
                  context.go('/');
                },
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
