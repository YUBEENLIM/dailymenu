import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:geocoding/geocoding.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/theme.dart';
import '../core/api_client.dart';
import '../core/category_map.dart';
import '../widgets/primary_button.dart';
import '../widgets/secondary_button.dart';
import '../widgets/app_card.dart';
import '../widgets/app_chip.dart';
import '../widgets/bottom_nav.dart';
import '../widgets/bottom_sheet.dart';
import '../widgets/skeleton_ui.dart';

enum RecommendState { initial, loading, result, empty }

const _rejectReasons = [
  {'label': '너무 멀어요', 'value': 'TOO_FAR'},
  {'label': '최근에 먹었어요', 'value': 'ATE_RECENTLY'},
  {'label': '이 종류 말고요', 'value': 'NOT_THIS_TYPE'},
  {'label': '기타', 'value': 'OTHER'},
];

class RecommendPage extends StatefulWidget {
  const RecommendPage({super.key});

  @override
  State<RecommendPage> createState() => _RecommendPageState();
}

class _RecommendPageState extends State<RecommendPage> {
  RecommendState _state = RecommendState.initial;
  Map<String, dynamic>? _currentResult;
  String _locationText = '위치를 설정해주세요';
  double? _latitude;
  double? _longitude;

  Future<Position?> _getCurrentPosition() async {
    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      final requested = await Geolocator.requestPermission();
      if (requested == LocationPermission.denied ||
          requested == LocationPermission.deniedForever) {
        return null;
      }
    }
    if (permission == LocationPermission.deniedForever) return null;
    return await Geolocator.getCurrentPosition();
  }

  Future<bool> _resolveLocation() async {
    final position = await _getCurrentPosition();
    if (position != null) {
      _latitude = position.latitude;
      _longitude = position.longitude;
      await _updateLocationText(_latitude!, _longitude!);
      return true;
    }
    // GPS 거부 → 주소 입력 다이얼로그
    if (!mounted) return false;
    final address = await _showAddressInputDialog();
    if (address == null || address.isEmpty) return false;
    try {
      final locations = await locationFromAddress(address);
      if (locations.isNotEmpty) {
        _latitude = locations.first.latitude;
        _longitude = locations.first.longitude;
        if (mounted) setState(() => _locationText = address);
        return true;
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('주소를 찾을 수 없습니다. 다시 시도해주세요.')),
        );
      }
    }
    return false;
  }

  Future<void> _updateLocationText(double lat, double lng) async {
    try {
      final placemarks = await placemarkFromCoordinates(lat, lng);
      if (placemarks.isNotEmpty && mounted) {
        final p = placemarks.first;
        final addr = [p.locality, p.subLocality, p.thoroughfare]
            .where((s) => s != null && s.isNotEmpty)
            .join(' ');
        setState(() => _locationText = addr.isNotEmpty ? addr : '내 위치');
      }
    } catch (_) {
      if (mounted) setState(() => _locationText = '내 위치');
    }
  }

  Future<String?> _showAddressInputDialog() async {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('위치 입력'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '예: 서울시 은평구 불광동',
            prefixIcon: Icon(Icons.search),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, controller.text.trim()),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  void _openMap() {
    final restaurant = _currentResult?['restaurant'] as Map<String, dynamic>?;
    final externalId = restaurant?['externalId'];
    if (externalId != null) {
      launchUrl(Uri.parse('https://place.map.kakao.com/$externalId'));
    } else {
      final address = restaurant?['address'] ?? '';
      final name = restaurant?['name'] ?? '';
      final query = Uri.encodeComponent(address.isNotEmpty ? '$name $address' : name);
      launchUrl(Uri.parse('https://map.kakao.com/?q=$query'));
    }
  }

  Future<void> _getRecommendation() async {
    setState(() => _state = RecommendState.loading);

    try {
      if (_latitude == null || _longitude == null) {
        final resolved = await _resolveLocation();
        if (!resolved) {
          if (mounted) setState(() => _state = RecommendState.initial);
          return;
        }
      }

      final idempotencyKey = '${DateTime.now().millisecondsSinceEpoch}-${Random().nextInt(999999)}';
      final response = await ApiClient.post('/recommendations',
        body: {
          'latitude': _latitude,
          'longitude': _longitude,
        },
        extraHeaders: {'Idempotency-Key': idempotencyKey},
      );

      if (!mounted) return;

      if (response.statusCode == 200 || response.statusCode == 201) {
        final body = jsonDecode(response.body);
        final data = body['data'] ?? body;
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

      // 식사 기록 자동 생성
      await ApiClient.post('/meal-histories', body: {
        'recommendationId': id,
        'eatenAt': DateTime.now().toIso8601String(),
      });

      // 지도 열기
      _openMap();

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

  Future<void> _handleReject(String reasonValue, {String? memo}) async {
    if (_currentResult == null) return;
    final id = _currentResult!['recommendationId'];

    try {
      final body = <String, dynamic>{'reason': reasonValue};
      if (memo != null && memo.isNotEmpty) body['memo'] = memo;
      await ApiClient.patch('/recommendations/$id/reject', body: body);
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
                      onPressed: () {
                        if (reason['value'] == 'OTHER') {
                          Navigator.of(context).pop();
                          _showOtherReasonInput();
                        } else {
                          _handleReject(reason['value']!);
                        }
                      },
                      style: TextButton.styleFrom(
                        backgroundColor: AppColors.accent,
                        padding: const EdgeInsets.all(16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        alignment: Alignment.centerLeft,
                      ),
                      child: Text(
                        reason['label']!,
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

  void _showOtherReasonInput() {
    final controller = TextEditingController();
    showAppBottomSheet(
      context: context,
      title: '어떤 이유인지 알려주세요',
      child: Column(
        children: [
          TextField(
            controller: controller,
            maxLength: 200,
            decoration: const InputDecoration(
              hintText: '거절 사유를 입력해주세요',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          PrimaryButton(
            text: '제출',
            fullWidth: true,
            onPressed: () {
              _handleReject('OTHER', memo: controller.text);
            },
          ),
          const SizedBox(height: 8),
          SecondaryButton(
            text: '건너뛰기',
            fullWidth: true,
            onPressed: () {
              _handleReject('OTHER');
            },
          ),
        ],
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
                  GestureDetector(
                    onTap: () async {
                      final address = await _showAddressInputDialog();
                      if (address == null || address.isEmpty) return;
                      try {
                        final locations = await locationFromAddress(address);
                        if (locations.isNotEmpty) {
                          _latitude = locations.first.latitude;
                          _longitude = locations.first.longitude;
                          if (mounted) setState(() => _locationText = address);
                        }
                      } catch (_) {
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('주소를 찾을 수 없습니다.')),
                          );
                        }
                      }
                    },
                    child: Row(
                      children: [
                        Icon(Icons.location_on,
                            size: 16, color: AppColors.primary),
                        const SizedBox(width: 4),
                        Text(
                          _locationText,
                          style: TextStyle(
                            fontSize: 14,
                            color: AppColors.primary,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Icon(Icons.edit, size: 12, color: AppColors.primary),
                      ],
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    '오늘 뭐 먹지?',
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
    final menu = _currentResult!['menu'] as Map<String, dynamic>?;
    final restaurant = _currentResult!['restaurant'] as Map<String, dynamic>?;
    final hasMenu = menu != null;

    return SingleChildScrollView(
      child: Column(
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (hasMenu) ...[
                  Text(
                    menu['name'] ?? '',
                    style: const TextStyle(
                        fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    restaurant?['name'] ?? '',
                    style: const TextStyle(fontSize: 18),
                  ),
                  if (menu['price'] != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      '${menu['price']}원',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ],
                ] else ...[
                  Text(
                    restaurant?['name'] ?? '식당 추천',
                    style: const TextStyle(
                        fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                ],
                const SizedBox(height: 12),
                if (restaurant?['address'] != null)
                  Text(
                    restaurant!['address'],
                    style: const TextStyle(
                      fontSize: 14,
                      color: AppColors.mutedForeground,
                    ),
                  ),
                if (restaurant?['distance'] != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    '${restaurant!['distance'].toInt()}m 거리',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: AppColors.primary,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  children: [
                    if (menu?['category'] != null)
                      AppChip(
                        label: categoryLabel(menu!['category']),
                        selected: true,
                      )
                    else if (!hasMenu && restaurant?['subCategory'] != null)
                      AppChip(
                        label: restaurant!['subCategory'],
                        selected: true,
                      ),
                    if (hasMenu && restaurant?['subCategory'] != null)
                      AppChip(
                        label: restaurant!['subCategory'],
                        selected: false,
                      ),
                  ],
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
