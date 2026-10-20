//
//  Generated code. Do not modify.
//  source: demo/v1/demo.proto
//
// @dart = 2.12

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names
// ignore_for_file: deprecated_member_use_from_same_package, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_final_fields
// ignore_for_file: unnecessary_import, unnecessary_this, unused_import

import 'dart:async' as $async;
import 'dart:core' as $core;

import 'package:protobuf/protobuf.dart' as $pb;

import 'demo.pb.dart' as $0;
import 'demo.pbjson.dart';

export 'demo.pb.dart';

abstract class DemoServiceBase extends $pb.GeneratedService {
  $async.Future<$0.EchoResponse> echo($pb.ServerContext ctx, $0.EchoRequest request);
  $async.Future<$0.WhoAmIResponse> whoAmI($pb.ServerContext ctx, $0.WhoAmIRequest request);

  $pb.GeneratedMessage createRequest($core.String methodName) {
    switch (methodName) {
      case 'Echo': return $0.EchoRequest();
      case 'WhoAmI': return $0.WhoAmIRequest();
      default: throw $core.ArgumentError('Unknown method: $methodName');
    }
  }

  $async.Future<$pb.GeneratedMessage> handleCall($pb.ServerContext ctx, $core.String methodName, $pb.GeneratedMessage request) {
    switch (methodName) {
      case 'Echo': return this.echo(ctx, request as $0.EchoRequest);
      case 'WhoAmI': return this.whoAmI(ctx, request as $0.WhoAmIRequest);
      default: throw $core.ArgumentError('Unknown method: $methodName');
    }
  }

  $core.Map<$core.String, $core.dynamic> get $json => DemoServiceBase$json;
  $core.Map<$core.String, $core.Map<$core.String, $core.dynamic>> get $messageJson => DemoServiceBase$messageJson;
}

