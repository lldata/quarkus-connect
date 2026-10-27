import 'package:connectrpc/connect.dart' show AbortSignal, TimeoutSignal;

/// Longer timeout on the first RPC after an idle gap (scaled-to-zero cold start).
class IdleAwareTimeout {
  IdleAwareTimeout({
    this.idleThreshold = const Duration(seconds: 30),
    this.coldStartTimeout = const Duration(seconds: 60),
    this.warmTimeout = const Duration(seconds: 10),
    DateTime Function()? clock,
  }) : _clock = clock ?? DateTime.now;

  final Duration idleThreshold;
  final Duration coldStartTimeout;
  final Duration warmTimeout;
  final DateTime Function() _clock;

  DateTime? _lastCallAt;

  /// Whether the next call should use [coldStartTimeout].
  bool get isColdStart {
    final last = _lastCallAt;
    if (last == null) return true;
    return _clock().difference(last) >= idleThreshold;
  }

  Duration get nextTimeout => isColdStart ? coldStartTimeout : warmTimeout;

  /// [TimeoutSignal] for the next call; marks activity when created.
  AbortSignal signal() {
    final timeout = nextTimeout;
    _lastCallAt = _clock();
    return TimeoutSignal(timeout);
  }

  /// Test helper: clear activity so the next call is treated as cold.
  void reset() => _lastCallAt = null;
}
