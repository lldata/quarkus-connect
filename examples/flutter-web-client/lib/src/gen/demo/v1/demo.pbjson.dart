//
//  Generated code. Do not modify.
//  source: demo/v1/demo.proto
//
// @dart = 2.12

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_final_fields
// ignore_for_file: unnecessary_import, unnecessary_this, unused_import

import 'dart:convert' as $convert;
import 'dart:core' as $core;
import 'dart:typed_data' as $typed_data;

@$core.Deprecated('Use echoRequestDescriptor instead')
const EchoRequest$json = {
  '1': 'EchoRequest',
  '2': [
    {'1': 'message', '3': 1, '4': 1, '5': 9, '10': 'message'},
    {'1': 'client_seq', '3': 2, '4': 1, '5': 3, '10': 'clientSeq'},
  ],
};

/// Descriptor for `EchoRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List echoRequestDescriptor = $convert.base64Decode(
    'CgtFY2hvUmVxdWVzdBIYCgdtZXNzYWdlGAEgASgJUgdtZXNzYWdlEh0KCmNsaWVudF9zZXEYAi'
    'ABKANSCWNsaWVudFNlcQ==');

@$core.Deprecated('Use echoResponseDescriptor instead')
const EchoResponse$json = {
  '1': 'EchoResponse',
  '2': [
    {'1': 'message', '3': 1, '4': 1, '5': 9, '10': 'message'},
    {'1': 'client_seq', '3': 2, '4': 1, '5': 3, '10': 'clientSeq'},
    {'1': 'server_time_unix_ms', '3': 3, '4': 1, '5': 3, '10': 'serverTimeUnixMs'},
  ],
};

/// Descriptor for `EchoResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List echoResponseDescriptor = $convert.base64Decode(
    'CgxFY2hvUmVzcG9uc2USGAoHbWVzc2FnZRgBIAEoCVIHbWVzc2FnZRIdCgpjbGllbnRfc2VxGA'
    'IgASgDUgljbGllbnRTZXESLQoTc2VydmVyX3RpbWVfdW5peF9tcxgDIAEoA1IQc2VydmVyVGlt'
    'ZVVuaXhNcw==');

@$core.Deprecated('Use whoAmIRequestDescriptor instead')
const WhoAmIRequest$json = {
  '1': 'WhoAmIRequest',
};

/// Descriptor for `WhoAmIRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List whoAmIRequestDescriptor = $convert.base64Decode(
    'Cg1XaG9BbUlSZXF1ZXN0');

@$core.Deprecated('Use whoAmIResponseDescriptor instead')
const WhoAmIResponse$json = {
  '1': 'WhoAmIResponse',
  '2': [
    {'1': 'subject', '3': 1, '4': 1, '5': 9, '10': 'subject'},
  ],
};

/// Descriptor for `WhoAmIResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List whoAmIResponseDescriptor = $convert.base64Decode(
    'Cg5XaG9BbUlSZXNwb25zZRIYCgdzdWJqZWN0GAEgASgJUgdzdWJqZWN0');

const $core.Map<$core.String, $core.dynamic> DemoServiceBase$json = {
  '1': 'DemoService',
  '2': [
    {'1': 'Echo', '2': '.demo.v1.EchoRequest', '3': '.demo.v1.EchoResponse'},
    {'1': 'WhoAmI', '2': '.demo.v1.WhoAmIRequest', '3': '.demo.v1.WhoAmIResponse'},
  ],
};

@$core.Deprecated('Use demoServiceDescriptor instead')
const $core.Map<$core.String, $core.Map<$core.String, $core.dynamic>> DemoServiceBase$messageJson = {
  '.demo.v1.EchoRequest': EchoRequest$json,
  '.demo.v1.EchoResponse': EchoResponse$json,
  '.demo.v1.WhoAmIRequest': WhoAmIRequest$json,
  '.demo.v1.WhoAmIResponse': WhoAmIResponse$json,
};

/// Descriptor for `DemoService`. Decode as a `google.protobuf.ServiceDescriptorProto`.
final $typed_data.Uint8List demoServiceDescriptor = $convert.base64Decode(
    'CgtEZW1vU2VydmljZRIzCgRFY2hvEhQuZGVtby52MS5FY2hvUmVxdWVzdBoVLmRlbW8udjEuRW'
    'Nob1Jlc3BvbnNlEjkKBldob0FtSRIWLmRlbW8udjEuV2hvQW1JUmVxdWVzdBoXLmRlbW8udjEu'
    'V2hvQW1JUmVzcG9uc2U=');

