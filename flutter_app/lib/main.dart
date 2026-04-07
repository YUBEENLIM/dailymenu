import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme.dart';
import 'core/auth_provider.dart';
import 'core/router.dart';

void main() {
  runApp(const DailymenuApp());
}

class DailymenuApp extends StatelessWidget {
  const DailymenuApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AuthProvider()..checkAuth(),
      child: Consumer<AuthProvider>(
        builder: (context, auth, _) {
          final router = createRouter(auth);
          return MaterialApp.router(
            title: '맛있는 하루',
            theme: AppTheme.light,
            routerConfig: router,
            debugShowCheckedModeBanner: false,
          );
        },
      ),
    );
  }
}
