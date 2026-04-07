import 'dart:convert';
import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../core/api_client.dart';
import '../widgets/primary_button.dart';
import '../widgets/secondary_button.dart';
import '../widgets/app_card.dart';
import '../widgets/app_chip.dart';
import '../widgets/bottom_nav.dart';
import '../widgets/bottom_sheet.dart';
import '../widgets/skeleton_ui.dart';

enum RecommendState { initial, loading, result, empty }

const _rejectReasons = ['너무 멀어요', '배고프지 않아요', '혼밥 선호', '기타'];

class RecommendPage extends StatefulWidget {
  const RecommendPage({super.key});

  @override
  State<RecommendPage> createState() => _RecommendPageState();
}

class _RecommendPageState extends State<RecommendPage> {
  RecommendState _state = RecommendState.initial;
  Map<String, dynamic>? _currentResult;

  Future<void> _getRecommendation() async {
    setState(() => _state = RecommendState.loading);

    try {
      final response = await ApiClient.post('/recommendations', body: {
        'latitude': 37.4979,
        'longitude': 127.0276,
        // TODO: GPS 위치 연동
      });

      if (!mounted) return;

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          _currentResult = data;
          _state = RecommendState.result;
        });
      } else if (response.statusCode == 404) {
        setState(() => _state = RecommendState.empty);
      } else {
        final error = jsonDecode(response.body);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(error['message'] ?? '추천에 실패했습니다.'),
            backgroundColor: AppColors.destructive,
          ),
        );
        setState(() => _state = RecommendState.initial);
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('서버에 연결할 수 없습니다.'),
          backgroundColor: AppColors.destructive,
        ),
      );
      setState(() => _state = RecommendState.initial);
    }
  }

  Future<void> _handleAccept() async {
    if (_currentResult == null) return;
    final id = _currentResult!['recommendationId'];

    try {
      await ApiClient.patch('/recommendations/$id/accept');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('좋은 식사 되세요!')),
      );
      setState(() {
        _state = RecommendState.initial;
        _currentResult = null;
      });
    } catch (_) {}
  }

  Future<void> _handleReject(String reason) async {
    if (_currentResult == null) return;
    final id = _currentResult!['recommendationId'];

    try {
      await ApiClient.patch('/recommendations/$id/reject', body: {
        'reason': reason,
      });
    } catch (_) {}

    if (!mounted) return;
    Navigator.of(context).pop(); // close bottom sheet
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('다른 메뉴를 추천해드릴게요!')),
    );
    _getRecommendation();
  }

  void _showRejectSheet() {
    showAppBottomSheet(
      context: context,
      title: '이유를 알려주시겠어요?',
      child: Column(
        children: _rejectReasons
            .map((reason) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: SizedBox(
                    width: double.infinity,
                    child: TextButton(
                      onPressed: () => _handleReject(reason),
                      style: TextButton.styleFrom(
                        backgroundColor: AppColors.accent,
                        padding: const EdgeInsets.all(16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        alignment: Alignment.centerLeft,
                      ),
                      child: Text(
                        reason,
                        style: const TextStyle(
                          color: AppColors.foreground,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ))
            .toList(),
      ),
    );
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
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.location_on,
                          size: 16, color: AppColors.mutedForeground),
                      const SizedBox(width: 4),
                      Text(
                        '강남역 근처',
                        style: TextStyle(
                          fontSize: 14,
                          color: AppColors.mutedForeground,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    '오늘 뭐 먹을까?',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),

            // 메인 콘텐츠
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: _buildContent(),
              ),
            ),

            // 하단 네비게이션
            const BottomNav(),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    switch (_state) {
      case RecommendState.initial:
        return _buildInitial();
      case RecommendState.loading:
        return _buildLoading();
      case RecommendState.result:
        return _buildResult();
      case RecommendState.empty:
        return _buildEmpty();
    }
  }

  Widget _buildInitial() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 96,
          height: 96,
          decoration: BoxDecoration(
            color: AppColors.accent,
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.thumb_up, size: 48, color: AppColors.primary),
        ),
        const SizedBox(height: 24),
        const Text(
          '메뉴 추천 받기',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        const Text(
          'AI가 당신의 취향에 맞는\n메뉴를 추천해드립니다',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.mutedForeground, height: 1.5),
        ),
        const SizedBox(height: 32),
        PrimaryButton(
          text: '추천 받기',
          onPressed: _getRecommendation,
        ),
      ],
    );
  }

  Widget _buildLoading() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const SkeletonCard(),
        const SizedBox(height: 16),
        Text(
          '맛있는 메뉴를 찾고 있어요...',
          style: TextStyle(color: AppColors.mutedForeground),
        ),
      ],
    );
  }

  Widget _buildResult() {
    if (_currentResult == null) return const SizedBox();
    final hasMenu = _currentResult!['menuName'] != null;

    return SingleChildScrollView(
      child: Column(
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (hasMenu) ...[
                  Text(
                    _currentResult!['menuName'],
                    style: const TextStyle(
                        fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _currentResult!['restaurantName'] ?? '',
                    style: const TextStyle(fontSize: 18),
                  ),
                ] else ...[
                  Text(
                    _currentResult!['restaurantName'] ?? '식당 추천',
                    style: const TextStyle(
                        fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    '메뉴 정보 준비 중',
                    style: TextStyle(
                      fontSize: 14,
                      color: AppColors.mutedForeground,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                if (_currentResult!['address'] != null)
                  Text(
                    _currentResult!['address'],
                    style: const TextStyle(
                      fontSize: 14,
                      color: AppColors.mutedForeground,
                    ),
                  ),
                if (_currentResult!['distance'] != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    '${_currentResult!['distance']}m 거리',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: AppColors.primary,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                if (_currentResult!['category'] != null)
                  AppChip(
                    label: _currentResult!['category'],
                    selected: true,
                  ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PrimaryButton(
            text: '여기 갈래요!',
            fullWidth: true,
            onPressed: _handleAccept,
          ),
          const SizedBox(height: 12),
          SecondaryButton(
            text: '다른 거 추천해줘',
            fullWidth: true,
            onPressed: _showRejectSheet,
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 96,
          height: 96,
          decoration: BoxDecoration(
            color: AppColors.accent,
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.refresh,
              size: 48, color: AppColors.mutedForeground),
        ),
        const SizedBox(height: 24),
        const Text(
          '추천할 메뉴가 없어요',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        const Text(
          '지금은 추천이 어려운 상황이에요.\n잠시 후 다시 시도해주세요.',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.mutedForeground, height: 1.5),
        ),
        const SizedBox(height: 32),
        PrimaryButton(
          text: '처음부터 다시 시작',
          onPressed: () {
            setState(() => _state = RecommendState.initial);
          },
        ),
      ],
    );
  }
}
