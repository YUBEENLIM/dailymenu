import 'dart:convert';
import 'package:flutter/material.dart';
import 'api_client.dart';

class AuthProvider extends ChangeNotifier {
  bool _isLoggedIn = false;
  bool _isLoading = true;

  bool get isLoggedIn => _isLoggedIn;
  bool get isLoading => _isLoading;

  Future<void> checkAuth() async {
    _isLoading = true;
    notifyListeners();
    _isLoggedIn = await ApiClient.isLoggedIn();
    _isLoading = false;
    notifyListeners();
  }

  Future<String?> login(String email, String password) async {
    final response = await ApiClient.post('/auth/login', body: {
      'email': email,
      'password': password,
    });

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final data = body['data'] ?? body;
      await ApiClient.saveTokens(data['accessToken'], data['refreshToken']);
      _isLoggedIn = true;
      notifyListeners();
      return null;
    }
    final error = jsonDecode(response.body);
    return error['message'] ?? '로그인에 실패했습니다.';
  }

  Future<String?> register(String email, String password, String nickname) async {
    final response = await ApiClient.post('/auth/register', body: {
      'email': email,
      'password': password,
      'nickname': nickname,
    });

    if (response.statusCode == 200 || response.statusCode == 201) {
      // 토큰 확보 (온보딩 API 호출에 필요), 라우터 redirect 방지를 위해 notifyListeners 생략
      final loginResponse = await ApiClient.post('/auth/login', body: {
        'email': email,
        'password': password,
      });
      if (loginResponse.statusCode == 200) {
        final body = jsonDecode(loginResponse.body);
        final data = body['data'] ?? body;
        await ApiClient.saveTokens(data['accessToken'], data['refreshToken']);
      }
      return null;
    }
    final error = jsonDecode(response.body);
    return error['message'] ?? '회원가입에 실패했습니다.';
  }

  Future<void> logout() async {
    try {
      await ApiClient.post('/auth/logout');
    } catch (_) {}
    await ApiClient.clearTokens();
    _isLoggedIn = false;
    notifyListeners();
  }
}
