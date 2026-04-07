import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/auth_provider.dart';
import '../widgets/primary_button.dart';

class SignupPage extends StatefulWidget {
  const SignupPage({super.key});

  @override
  State<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends State<SignupPage> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _loading = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _handleSignup() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _loading = true);
    final auth = context.read<AuthProvider>();
    final error = await auth.register(
      _emailController.text.trim(),
      _passwordController.text,
      _nicknameController.text.trim(),
    );
    setState(() => _loading = false);

    if (!mounted) return;
    if (error != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error), backgroundColor: AppColors.destructive),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('회원가입 성공!')),
      );
      context.go('/onboarding');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.go('/login'),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 16),
                const Text(
                  '회원가입',
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  '맛있는 하루를 시작해보세요',
                  style: TextStyle(
                    fontSize: 16,
                    color: AppColors.mutedForeground,
                  ),
                ),
                const SizedBox(height: 32),

                // 이메일
                const Text('이메일', style: TextStyle(fontSize: 14)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    hintText: 'example@email.com',
                    prefixIcon: Icon(Icons.mail_outline,
                        color: AppColors.mutedForeground),
                  ),
                  validator: (v) =>
                      v == null || v.isEmpty ? '이메일을 입력해주세요' : null,
                ),
                const SizedBox(height: 16),

                // 비밀번호
                const Text('비밀번호', style: TextStyle(fontSize: 14)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _passwordController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    hintText: '8자 이상 입력',
                    prefixIcon: Icon(Icons.lock_outline,
                        color: AppColors.mutedForeground),
                  ),
                  validator: (v) {
                    if (v == null || v.isEmpty) return '비밀번호를 입력해주세요';
                    if (v.length < 8) return '8자 이상 입력해주세요';
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // 닉네임
                const Text('닉네임', style: TextStyle(fontSize: 14)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _nicknameController,
                  decoration: const InputDecoration(
                    hintText: '닉네임을 입력하세요',
                    prefixIcon:
                        Icon(Icons.person_outline, color: AppColors.mutedForeground),
                  ),
                  validator: (v) =>
                      v == null || v.isEmpty ? '닉네임을 입력해주세요' : null,
                ),
                const SizedBox(height: 32),

                PrimaryButton(
                  text: '가입하기',
                  fullWidth: true,
                  loading: _loading,
                  onPressed: _handleSignup,
                ),
                const SizedBox(height: 16),

                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text(
                      '이미 계정이 있으신가요? ',
                      style: TextStyle(color: AppColors.mutedForeground),
                    ),
                    GestureDetector(
                      onTap: () => context.go('/login'),
                      child: const Text(
                        '로그인',
                        style: TextStyle(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
