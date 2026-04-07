import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/api_client.dart';
import '../core/auth_provider.dart';
import '../widgets/primary_button.dart';
import '../widgets/app_card.dart';
import '../widgets/app_chip.dart';
import '../widgets/bottom_nav.dart';
import '../widgets/skeleton_ui.dart';

const _categories = [
  '한식', '중식', '일식', '양식', '분식',
  '치킨', '피자', '버거', '아시안', '디저트',
];

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  bool _loading = true;
  bool _editing = false;
  Map<String, dynamic>? _userData;
  List<Map<String, dynamic>> _mealRecords = [];

  final _nicknameController = TextEditingController();
  List<String> _selectedCategories = [];
  List<String> _dislikedCategories = [];
  double _priceRange = 20000;
  bool _soloPreference = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);

    try {
      // 프로필과 식사 기록 병렬 로딩
      final results = await Future.wait([
        ApiClient.get('/users/me'),
        ApiClient.get('/meal-histories'),
      ]);

      if (!mounted) return;

      if (results[0].statusCode == 200) {
        final data = jsonDecode(results[0].body);
        _userData = data;
        _nicknameController.text = data['nickname'] ?? '';
        final prefs = data['preferences'];
        if (prefs != null) {
          _selectedCategories =
              List<String>.from(prefs['preferredCategories'] ?? []);
          _priceRange = (prefs['maxPriceRange'] ?? 20000).toDouble();
          _soloPreference = prefs['soloPreference'] ?? false;
        }
        _dislikedCategories =
            List<String>.from(data['excludedCategories'] ?? []);
      }

      if (results[1].statusCode == 200) {
        final records = jsonDecode(results[1].body);
        _mealRecords = List<Map<String, dynamic>>.from(records);
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('데이터를 불러올 수 없습니다.'),
          backgroundColor: AppColors.destructive,
        ),
      );
    }

    setState(() => _loading = false);
  }

  Future<void> _handleSave() async {
    // 닉네임 업데이트
    if (_nicknameController.text.isNotEmpty) {
      await ApiClient.patch('/users/me/nickname', body: {
        'nickname': _nicknameController.text.trim(),
      });
    }

    // 취향 업데이트
    await ApiClient.put('/users/me/preferences', body: {
      'preferredCategories': _selectedCategories,
      'maxPriceRange': _priceRange.toInt(),
      'soloPreference': _soloPreference,
    });

    // 싫어하는 카테고리 업데이트
    await ApiClient.put('/users/me/restrictions', body: {
      'excludedCategories': _dislikedCategories,
    });

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('저장되었습니다!')),
    );
    setState(() => _editing = false);
    _loadData();
  }

  Future<void> _handleLogout() async {
    await context.read<AuthProvider>().logout();
    if (!mounted) return;
    context.go('/login');
  }

  void _toggleCategory(String category) {
    if (!_editing) return;
    setState(() {
      if (_selectedCategories.contains(category)) {
        _selectedCategories.remove(category);
      } else {
        _selectedCategories.add(category);
      }
    });
  }

  void _toggleDisliked(String category) {
    if (!_editing) return;
    setState(() {
      if (_dislikedCategories.contains(category)) {
        _dislikedCategories.remove(category);
      } else {
        _dislikedCategories.add(category);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: Column(
          children: [
            // 헤더
            Container(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(color: AppColors.border)),
              ),
              child: const Row(
                children: [
                  Text(
                    '마이페이지',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                ],
              ),
            ),

            // 메인 콘텐츠
            Expanded(
              child: _loading
                  ? const Center(child: SkeletonCard())
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _buildProfile(),
                          const SizedBox(height: 24),
                          _buildPreferences(),
                          const SizedBox(height: 24),
                          _buildMealHistory(),
                          const SizedBox(height: 24),
                          _buildLogout(),
                        ],
                      ),
                    ),
            ),

            const BottomNav(),
          ],
        ),
      ),
    );
  }

  Widget _buildProfile() {
    return AppCard(
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.person,
                    size: 32, color: AppColors.primary),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _editing
                    ? TextField(
                        controller: _nicknameController,
                        decoration: const InputDecoration(
                          hintText: '닉네임',
                          isDense: true,
                        ),
                      )
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _userData?['nickname'] ?? '',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _userData?['email'] ?? '',
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppColors.mutedForeground,
                            ),
                          ),
                        ],
                      ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_editing)
            PrimaryButton(
              text: '저장',
              fullWidth: true,
              onPressed: _handleSave,
            )
          else
            GestureDetector(
              onTap: () => setState(() => _editing = true),
              child: const Row(
                children: [
                  Icon(Icons.settings, size: 18, color: AppColors.primary),
                  SizedBox(width: 8),
                  Text(
                    '프로필 수정',
                    style: TextStyle(color: AppColors.primary),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildPreferences() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Row(
          children: [
            Icon(Icons.settings, size: 20),
            SizedBox(width: 8),
            Text(
              '취향 설정',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
          ],
        ),
        const SizedBox(height: 16),

        if (_selectedCategories.isEmpty && !_editing)
          AppCard(
            backgroundColor: AppColors.primaryLight,
            border: Border.all(color: AppColors.primary.withValues(alpha: 0.2)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '취향을 설정하면 더 정확한 추천을 받을 수 있어요!',
                  style: TextStyle(
                    fontSize: 14,
                    color: AppColors.mutedForeground,
                  ),
                ),
                const SizedBox(height: 12),
                GestureDetector(
                  onTap: () => setState(() => _editing = true),
                  child: const Text(
                    '지금 설정하기 →',
                    style: TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          ),

        // 혼밥 토글
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('혼밥 선호'),
              GestureDetector(
                onTap: _editing
                    ? () => setState(() => _soloPreference = !_soloPreference)
                    : null,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: 48,
                  height: 24,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: _soloPreference
                        ? AppColors.primary
                        : AppColors.switchBackground,
                  ),
                  child: AnimatedAlign(
                    duration: const Duration(milliseconds: 200),
                    alignment: _soloPreference
                        ? Alignment.centerRight
                        : Alignment.centerLeft,
                    child: Container(
                      width: 20,
                      height: 20,
                      margin: const EdgeInsets.symmetric(horizontal: 2),
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.white,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 가격 범위
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('가격 범위'),
                  Text(
                    '~${_priceRange.toInt().toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (m) => '${m[1]},')}원',
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              if (_editing) ...[
                const SizedBox(height: 8),
                Slider(
                  value: _priceRange,
                  min: 5000,
                  max: 50000,
                  divisions: 9,
                  activeColor: AppColors.primary,
                  onChanged: (v) => setState(() => _priceRange = v),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 선호 카테고리
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('선호 카테고리',
                  style: TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _categories
                    .map((c) => AppChip(
                          label: c,
                          selected: _selectedCategories.contains(c),
                          onTap: _editing ? () => _toggleCategory(c) : null,
                        ))
                    .toList(),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 싫어하는 카테고리
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('싫어하는 카테고리',
                  style: TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _categories
                    .map((c) => AppChip(
                          label: c,
                          variant: ChipVariant.dislike,
                          selected: _dislikedCategories.contains(c),
                          onTap: _editing ? () => _toggleDisliked(c) : null,
                        ))
                    .toList(),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMealHistory() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Row(
              children: [
                Icon(Icons.calendar_today, size: 20),
                SizedBox(width: 8),
                Text(
                  '식사 기록',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
              ],
            ),
            IconButton(
              icon: const Icon(Icons.add, size: 20),
              onPressed: () {
                // TODO: 식사 기록 추가
              },
              style: IconButton.styleFrom(
                backgroundColor: AppColors.accent,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (_mealRecords.isEmpty)
          AppCard(
            backgroundColor: AppColors.accent.withValues(alpha: 0.5),
            child: Column(
              children: [
                const Text(
                  '아직 기록이 없어요.\n추천받고 식사를 기록해보세요!',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.mutedForeground),
                ),
                const SizedBox(height: 16),
                GestureDetector(
                  onTap: () => context.go('/'),
                  child: const Text(
                    '추천 받으러 가기 →',
                    style: TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          )
        else
          ..._mealRecords.map((record) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        record['menuName'] ??
                            record['restaurantName'] ??
                            '',
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        record['restaurantName'] ?? '',
                        style: const TextStyle(
                          fontSize: 14,
                          color: AppColors.mutedForeground,
                        ),
                      ),
                      if (record['category'] != null) ...[
                        const SizedBox(height: 8),
                        AppChip(
                          label: record['category'],
                          selected: true,
                        ),
                      ],
                    ],
                  ),
                ),
              )),
      ],
    );
  }

  Widget _buildLogout() {
    return SizedBox(
      width: double.infinity,
      child: TextButton.icon(
        onPressed: _handleLogout,
        icon: const Icon(Icons.logout, color: AppColors.destructive),
        label: const Text(
          '로그아웃',
          style: TextStyle(color: AppColors.destructive),
        ),
        style: TextButton.styleFrom(
          padding: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}
