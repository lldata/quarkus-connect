import 'package:connectrpc/connect.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_web_client/token_provider.dart';

void main() {
  test('adds Bearer from token', () {
    final p = TokenProvider('demo-user');
    expect(p.headers()['authorization'], 'Bearer demo-user');
  });

  test('per-call override replaces Authorization', () {
    final p = TokenProvider('demo-user');
    final override = Headers()..add('authorization', 'Bearer demo-guest');
    final h = p.headers(overrideHeaders: override);
    expect(h['authorization'], 'Bearer demo-guest');
  });
}
