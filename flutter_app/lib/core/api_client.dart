import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  // Web: localhost, Android 에뮬레이터: 10.0.2.2
  static const String baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://localhost:8080',
  );
  static const _storage = FlutterSecureStorage();

  static Future<String?> get accessToken => _storage.read(key: 'accessToken');
  static Future<String?> get refreshToken =>
      _storage.read(key: 'refreshToken');

  static Future<void> saveTokens(String access, String refresh) async {
    await _storage.write(key: 'accessToken', value: access);
    await _storage.write(key: 'refreshToken', value: refresh);
  }

  static Future<void> clearTokens() async {
    await _storage.deleteAll();
  }

  static Future<bool> isLoggedIn() async {
    final token = await accessToken;
    return token != null && token.isNotEmpty;
  }

  static Future<Map<String, String>> _authHeaders() async {
    final token = await accessToken;
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  static Future<http.Response> get(String path) async {
    final headers = await _authHeaders();
    final response =
        await http.get(Uri.parse('$baseUrl$path'), headers: headers);
    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        final newHeaders = await _authHeaders();
        return http.get(Uri.parse('$baseUrl$path'), headers: newHeaders);
      }
    }
    return response;
  }

  static Future<http.Response> post(String path,
      {Map<String, dynamic>? body}) async {
    final headers = await _authHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        final newHeaders = await _authHeaders();
        return http.post(
          Uri.parse('$baseUrl$path'),
          headers: newHeaders,
          body: body != null ? jsonEncode(body) : null,
        );
      }
    }
    return response;
  }

  static Future<http.Response> patch(String path,
      {Map<String, dynamic>? body}) async {
    final headers = await _authHeaders();
    final response = await http.patch(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        final newHeaders = await _authHeaders();
        return http.patch(
          Uri.parse('$baseUrl$path'),
          headers: newHeaders,
          body: body != null ? jsonEncode(body) : null,
        );
      }
    }
    return response;
  }

  static Future<http.Response> put(String path,
      {Map<String, dynamic>? body}) async {
    final headers = await _authHeaders();
    final response = await http.put(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        final newHeaders = await _authHeaders();
        return http.put(
          Uri.parse('$baseUrl$path'),
          headers: newHeaders,
          body: body != null ? jsonEncode(body) : null,
        );
      }
    }
    return response;
  }

  static Future<bool> _tryRefreshToken() async {
    final refresh = await refreshToken;
    if (refresh == null) return false;

    final response = await http.post(
      Uri.parse('$baseUrl/auth/refresh'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refresh}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      await saveTokens(data['accessToken'], data['refreshToken']);
      return true;
    }
    await clearTokens();
    return false;
  }
}
