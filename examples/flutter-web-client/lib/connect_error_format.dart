import 'package:connectrpc/connect.dart';

/// Formats a [ConnectException] including trailers/headers from [ConnectException.metadata].
///
/// Over Connect unary, server trailers arrive as `trailer-<name>` HTTP headers;
/// connectrpc strips the prefix so clients read e.g. `metadata['retry-after-seconds']`.
String formatConnectError(Object error, {String label = 'error'}) {
  if (error is! ConnectException) {
    return '$label\n$error';
  }
  final buf = StringBuffer('$label\ncode=${error.code.name}\nmessage=${error.message}');
  final meta = _formatHeaders(error.metadata);
  if (meta.isNotEmpty) {
    buf.writeln('\nmetadata (headers + trailers):');
    buf.write(meta);
  }
  return buf.toString();
}

String _formatHeaders(Headers headers) {
  final lines = <String>[];
  for (final e in headers.entries) {
    lines.add('  ${e.name}=${e.value}');
  }
  lines.sort();
  return lines.join('\n');
}
