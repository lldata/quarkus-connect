//
//  Generated code. Do not modify.
//  source: demo/v1/demo.proto
//

import "package:connectrpc/connect.dart" as connect;
import "demo.pb.dart" as demov1demo;
import "demo.connect.spec.dart" as specs;

/// Shared with examples/flutter-web-client (same .proto → Dart + Kotlin stubs).
extension type DemoServiceClient (connect.Transport _transport) {
  Future<demov1demo.EchoResponse> echo(
    demov1demo.EchoRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.DemoService.echo,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  Future<demov1demo.WhoAmIResponse> whoAmI(
    demov1demo.WhoAmIRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.DemoService.whoAmI,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }
}
