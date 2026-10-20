//
//  Generated code. Do not modify.
//  source: demo/v1/demo.proto
//

import "package:connectrpc/connect.dart" as connect;
import "demo.pb.dart" as demov1demo;

/// Shared with examples/flutter-web-client (same .proto → Dart + Kotlin stubs).
abstract final class DemoService {
  /// Fully-qualified name of the DemoService service.
  static const name = 'demo.v1.DemoService';

  static const echo = connect.Spec(
    '/$name/Echo',
    connect.StreamType.unary,
    demov1demo.EchoRequest.new,
    demov1demo.EchoResponse.new,
  );

  static const whoAmI = connect.Spec(
    '/$name/WhoAmI',
    connect.StreamType.unary,
    demov1demo.WhoAmIRequest.new,
    demov1demo.WhoAmIResponse.new,
  );
}
