import 'package:go_router/go_router.dart';
import 'auth_provider.dart';
import '../pages/login_page.dart';
import '../pages/signup_page.dart';
import '../pages/onboarding_page.dart';
import '../pages/recommend_page.dart';
import '../pages/profile_page.dart';

GoRouter createRouter(AuthProvider authProvider) {
  return GoRouter(
    refreshListenable: authProvider,
    redirect: (context, state) {
      final loggedIn = authProvider.isLoggedIn;
      final isLoading = authProvider.isLoading;
      final location = state.uri.toString();

      if (isLoading) return null;

      final publicRoutes = ['/login', '/signup'];
      final isPublic = publicRoutes.contains(location);

      if (!loggedIn && !isPublic) return '/login';
      if (loggedIn && isPublic) return '/';

      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, _) => const LoginPage()),
      GoRoute(path: '/signup', builder: (_, _) => const SignupPage()),
      GoRoute(path: '/onboarding', builder: (_, _) => const OnboardingPage()),
      GoRoute(path: '/', builder: (_, _) => const RecommendPage()),
      GoRoute(path: '/profile', builder: (_, _) => const ProfilePage()),
    ],
  );
}
