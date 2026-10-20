//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
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

import 'media.pb.dart' as $1;
import 'media.pbjson.dart';

export 'media.pb.dart';

abstract class MediaServiceBase extends $pb.GeneratedService {
  $async.Future<$1.BeginUploadResponse> beginUpload($pb.ServerContext ctx, $1.BeginUploadRequest request);
  $async.Future<$1.UploadChunkResponse> uploadChunk($pb.ServerContext ctx, $1.UploadChunkRequest request);
  $async.Future<$1.GetUploadStatusResponse> getUploadStatus($pb.ServerContext ctx, $1.GetUploadStatusRequest request);
  $async.Future<$1.CompleteUploadResponse> completeUpload($pb.ServerContext ctx, $1.CompleteUploadRequest request);
  $async.Future<$1.GetMessageResponse> getMessage($pb.ServerContext ctx, $1.GetMessageRequest request);
  $async.Future<$1.ListMessagesResponse> listMessages($pb.ServerContext ctx, $1.ListMessagesRequest request);
  $async.Future<$1.Message> watchMessages($pb.ServerContext ctx, $1.WatchMessagesRequest request);

  $pb.GeneratedMessage createRequest($core.String methodName) {
    switch (methodName) {
      case 'BeginUpload': return $1.BeginUploadRequest();
      case 'UploadChunk': return $1.UploadChunkRequest();
      case 'GetUploadStatus': return $1.GetUploadStatusRequest();
      case 'CompleteUpload': return $1.CompleteUploadRequest();
      case 'GetMessage': return $1.GetMessageRequest();
      case 'ListMessages': return $1.ListMessagesRequest();
      case 'WatchMessages': return $1.WatchMessagesRequest();
      default: throw $core.ArgumentError('Unknown method: $methodName');
    }
  }

  $async.Future<$pb.GeneratedMessage> handleCall($pb.ServerContext ctx, $core.String methodName, $pb.GeneratedMessage request) {
    switch (methodName) {
      case 'BeginUpload': return this.beginUpload(ctx, request as $1.BeginUploadRequest);
      case 'UploadChunk': return this.uploadChunk(ctx, request as $1.UploadChunkRequest);
      case 'GetUploadStatus': return this.getUploadStatus(ctx, request as $1.GetUploadStatusRequest);
      case 'CompleteUpload': return this.completeUpload(ctx, request as $1.CompleteUploadRequest);
      case 'GetMessage': return this.getMessage(ctx, request as $1.GetMessageRequest);
      case 'ListMessages': return this.listMessages(ctx, request as $1.ListMessagesRequest);
      case 'WatchMessages': return this.watchMessages(ctx, request as $1.WatchMessagesRequest);
      default: throw $core.ArgumentError('Unknown method: $methodName');
    }
  }

  $core.Map<$core.String, $core.dynamic> get $json => MediaServiceBase$json;
  $core.Map<$core.String, $core.Map<$core.String, $core.dynamic>> get $messageJson => MediaServiceBase$messageJson;
}

