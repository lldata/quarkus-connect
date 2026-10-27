import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_web_client/idle_timeout.dart';

void main() {
  test('first call and post-idle use cold-start timeout', () {
    var now = DateTime.utc(2026, 1, 1, 12);
    final idle = IdleAwareTimeout(
      idleThreshold: const Duration(seconds: 30),
      coldStartTimeout: const Duration(seconds: 60),
      warmTimeout: const Duration(seconds: 10),
      clock: () => now,
    );
    expect(idle.isColdStart, isTrue);
    expect(idle.nextTimeout, const Duration(seconds: 60));
    idle.signal();

    now = now.add(const Duration(seconds: 5));
    expect(idle.isColdStart, isFalse);
    expect(idle.nextTimeout, const Duration(seconds: 10));

    now = now.add(const Duration(seconds: 30));
    expect(idle.isColdStart, isTrue);
    expect(idle.nextTimeout, const Duration(seconds: 60));
  });
}
