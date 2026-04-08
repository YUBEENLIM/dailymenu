import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../core/api_client.dart';
import '../core/auth_provider.dart';
import '../core/theme.dart';

class KakaoLoginPage extends StatefulWidget {
  const KakaoLoginPage({super.key});

  @override
  State<KakaoLoginPage> createState() => _KakaoLoginPageState();
}

class _KakaoLoginPageState extends State<KakaoLoginPage> {
  late final WebViewController _controller;
  bool _loading = true;

  // 카카오 OAuth URL (백엔드와 동일한 client_id, redirect_uri 사용)
  static const _kakaoAuthUrl =
      'https://kauth.kakao.com/oauth/authorize'
      '?client_id=427c342e3b8aa668eb44c22afae86abb'
      '&redirect_uri=http://localhost:8080/auth/kakao/callback'
      '&response_type=code';

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(NavigationDelegate(
        onNavigationRequest: (request) => _handleNavigation(request),
        onPageFinished: (_) {
          if (mounted) setState(() => _loading = false);
        },
      ))
      ..loadRequest(Uri.parse(_kakaoAuthUrl));
  }

  NavigationDecision _handleNavigation(NavigationRequest request) {
    final uri = Uri.parse(request.url);

    // redirect URI 감지: localhost 콜백에 code 파라미터가 있으면 가로채기
    if (uri.host == 'localhost' && uri.path == '/auth/kakao/callback') {
      final code = uri.queryParameters['code'];
      if (code != null) {
        _exchangeCodeForToken(code);
        return NavigationDecision.prevent;
      }
    }
    return NavigationDecision.navigate;
  }

  Future<void> _exchangeCodeForToken(String code) async {
    setState(() => _loading = true);

    try {
      // POST /auth/kakao로 인가 코드 전달하여 JWT 수신
      final response = await ApiClient.post('/auth/kakao', body: {'code': code});

      if (!mounted) return;

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        final data = body['data'] ?? body;
        await ApiClient.saveTokens(data['accessToken'], data['refreshToken']);

        if (!mounted) return;
        context.go('/onboarding');
      } else {
        final error = jsonDecode(response.body);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(error['message'] ?? '카카오 로그인에 실패했습니다.'),
            backgroundColor: AppColors.destructive,
          ),
        );
        context.go('/login');
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('카카오 로그인 처리 중 오류가 발생했습니다.'),
          backgroundColor: AppColors.destructive,
        ),
      );
      context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.white,
        title: const Text('카카오 로그인'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.go('/login'),
        ),
      ),
      body: Stack(
        children: [
          WebViewWidget(controller: _controller),
          if (_loading)
            const Center(child: CircularProgressIndicator(color: AppColors.primary)),
        ],
      ),
    );
  }
}
