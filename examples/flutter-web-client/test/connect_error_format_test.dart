import 'package:connectrpc/connect.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_web_client/connect_error_format.dart';

void main() {
  test('includes metadata trailers', () {
    final meta = Headers()..add('retry-after-seconds', '30');
    final ex = ConnectException(
      Code.permissionDenied,
      'guest cannot call WhoAmI',
      metadata: meta,
    );
    final text = formatConnectError(ex, label: 'WhoAmI error');
    expect(text, contains('permission_denied'));
    expect(text, contains('retry-after-seconds=30'));
  });
}
