import 'package:flutter_test/flutter_test.dart';
import 'package:dailymenu_app/main.dart';

void main() {
  testWidgets('App launches', (WidgetTester tester) async {
    await tester.pumpWidget(const DailymenuApp());
    await tester.pump();
    expect(find.text('맛있는 하루'), findsAny);
  });
}
