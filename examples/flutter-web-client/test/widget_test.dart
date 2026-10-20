import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_web_client/main.dart';

void main() {
  testWidgets('demo home loads', (WidgetTester tester) async {
    await tester.pumpWidget(const DemoApp());
    expect(find.text('Connect → quarkus-connect'), findsOneWidget);
    expect(find.text('Echo'), findsOneWidget);
  });
}
