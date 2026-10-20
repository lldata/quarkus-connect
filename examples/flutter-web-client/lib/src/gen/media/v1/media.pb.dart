//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
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

import 'media.pbenum.dart';

export 'media.pbenum.dart';

class BeginUploadRequest extends $pb.GeneratedMessage {
  factory BeginUploadRequest({
    $core.String? clientRequestId,
    $core.String? filename,
    $core.String? contentType,
    $fixnum.Int64? totalSizeBytes,
    MediaKind? kind,
  }) {
    final $result = create();
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    if (filename != null) {
      $result.filename = filename;
    }
    if (contentType != null) {
      $result.contentType = contentType;
    }
    if (totalSizeBytes != null) {
      $result.totalSizeBytes = totalSizeBytes;
    }
    if (kind != null) {
      $result.kind = kind;
    }
    return $result;
  }
  BeginUploadRequest._() : super();
  factory BeginUploadRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory BeginUploadRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'BeginUploadRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'clientRequestId')
    ..aOS(2, _omitFieldNames ? '' : 'filename')
    ..aOS(3, _omitFieldNames ? '' : 'contentType')
    ..aInt64(4, _omitFieldNames ? '' : 'totalSizeBytes')
    ..e<MediaKind>(5, _omitFieldNames ? '' : 'kind', $pb.PbFieldType.OE, defaultOrMaker: MediaKind.MEDIA_KIND_UNSPECIFIED, valueOf: MediaKind.valueOf, enumValues: MediaKind.values)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  BeginUploadRequest clone() => BeginUploadRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  BeginUploadRequest copyWith(void Function(BeginUploadRequest) updates) => super.copyWith((message) => updates(message as BeginUploadRequest)) as BeginUploadRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static BeginUploadRequest create() => BeginUploadRequest._();
  BeginUploadRequest createEmptyInstance() => create();
  static $pb.PbList<BeginUploadRequest> createRepeated() => $pb.PbList<BeginUploadRequest>();
  @$core.pragma('dart2js:noInline')
  static BeginUploadRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<BeginUploadRequest>(create);
  static BeginUploadRequest? _defaultInstance;

  /// Stable id chosen by the client (UUID). Retries never create a second upload.
  @$pb.TagNumber(1)
  $core.String get clientRequestId => $_getSZ(0);
  @$pb.TagNumber(1)
  set clientRequestId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasClientRequestId() => $_has(0);
  @$pb.TagNumber(1)
  void clearClientRequestId() => clearField(1);

