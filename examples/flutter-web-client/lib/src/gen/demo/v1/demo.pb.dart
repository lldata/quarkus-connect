//
//  Generated code. Do not modify.
//  source: demo/v1/demo.proto
//
// @dart = 2.12

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_final_fields
// ignore_for_file: unnecessary_import, unnecessary_this, unused_import

import 'dart:async' as $async;
import 'dart:core' as $core;

import 'package:fixnum/fixnum.dart' as $fixnum;
import 'package:protobuf/protobuf.dart' as $pb;

class EchoRequest extends $pb.GeneratedMessage {
  factory EchoRequest({
    $core.String? message,
    $fixnum.Int64? clientSeq,
  }) {
    final $result = create();
    if (message != null) {
      $result.message = message;
    }
    if (clientSeq != null) {
      $result.clientSeq = clientSeq;
    }
    return $result;
  }
  EchoRequest._() : super();
  factory EchoRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory EchoRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'EchoRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'demo.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'message')
    ..aInt64(2, _omitFieldNames ? '' : 'clientSeq')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  EchoRequest clone() => EchoRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  EchoRequest copyWith(void Function(EchoRequest) updates) => super.copyWith((message) => updates(message as EchoRequest)) as EchoRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static EchoRequest create() => EchoRequest._();
  EchoRequest createEmptyInstance() => create();
  static $pb.PbList<EchoRequest> createRepeated() => $pb.PbList<EchoRequest>();
  @$core.pragma('dart2js:noInline')
  static EchoRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<EchoRequest>(create);
  static EchoRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get message => $_getSZ(0);
  @$pb.TagNumber(1)
  set message($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessage() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessage() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get clientSeq => $_getI64(1);
  @$pb.TagNumber(2)
  set clientSeq($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasClientSeq() => $_has(1);
  @$pb.TagNumber(2)
  void clearClientSeq() => clearField(2);
}

class EchoResponse extends $pb.GeneratedMessage {
  factory EchoResponse({
    $core.String? message,
    $fixnum.Int64? clientSeq,
    $fixnum.Int64? serverTimeUnixMs,
  }) {
    final $result = create();
    if (message != null) {
      $result.message = message;
    }
    if (clientSeq != null) {
      $result.clientSeq = clientSeq;
    }
    if (serverTimeUnixMs != null) {
      $result.serverTimeUnixMs = serverTimeUnixMs;
    }
    return $result;
  }
  EchoResponse._() : super();
  factory EchoResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory EchoResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'EchoResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'demo.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'message')
    ..aInt64(2, _omitFieldNames ? '' : 'clientSeq')
    ..aInt64(3, _omitFieldNames ? '' : 'serverTimeUnixMs')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  EchoResponse clone() => EchoResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  EchoResponse copyWith(void Function(EchoResponse) updates) => super.copyWith((message) => updates(message as EchoResponse)) as EchoResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static EchoResponse create() => EchoResponse._();
  EchoResponse createEmptyInstance() => create();
  static $pb.PbList<EchoResponse> createRepeated() => $pb.PbList<EchoResponse>();
  @$core.pragma('dart2js:noInline')
  static EchoResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<EchoResponse>(create);
  static EchoResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get message => $_getSZ(0);
  @$pb.TagNumber(1)
  set message($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessage() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessage() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get clientSeq => $_getI64(1);
  @$pb.TagNumber(2)
  set clientSeq($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasClientSeq() => $_has(1);
  @$pb.TagNumber(2)
  void clearClientSeq() => clearField(2);

  @$pb.TagNumber(3)
  $fixnum.Int64 get serverTimeUnixMs => $_getI64(2);
  @$pb.TagNumber(3)
  set serverTimeUnixMs($fixnum.Int64 v) { $_setInt64(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasServerTimeUnixMs() => $_has(2);
  @$pb.TagNumber(3)
  void clearServerTimeUnixMs() => clearField(3);
}

class WhoAmIRequest extends $pb.GeneratedMessage {
  factory WhoAmIRequest() => create();
  WhoAmIRequest._() : super();
  factory WhoAmIRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory WhoAmIRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'WhoAmIRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'demo.v1'), createEmptyInstance: create)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  WhoAmIRequest clone() => WhoAmIRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  WhoAmIRequest copyWith(void Function(WhoAmIRequest) updates) => super.copyWith((message) => updates(message as WhoAmIRequest)) as WhoAmIRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static WhoAmIRequest create() => WhoAmIRequest._();
  WhoAmIRequest createEmptyInstance() => create();
  static $pb.PbList<WhoAmIRequest> createRepeated() => $pb.PbList<WhoAmIRequest>();
  @$core.pragma('dart2js:noInline')
  static WhoAmIRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<WhoAmIRequest>(create);
  static WhoAmIRequest? _defaultInstance;
}

class WhoAmIResponse extends $pb.GeneratedMessage {
  factory WhoAmIResponse({
    $core.String? subject,
  }) {
    final $result = create();
    if (subject != null) {
      $result.subject = subject;
    }
    return $result;
  }
  WhoAmIResponse._() : super();
  factory WhoAmIResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory WhoAmIResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'WhoAmIResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'demo.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'subject')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  WhoAmIResponse clone() => WhoAmIResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  WhoAmIResponse copyWith(void Function(WhoAmIResponse) updates) => super.copyWith((message) => updates(message as WhoAmIResponse)) as WhoAmIResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static WhoAmIResponse create() => WhoAmIResponse._();
  WhoAmIResponse createEmptyInstance() => create();
  static $pb.PbList<WhoAmIResponse> createRepeated() => $pb.PbList<WhoAmIResponse>();
  @$core.pragma('dart2js:noInline')
  static WhoAmIResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<WhoAmIResponse>(create);
  static WhoAmIResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get subject => $_getSZ(0);
  @$pb.TagNumber(1)
  set subject($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasSubject() => $_has(0);
  @$pb.TagNumber(1)
  void clearSubject() => clearField(1);
}

class DemoServiceApi {
  $pb.RpcClient _client;
  DemoServiceApi(this._client);

  $async.Future<EchoResponse> echo($pb.ClientContext? ctx, EchoRequest request) =>
    _client.invoke<EchoResponse>(ctx, 'DemoService', 'Echo', request, EchoResponse())
  ;
  $async.Future<WhoAmIResponse> whoAmI($pb.ClientContext? ctx, WhoAmIRequest request) =>
    _client.invoke<WhoAmIResponse>(ctx, 'DemoService', 'WhoAmI', request, WhoAmIResponse())
  ;
}


const _omitFieldNames = $core.bool.fromEnvironment('protobuf.omit_field_names');
const _omitMessageNames = $core.bool.fromEnvironment('protobuf.omit_message_names');
