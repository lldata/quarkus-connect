import 'package:connectrpc/connect.dart' show Headers;

/// Holds the current Bearer token and builds per-call [Headers].
///
/// Default headers include `Authorization: Bearer <token>` when a token is set.
/// Pass [overrideHeaders] to add or replace values for a single call (e.g. a
/// different Authorization for one WhoAmI).
class TokenProvider {
  TokenProvider([this.token = 'demo-user']);

  String token;

  /// Headers for one RPC. [overrideHeaders] win on name collision.
  Headers headers({Headers? overrideHeaders}) {
    final h = Headers();
    if (token.isNotEmpty) {
      h.add('authorization', 'Bearer $token');
    }
    if (overrideHeaders != null) {
      for (final e in overrideHeaders.entries) {
        h.remove(e.name);
        h.add(e.name, e.value);
      }
    }
    return h;
  }
}