  @$pb.TagNumber(2)
  $core.String get filename => $_getSZ(1);
  @$pb.TagNumber(2)
  set filename($core.String v) { $_setString(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasFilename() => $_has(1);
  @$pb.TagNumber(2)
  void clearFilename() => clearField(2);

  @$pb.TagNumber(3)
  $core.String get contentType => $_getSZ(2);
  @$pb.TagNumber(3)
  set contentType($core.String v) { $_setString(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasContentType() => $_has(2);
  @$pb.TagNumber(3)
  void clearContentType() => clearField(3);

  @$pb.TagNumber(4)
  $fixnum.Int64 get totalSizeBytes => $_getI64(3);
  @$pb.TagNumber(4)
  set totalSizeBytes($fixnum.Int64 v) { $_setInt64(3, v); }
  @$pb.TagNumber(4)
  $core.bool hasTotalSizeBytes() => $_has(3);
  @$pb.TagNumber(4)
  void clearTotalSizeBytes() => clearField(4);

  @$pb.TagNumber(5)
  MediaKind get kind => $_getN(4);
  @$pb.TagNumber(5)
  set kind(MediaKind v) { setField(5, v); }
  @$pb.TagNumber(5)
  $core.bool hasKind() => $_has(4);
  @$pb.TagNumber(5)
  void clearKind() => clearField(5);
}

class BeginUploadResponse extends $pb.GeneratedMessage {
  factory BeginUploadResponse({
    $core.String? uploadId,
    $core.String? clientRequestId,
    $fixnum.Int64? nextOffset,
    $core.bool? resumed,
  }) {
    final $result = create();
    if (uploadId != null) {
      $result.uploadId = uploadId;
    }
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    if (nextOffset != null) {
      $result.nextOffset = nextOffset;
    }
    if (resumed != null) {
      $result.resumed = resumed;
    }
    return $result;
  }
  BeginUploadResponse._() : super();
  factory BeginUploadResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory BeginUploadResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'BeginUploadResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'uploadId')
    ..aOS(2, _omitFieldNames ? '' : 'clientRequestId')
    ..aInt64(3, _omitFieldNames ? '' : 'nextOffset')
    ..aOB(4, _omitFieldNames ? '' : 'resumed')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  BeginUploadResponse clone() => BeginUploadResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  BeginUploadResponse copyWith(void Function(BeginUploadResponse) updates) => super.copyWith((message) => updates(message as BeginUploadResponse)) as BeginUploadResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static BeginUploadResponse create() => BeginUploadResponse._();
  BeginUploadResponse createEmptyInstance() => create();
  static $pb.PbList<BeginUploadResponse> createRepeated() => $pb.PbList<BeginUploadResponse>();
  @$core.pragma('dart2js:noInline')
  static BeginUploadResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<BeginUploadResponse>(create);
  static BeginUploadResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get uploadId => $_getSZ(0);
  @$pb.TagNumber(1)
  set uploadId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasUploadId() => $_has(0);
  @$pb.TagNumber(1)
  void clearUploadId() => clearField(1);

  @$pb.TagNumber(2)
  $core.String get clientRequestId => $_getSZ(1);
  @$pb.TagNumber(2)
  set clientRequestId($core.String v) { $_setString(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasClientRequestId() => $_has(1);
  @$pb.TagNumber(2)
  void clearClientRequestId() => clearField(2);

  /// Next byte offset the server expects (0 for new uploads).
  @$pb.TagNumber(3)
  $fixnum.Int64 get nextOffset => $_getI64(2);
  @$pb.TagNumber(3)
  set nextOffset($fixnum.Int64 v) { $_setInt64(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasNextOffset() => $_has(2);
  @$pb.TagNumber(3)
  void clearNextOffset() => clearField(3);

  @$pb.TagNumber(4)
  $core.bool get resumed => $_getBF(3);
  @$pb.TagNumber(4)
  set resumed($core.bool v) { $_setBool(3, v); }
  @$pb.TagNumber(4)
  $core.bool hasResumed() => $_has(3);
  @$pb.TagNumber(4)
  void clearResumed() => clearField(4);
}

class UploadChunkRequest extends $pb.GeneratedMessage {
  factory UploadChunkRequest({
    $core.String? clientRequestId,
    $fixnum.Int64? offset,
    $core.List<$core.int>? data,
    $core.int? chunkIndex,
  }) {
    final $result = create();
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    if (offset != null) {
      $result.offset = offset;
    }
    if (data != null) {
      $result.data = data;
    }
    if (chunkIndex != null) {
      $result.chunkIndex = chunkIndex;
    }
    return $result;
  }
  UploadChunkRequest._() : super();
  factory UploadChunkRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory UploadChunkRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'UploadChunkRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'clientRequestId')
    ..aInt64(2, _omitFieldNames ? '' : 'offset')
    ..a<$core.List<$core.int>>(3, _omitFieldNames ? '' : 'data', $pb.PbFieldType.OY)
    ..a<$core.int>(4, _omitFieldNames ? '' : 'chunkIndex', $pb.PbFieldType.O3)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  UploadChunkRequest clone() => UploadChunkRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  UploadChunkRequest copyWith(void Function(UploadChunkRequest) updates) => super.copyWith((message) => updates(message as UploadChunkRequest)) as UploadChunkRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static UploadChunkRequest create() => UploadChunkRequest._();
  UploadChunkRequest createEmptyInstance() => create();
  static $pb.PbList<UploadChunkRequest> createRepeated() => $pb.PbList<UploadChunkRequest>();
  @$core.pragma('dart2js:noInline')
  static UploadChunkRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<UploadChunkRequest>(create);
  static UploadChunkRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get clientRequestId => $_getSZ(0);
  @$pb.TagNumber(1)
  set clientRequestId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasClientRequestId() => $_has(0);
  @$pb.TagNumber(1)
  void clearClientRequestId() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get offset => $_getI64(1);
  @$pb.TagNumber(2)
  set offset($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasOffset() => $_has(1);
  @$pb.TagNumber(2)
  void clearOffset() => clearField(2);

  @$pb.TagNumber(3)
  $core.List<$core.int> get data => $_getN(2);
  @$pb.TagNumber(3)
  set data($core.List<$core.int> v) { $_setBytes(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasData() => $_has(2);
  @$pb.TagNumber(3)
  void clearData() => clearField(3);

  /// Monotonic chunk index from 0; used with offset for idempotent retries.
  @$pb.TagNumber(4)
  $core.int get chunkIndex => $_getIZ(3);
  @$pb.TagNumber(4)
  set chunkIndex($core.int v) { $_setSignedInt32(3, v); }
  @$pb.TagNumber(4)
  $core.bool hasChunkIndex() => $_has(3);
  @$pb.TagNumber(4)
  void clearChunkIndex() => clearField(4);
}

class UploadChunkResponse extends $pb.GeneratedMessage {
  factory UploadChunkResponse({
    $fixnum.Int64? bytesReceived,
    $fixnum.Int64? nextOffset,
  }) {
    final $result = create();
    if (bytesReceived != null) {
      $result.bytesReceived = bytesReceived;
    }
    if (nextOffset != null) {
      $result.nextOffset = nextOffset;
    }
    return $result;
  }
  UploadChunkResponse._() : super();
  factory UploadChunkResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory UploadChunkResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'UploadChunkResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aInt64(1, _omitFieldNames ? '' : 'bytesReceived')
    ..aInt64(2, _omitFieldNames ? '' : 'nextOffset')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  UploadChunkResponse clone() => UploadChunkResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  UploadChunkResponse copyWith(void Function(UploadChunkResponse) updates) => super.copyWith((message) => updates(message as UploadChunkResponse)) as UploadChunkResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static UploadChunkResponse create() => UploadChunkResponse._();
  UploadChunkResponse createEmptyInstance() => create();
  static $pb.PbList<UploadChunkResponse> createRepeated() => $pb.PbList<UploadChunkResponse>();
  @$core.pragma('dart2js:noInline')
  static UploadChunkResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<UploadChunkResponse>(create);
  static UploadChunkResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $fixnum.Int64 get bytesReceived => $_getI64(0);
  @$pb.TagNumber(1)
  set bytesReceived($fixnum.Int64 v) { $_setInt64(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasBytesReceived() => $_has(0);
  @$pb.TagNumber(1)
  void clearBytesReceived() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get nextOffset => $_getI64(1);
  @$pb.TagNumber(2)
  set nextOffset($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasNextOffset() => $_has(1);
  @$pb.TagNumber(2)
  void clearNextOffset() => clearField(2);
}

class GetUploadStatusRequest extends $pb.GeneratedMessage {
  factory GetUploadStatusRequest({
    $core.String? clientRequestId,
  }) {
    final $result = create();
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    return $result;
  }
  GetUploadStatusRequest._() : super();
  factory GetUploadStatusRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory GetUploadStatusRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'GetUploadStatusRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'clientRequestId')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  GetUploadStatusRequest clone() => GetUploadStatusRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  GetUploadStatusRequest copyWith(void Function(GetUploadStatusRequest) updates) => super.copyWith((message) => updates(message as GetUploadStatusRequest)) as GetUploadStatusRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static GetUploadStatusRequest create() => GetUploadStatusRequest._();
  GetUploadStatusRequest createEmptyInstance() => create();
  static $pb.PbList<GetUploadStatusRequest> createRepeated() => $pb.PbList<GetUploadStatusRequest>();
  @$core.pragma('dart2js:noInline')
  static GetUploadStatusRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<GetUploadStatusRequest>(create);
  static GetUploadStatusRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get clientRequestId => $_getSZ(0);
  @$pb.TagNumber(1)
  set clientRequestId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasClientRequestId() => $_has(0);
  @$pb.TagNumber(1)
  void clearClientRequestId() => clearField(1);
}

class GetUploadStatusResponse extends $pb.GeneratedMessage {
  factory GetUploadStatusResponse({
    $core.String? uploadId,
    $fixnum.Int64? bytesReceived,
    $fixnum.Int64? nextOffset,
    $fixnum.Int64? totalSizeBytes,
    $core.bool? completed,
    $core.String? messageId,
  }) {
    final $result = create();
    if (uploadId != null) {
      $result.uploadId = uploadId;
    }
    if (bytesReceived != null) {
      $result.bytesReceived = bytesReceived;
    }
    if (nextOffset != null) {
      $result.nextOffset = nextOffset;
    }
    if (totalSizeBytes != null) {
      $result.totalSizeBytes = totalSizeBytes;
    }
    if (completed != null) {
      $result.completed = completed;
    }
    if (messageId != null) {
      $result.messageId = messageId;
    }
    return $result;
  }
  GetUploadStatusResponse._() : super();
  factory GetUploadStatusResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory GetUploadStatusResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'GetUploadStatusResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'uploadId')
    ..aInt64(2, _omitFieldNames ? '' : 'bytesReceived')
    ..aInt64(3, _omitFieldNames ? '' : 'nextOffset')
    ..aInt64(4, _omitFieldNames ? '' : 'totalSizeBytes')
    ..aOB(5, _omitFieldNames ? '' : 'completed')
    ..aOS(6, _omitFieldNames ? '' : 'messageId')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  GetUploadStatusResponse clone() => GetUploadStatusResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  GetUploadStatusResponse copyWith(void Function(GetUploadStatusResponse) updates) => super.copyWith((message) => updates(message as GetUploadStatusResponse)) as GetUploadStatusResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static GetUploadStatusResponse create() => GetUploadStatusResponse._();
  GetUploadStatusResponse createEmptyInstance() => create();
  static $pb.PbList<GetUploadStatusResponse> createRepeated() => $pb.PbList<GetUploadStatusResponse>();
  @$core.pragma('dart2js:noInline')
  static GetUploadStatusResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<GetUploadStatusResponse>(create);
  static GetUploadStatusResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get uploadId => $_getSZ(0);
  @$pb.TagNumber(1)
  set uploadId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasUploadId() => $_has(0);
  @$pb.TagNumber(1)
  void clearUploadId() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get bytesReceived => $_getI64(1);
  @$pb.TagNumber(2)
  set bytesReceived($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasBytesReceived() => $_has(1);
  @$pb.TagNumber(2)
  void clearBytesReceived() => clearField(2);

  @$pb.TagNumber(3)
  $fixnum.Int64 get nextOffset => $_getI64(2);
  @$pb.TagNumber(3)
  set nextOffset($fixnum.Int64 v) { $_setInt64(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasNextOffset() => $_has(2);
  @$pb.TagNumber(3)
  void clearNextOffset() => clearField(3);

  @$pb.TagNumber(4)
  $fixnum.Int64 get totalSizeBytes => $_getI64(3);
  @$pb.TagNumber(4)
  set totalSizeBytes($fixnum.Int64 v) { $_setInt64(3, v); }
  @$pb.TagNumber(4)
  $core.bool hasTotalSizeBytes() => $_has(3);
  @$pb.TagNumber(4)
  void clearTotalSizeBytes() => clearField(4);

  @$pb.TagNumber(5)
  $core.bool get completed => $_getBF(4);
  @$pb.TagNumber(5)
  set completed($core.bool v) { $_setBool(4, v); }
  @$pb.TagNumber(5)
  $core.bool hasCompleted() => $_has(4);
  @$pb.TagNumber(5)
  void clearCompleted() => clearField(5);

  @$pb.TagNumber(6)
  $core.String get messageId => $_getSZ(5);
  @$pb.TagNumber(6)
  set messageId($core.String v) { $_setString(5, v); }
  @$pb.TagNumber(6)
  $core.bool hasMessageId() => $_has(5);
  @$pb.TagNumber(6)
  void clearMessageId() => clearField(6);
}

class CompleteUploadRequest extends $pb.GeneratedMessage {
  factory CompleteUploadRequest({
    $core.String? clientRequestId,
    $fixnum.Int64? durationMs,
    $core.List<$core.int>? posterJpeg,
  }) {
    final $result = create();
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    if (durationMs != null) {
      $result.durationMs = durationMs;
    }
    if (posterJpeg != null) {
      $result.posterJpeg = posterJpeg;
    }
    return $result;
  }
  CompleteUploadRequest._() : super();
  factory CompleteUploadRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory CompleteUploadRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'CompleteUploadRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'clientRequestId')
    ..aInt64(2, _omitFieldNames ? '' : 'durationMs')
    ..a<$core.List<$core.int>>(3, _omitFieldNames ? '' : 'posterJpeg', $pb.PbFieldType.OY)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  CompleteUploadRequest clone() => CompleteUploadRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  CompleteUploadRequest copyWith(void Function(CompleteUploadRequest) updates) => super.copyWith((message) => updates(message as CompleteUploadRequest)) as CompleteUploadRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static CompleteUploadRequest create() => CompleteUploadRequest._();
  CompleteUploadRequest createEmptyInstance() => create();
  static $pb.PbList<CompleteUploadRequest> createRepeated() => $pb.PbList<CompleteUploadRequest>();
  @$core.pragma('dart2js:noInline')
  static CompleteUploadRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<CompleteUploadRequest>(create);
  static CompleteUploadRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get clientRequestId => $_getSZ(0);
  @$pb.TagNumber(1)
  set clientRequestId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasClientRequestId() => $_has(0);
  @$pb.TagNumber(1)
  void clearClientRequestId() => clearField(1);

  /// Client-supplied metadata (no server-side ffmpeg required for the proof).
  @$pb.TagNumber(2)
  $fixnum.Int64 get durationMs => $_getI64(1);
  @$pb.TagNumber(2)
  set durationMs($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasDurationMs() => $_has(1);
  @$pb.TagNumber(2)
  void clearDurationMs() => clearField(2);

  @$pb.TagNumber(3)
  $core.List<$core.int> get posterJpeg => $_getN(2);
  @$pb.TagNumber(3)
  set posterJpeg($core.List<$core.int> v) { $_setBytes(2, v); }
  @$pb.TagNumber(3)
  $core.bool hasPosterJpeg() => $_has(2);
  @$pb.TagNumber(3)
  void clearPosterJpeg() => clearField(3);
}

class CompleteUploadResponse extends $pb.GeneratedMessage {
  factory CompleteUploadResponse({
    Message? message,
  }) {
    final $result = create();
    if (message != null) {
      $result.message = message;
    }
    return $result;
  }
  CompleteUploadResponse._() : super();
  factory CompleteUploadResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory CompleteUploadResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'CompleteUploadResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOM<Message>(1, _omitFieldNames ? '' : 'message', subBuilder: Message.create)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  CompleteUploadResponse clone() => CompleteUploadResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  CompleteUploadResponse copyWith(void Function(CompleteUploadResponse) updates) => super.copyWith((message) => updates(message as CompleteUploadResponse)) as CompleteUploadResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static CompleteUploadResponse create() => CompleteUploadResponse._();
  CompleteUploadResponse createEmptyInstance() => create();
  static $pb.PbList<CompleteUploadResponse> createRepeated() => $pb.PbList<CompleteUploadResponse>();
  @$core.pragma('dart2js:noInline')
  static CompleteUploadResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<CompleteUploadResponse>(create);
  static CompleteUploadResponse? _defaultInstance;

  @$pb.TagNumber(1)
  Message get message => $_getN(0);
  @$pb.TagNumber(1)
  set message(Message v) { setField(1, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessage() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessage() => clearField(1);
  @$pb.TagNumber(1)
  Message ensureMessage() => $_ensure(0);
}

class GetMessageRequest extends $pb.GeneratedMessage {
  factory GetMessageRequest({
    $core.String? messageId,
  }) {
    final $result = create();
    if (messageId != null) {
      $result.messageId = messageId;
    }
    return $result;
  }
  GetMessageRequest._() : super();
  factory GetMessageRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory GetMessageRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'GetMessageRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'messageId')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  GetMessageRequest clone() => GetMessageRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  GetMessageRequest copyWith(void Function(GetMessageRequest) updates) => super.copyWith((message) => updates(message as GetMessageRequest)) as GetMessageRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static GetMessageRequest create() => GetMessageRequest._();
  GetMessageRequest createEmptyInstance() => create();
  static $pb.PbList<GetMessageRequest> createRepeated() => $pb.PbList<GetMessageRequest>();
  @$core.pragma('dart2js:noInline')
  static GetMessageRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<GetMessageRequest>(create);
  static GetMessageRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get messageId => $_getSZ(0);
  @$pb.TagNumber(1)
  set messageId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessageId() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessageId() => clearField(1);
}

class GetMessageResponse extends $pb.GeneratedMessage {
  factory GetMessageResponse({
    Message? message,
  }) {
    final $result = create();
    if (message != null) {
      $result.message = message;
    }
    return $result;
  }
  GetMessageResponse._() : super();
  factory GetMessageResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory GetMessageResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'GetMessageResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOM<Message>(1, _omitFieldNames ? '' : 'message', subBuilder: Message.create)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  GetMessageResponse clone() => GetMessageResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  GetMessageResponse copyWith(void Function(GetMessageResponse) updates) => super.copyWith((message) => updates(message as GetMessageResponse)) as GetMessageResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static GetMessageResponse create() => GetMessageResponse._();
  GetMessageResponse createEmptyInstance() => create();
  static $pb.PbList<GetMessageResponse> createRepeated() => $pb.PbList<GetMessageResponse>();
  @$core.pragma('dart2js:noInline')
  static GetMessageResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<GetMessageResponse>(create);
  static GetMessageResponse? _defaultInstance;

  @$pb.TagNumber(1)
  Message get message => $_getN(0);
  @$pb.TagNumber(1)
  set message(Message v) { setField(1, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessage() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessage() => clearField(1);
  @$pb.TagNumber(1)
  Message ensureMessage() => $_ensure(0);
}

class ListMessagesRequest extends $pb.GeneratedMessage {
  factory ListMessagesRequest({
    $fixnum.Int64? afterSequence,
    $core.int? limit,
  }) {
    final $result = create();
    if (afterSequence != null) {
      $result.afterSequence = afterSequence;
    }
    if (limit != null) {
      $result.limit = limit;
    }
    return $result;
  }
  ListMessagesRequest._() : super();
  factory ListMessagesRequest.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory ListMessagesRequest.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'ListMessagesRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aInt64(1, _omitFieldNames ? '' : 'afterSequence')
    ..a<$core.int>(2, _omitFieldNames ? '' : 'limit', $pb.PbFieldType.O3)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  ListMessagesRequest clone() => ListMessagesRequest()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  ListMessagesRequest copyWith(void Function(ListMessagesRequest) updates) => super.copyWith((message) => updates(message as ListMessagesRequest)) as ListMessagesRequest;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static ListMessagesRequest create() => ListMessagesRequest._();
  ListMessagesRequest createEmptyInstance() => create();
  static $pb.PbList<ListMessagesRequest> createRepeated() => $pb.PbList<ListMessagesRequest>();
  @$core.pragma('dart2js:noInline')
  static ListMessagesRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<ListMessagesRequest>(create);
  static ListMessagesRequest? _defaultInstance;

  /// Return messages with sequence > after_sequence (0 = from start).
  @$pb.TagNumber(1)
  $fixnum.Int64 get afterSequence => $_getI64(0);
  @$pb.TagNumber(1)
  set afterSequence($fixnum.Int64 v) { $_setInt64(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasAfterSequence() => $_has(0);
  @$pb.TagNumber(1)
  void clearAfterSequence() => clearField(1);

  @$pb.TagNumber(2)
  $core.int get limit => $_getIZ(1);
  @$pb.TagNumber(2)
  set limit($core.int v) { $_setSignedInt32(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasLimit() => $_has(1);
  @$pb.TagNumber(2)
  void clearLimit() => clearField(2);
}

class ListMessagesResponse extends $pb.GeneratedMessage {
  factory ListMessagesResponse({
    $core.Iterable<Message>? messages,
    $fixnum.Int64? latestSequence,
  }) {
    final $result = create();
    if (messages != null) {
      $result.messages.addAll(messages);
    }
    if (latestSequence != null) {
      $result.latestSequence = latestSequence;
    }
    return $result;
  }
  ListMessagesResponse._() : super();
  factory ListMessagesResponse.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory ListMessagesResponse.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'ListMessagesResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..pc<Message>(1, _omitFieldNames ? '' : 'messages', $pb.PbFieldType.PM, subBuilder: Message.create)
    ..aInt64(2, _omitFieldNames ? '' : 'latestSequence')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  ListMessagesResponse clone() => ListMessagesResponse()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  ListMessagesResponse copyWith(void Function(ListMessagesResponse) updates) => super.copyWith((message) => updates(message as ListMessagesResponse)) as ListMessagesResponse;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static ListMessagesResponse create() => ListMessagesResponse._();
  ListMessagesResponse createEmptyInstance() => create();
  static $pb.PbList<ListMessagesResponse> createRepeated() => $pb.PbList<ListMessagesResponse>();
  @$core.pragma('dart2js:noInline')
  static ListMessagesResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<ListMessagesResponse>(create);
  static ListMessagesResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.List<Message> get messages => $_getList(0);

  @$pb.TagNumber(2)
  $fixnum.Int64 get latestSequence => $_getI64(1);
  @$pb.TagNumber(2)
  set latestSequence($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasLatestSequence() => $_has(1);
  @$pb.TagNumber(2)
  void clearLatestSequence() => clearField(2);
}

class Message extends $pb.GeneratedMessage {
  factory Message({
    $core.String? messageId,
    $fixnum.Int64? sequence,
    MediaKind? kind,
    $core.String? filename,
    $core.String? contentType,
    $fixnum.Int64? sizeBytes,
    $fixnum.Int64? durationMs,
    $core.String? mediaUrl,
    $core.String? posterUrl,
    $core.String? clientRequestId,
    $fixnum.Int64? createdUnixMs,
  }) {
    final $result = create();
    if (messageId != null) {
      $result.messageId = messageId;
    }
    if (sequence != null) {
      $result.sequence = sequence;
    }
    if (kind != null) {
      $result.kind = kind;
    }
    if (filename != null) {
      $result.filename = filename;
    }
    if (contentType != null) {
      $result.contentType = contentType;
    }
    if (sizeBytes != null) {
      $result.sizeBytes = sizeBytes;
    }
    if (durationMs != null) {
      $result.durationMs = durationMs;
    }
    if (mediaUrl != null) {
      $result.mediaUrl = mediaUrl;
    }
    if (posterUrl != null) {
      $result.posterUrl = posterUrl;
    }
    if (clientRequestId != null) {
      $result.clientRequestId = clientRequestId;
    }
    if (createdUnixMs != null) {
      $result.createdUnixMs = createdUnixMs;
    }
    return $result;
  }
  Message._() : super();
  factory Message.fromBuffer($core.List<$core.int> i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(i, r);
  factory Message.fromJson($core.String i, [$pb.ExtensionRegistry r = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(i, r);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'Message', package: const $pb.PackageName(_omitMessageNames ? '' : 'media.v1'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'messageId')
    ..aInt64(2, _omitFieldNames ? '' : 'sequence')
    ..e<MediaKind>(3, _omitFieldNames ? '' : 'kind', $pb.PbFieldType.OE, defaultOrMaker: MediaKind.MEDIA_KIND_UNSPECIFIED, valueOf: MediaKind.valueOf, enumValues: MediaKind.values)
    ..aOS(4, _omitFieldNames ? '' : 'filename')
    ..aOS(5, _omitFieldNames ? '' : 'contentType')
    ..aInt64(6, _omitFieldNames ? '' : 'sizeBytes')
    ..aInt64(7, _omitFieldNames ? '' : 'durationMs')
    ..aOS(8, _omitFieldNames ? '' : 'mediaUrl')
    ..aOS(9, _omitFieldNames ? '' : 'posterUrl')
    ..aOS(10, _omitFieldNames ? '' : 'clientRequestId')
    ..aInt64(11, _omitFieldNames ? '' : 'createdUnixMs')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.deepCopy] instead. '
  'Will be removed in next major version')
  Message clone() => Message()..mergeFromMessage(this);
  @$core.Deprecated(
  'Using this can add significant overhead to your binary. '
  'Use [GeneratedMessageGenericExtensions.rebuild] instead. '
  'Will be removed in next major version')
  Message copyWith(void Function(Message) updates) => super.copyWith((message) => updates(message as Message)) as Message;

  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static Message create() => Message._();
  Message createEmptyInstance() => create();
  static $pb.PbList<Message> createRepeated() => $pb.PbList<Message>();
  @$core.pragma('dart2js:noInline')
  static Message getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<Message>(create);
  static Message? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get messageId => $_getSZ(0);
  @$pb.TagNumber(1)
  set messageId($core.String v) { $_setString(0, v); }
  @$pb.TagNumber(1)
  $core.bool hasMessageId() => $_has(0);
  @$pb.TagNumber(1)
  void clearMessageId() => clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get sequence => $_getI64(1);
  @$pb.TagNumber(2)
  set sequence($fixnum.Int64 v) { $_setInt64(1, v); }
  @$pb.TagNumber(2)
  $core.bool hasSequence() => $_has(1);
  @$pb.TagNumber(2)
  void clearSequence() => clearField(2);

  @$pb.TagNumber(3)
  MediaKind get kind => $_getN(2);
  @$pb.TagNumber(3)
  set kind(MediaKind v) { setField(3, v); }
  @$pb.TagNumber(3)
  $core.bool hasKind() => $_has(2);
  @$pb.TagNumber(3)
  void clearKind() => clearField(3);

  @$pb.TagNumber(4)
  $core.String get filename => $_getSZ(3);
  @$pb.TagNumber(4)
  set filename($core.String v) { $_setString(3, v); }
  @$pb.TagNumber(4)
  $core.bool hasFilename() => $_has(3);
  @$pb.TagNumber(4)
  void clearFilename() => clearField(4);

  @$pb.TagNumber(5)
  $core.String get contentType => $_getSZ(4);
  @$pb.TagNumber(5)
  set contentType($core.String v) { $_setString(4, v); }
  @$pb.TagNumber(5)
  $core.bool hasContentType() => $_has(4);
  @$pb.TagNumber(5)
  void clearContentType() => clearField(5);

  @$pb.TagNumber(6)
  $fixnum.Int64 get sizeBytes => $_getI64(5);
  @$pb.TagNumber(6)
  set sizeBytes($fixnum.Int64 v) { $_setInt64(5, v); }
  @$pb.TagNumber(6)
  $core.bool hasSizeBytes() => $_has(5);
  @$pb.TagNumber(6)
  void clearSizeBytes() => clearField(6);

  @$pb.TagNumber(7)
  $fixnum.Int64 get durationMs => $_getI64(6);
  @$pb.TagNumber(7)
  set durationMs($fixnum.Int64 v) { $_setInt64(6, v); }
  @$pb.TagNumber(7)
  $core.bool hasDurationMs() => $_has(6);
  @$pb.TagNumber(7)
  void clearDurationMs() => clearField(7);

  /// Relative URLs on the same Quarkus HTTP port.
  @$pb.TagNumber(8)
  $core.String get mediaUrl => $_getSZ(7);
  @$pb.TagNumber(8)
  set mediaUrl($core.String v) { $_setString(7, v); }
  @$pb.TagNumber(8)
  $core.bool hasMediaUrl() => $_has(7);
  @$pb.TagNumber(8)
  void clearMediaUrl() => clearField(8);

  @$pb.TagNumber(9)
  $core.String get posterUrl => $_getSZ(8);
  @$pb.TagNumber(9)
  set posterUrl($core.String v) { $_setString(8, v); }
  @$pb.TagNumber(9)
  $core.bool hasPosterUrl() => $_has(8);
  @$pb.TagNumber(9)
  void clearPosterUrl() => clearField(9);

  @$pb.TagNumber(10)
  $core.String get clientRequestId => $_getSZ(9);
  @$pb.TagNumber(10)
  set clientRequestId($core.String v) { $_setString(9, v); }
  @$pb.TagNumber(10)
  $core.bool hasClientRequestId() => $_has(9);
  @$pb.TagNumber(10)
  void clearClientRequestId() => clearField(10);

  @$pb.TagNumber(11)
  $fixnum.Int64 get createdUnixMs => $_getI64(10);
  @$pb.TagNumber(11)
  set createdUnixMs($fixnum.Int64 v) { $_setInt64(10, v); }
  @$pb.TagNumber(11)
  $core.bool hasCreatedUnixMs() => $_has(10);
  @$pb.TagNumber(11)
  void clearCreatedUnixMs() => clearField(11);
}

class MediaServiceApi {
  $pb.RpcClient _client;
  MediaServiceApi(this._client);

  $async.Future<BeginUploadResponse> beginUpload($pb.ClientContext? ctx, BeginUploadRequest request) =>
    _client.invoke<BeginUploadResponse>(ctx, 'MediaService', 'BeginUpload', request, BeginUploadResponse())
  ;
  $async.Future<UploadChunkResponse> uploadChunk($pb.ClientContext? ctx, UploadChunkRequest request) =>
    _client.invoke<UploadChunkResponse>(ctx, 'MediaService', 'UploadChunk', request, UploadChunkResponse())
  ;
  $async.Future<GetUploadStatusResponse> getUploadStatus($pb.ClientContext? ctx, GetUploadStatusRequest request) =>
    _client.invoke<GetUploadStatusResponse>(ctx, 'MediaService', 'GetUploadStatus', request, GetUploadStatusResponse())
  ;
  $async.Future<CompleteUploadResponse> completeUpload($pb.ClientContext? ctx, CompleteUploadRequest request) =>
    _client.invoke<CompleteUploadResponse>(ctx, 'MediaService', 'CompleteUpload', request, CompleteUploadResponse())
  ;
  $async.Future<GetMessageResponse> getMessage($pb.ClientContext? ctx, GetMessageRequest request) =>
    _client.invoke<GetMessageResponse>(ctx, 'MediaService', 'GetMessage', request, GetMessageResponse())
  ;
  $async.Future<ListMessagesResponse> listMessages($pb.ClientContext? ctx, ListMessagesRequest request) =>
    _client.invoke<ListMessagesResponse>(ctx, 'MediaService', 'ListMessages', request, ListMessagesResponse())
  ;
}


const _omitFieldNames = $core.bool.fromEnvironment('protobuf.omit_field_names');
const _omitMessageNames = $core.bool.fromEnvironment('protobuf.omit_message_names');
