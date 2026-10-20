//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
//
// @dart = 2.12

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_final_fields
// ignore_for_file: unnecessary_import, unnecessary_this, unused_import

import 'dart:convert' as $convert;
import 'dart:core' as $core;
import 'dart:typed_data' as $typed_data;

@$core.Deprecated('Use mediaKindDescriptor instead')
const MediaKind$json = {
  '1': 'MediaKind',
  '2': [
    {'1': 'MEDIA_KIND_UNSPECIFIED', '2': 0},
    {'1': 'MEDIA_KIND_VIDEO', '2': 1},
    {'1': 'MEDIA_KIND_AUDIO', '2': 2},
  ],
};

/// Descriptor for `MediaKind`. Decode as a `google.protobuf.EnumDescriptorProto`.
final $typed_data.Uint8List mediaKindDescriptor = $convert.base64Decode(
    'CglNZWRpYUtpbmQSGgoWTUVESUFfS0lORF9VTlNQRUNJRklFRBAAEhQKEE1FRElBX0tJTkRfVk'
    'lERU8QARIUChBNRURJQV9LSU5EX0FVRElPEAI=');

@$core.Deprecated('Use beginUploadRequestDescriptor instead')
const BeginUploadRequest$json = {
  '1': 'BeginUploadRequest',
  '2': [
    {'1': 'client_request_id', '3': 1, '4': 1, '5': 9, '10': 'clientRequestId'},
    {'1': 'filename', '3': 2, '4': 1, '5': 9, '10': 'filename'},
    {'1': 'content_type', '3': 3, '4': 1, '5': 9, '10': 'contentType'},
    {'1': 'total_size_bytes', '3': 4, '4': 1, '5': 3, '10': 'totalSizeBytes'},
    {'1': 'kind', '3': 5, '4': 1, '5': 14, '6': '.media.v1.MediaKind', '10': 'kind'},
  ],
};

/// Descriptor for `BeginUploadRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List beginUploadRequestDescriptor = $convert.base64Decode(
    'ChJCZWdpblVwbG9hZFJlcXVlc3QSKgoRY2xpZW50X3JlcXVlc3RfaWQYASABKAlSD2NsaWVudF'
    'JlcXVlc3RJZBIaCghmaWxlbmFtZRgCIAEoCVIIZmlsZW5hbWUSIQoMY29udGVudF90eXBlGAMg'
    'ASgJUgtjb250ZW50VHlwZRIoChB0b3RhbF9zaXplX2J5dGVzGAQgASgDUg50b3RhbFNpemVCeX'
    'RlcxInCgRraW5kGAUgASgOMhMubWVkaWEudjEuTWVkaWFLaW5kUgRraW5k');

@$core.Deprecated('Use beginUploadResponseDescriptor instead')
const BeginUploadResponse$json = {
  '1': 'BeginUploadResponse',
  '2': [
    {'1': 'upload_id', '3': 1, '4': 1, '5': 9, '10': 'uploadId'},
    {'1': 'client_request_id', '3': 2, '4': 1, '5': 9, '10': 'clientRequestId'},
    {'1': 'next_offset', '3': 3, '4': 1, '5': 3, '10': 'nextOffset'},
    {'1': 'resumed', '3': 4, '4': 1, '5': 8, '10': 'resumed'},
  ],
};

/// Descriptor for `BeginUploadResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List beginUploadResponseDescriptor = $convert.base64Decode(
    'ChNCZWdpblVwbG9hZFJlc3BvbnNlEhsKCXVwbG9hZF9pZBgBIAEoCVIIdXBsb2FkSWQSKgoRY2'
    'xpZW50X3JlcXVlc3RfaWQYAiABKAlSD2NsaWVudFJlcXVlc3RJZBIfCgtuZXh0X29mZnNldBgD'
    'IAEoA1IKbmV4dE9mZnNldBIYCgdyZXN1bWVkGAQgASgIUgdyZXN1bWVk');

@$core.Deprecated('Use uploadChunkRequestDescriptor instead')
const UploadChunkRequest$json = {
  '1': 'UploadChunkRequest',
  '2': [
    {'1': 'client_request_id', '3': 1, '4': 1, '5': 9, '10': 'clientRequestId'},
    {'1': 'offset', '3': 2, '4': 1, '5': 3, '10': 'offset'},
    {'1': 'data', '3': 3, '4': 1, '5': 12, '10': 'data'},
    {'1': 'chunk_index', '3': 4, '4': 1, '5': 5, '10': 'chunkIndex'},
  ],
};

/// Descriptor for `UploadChunkRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List uploadChunkRequestDescriptor = $convert.base64Decode(
    'ChJVcGxvYWRDaHVua1JlcXVlc3QSKgoRY2xpZW50X3JlcXVlc3RfaWQYASABKAlSD2NsaWVudF'
    'JlcXVlc3RJZBIWCgZvZmZzZXQYAiABKANSBm9mZnNldBISCgRkYXRhGAMgASgMUgRkYXRhEh8K'
    'C2NodW5rX2luZGV4GAQgASgFUgpjaHVua0luZGV4');

@$core.Deprecated('Use uploadChunkResponseDescriptor instead')
const UploadChunkResponse$json = {
  '1': 'UploadChunkResponse',
  '2': [
    {'1': 'bytes_received', '3': 1, '4': 1, '5': 3, '10': 'bytesReceived'},
    {'1': 'next_offset', '3': 2, '4': 1, '5': 3, '10': 'nextOffset'},
  ],
};

/// Descriptor for `UploadChunkResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List uploadChunkResponseDescriptor = $convert.base64Decode(
    'ChNVcGxvYWRDaHVua1Jlc3BvbnNlEiUKDmJ5dGVzX3JlY2VpdmVkGAEgASgDUg1ieXRlc1JlY2'
    'VpdmVkEh8KC25leHRfb2Zmc2V0GAIgASgDUgpuZXh0T2Zmc2V0');

@$core.Deprecated('Use getUploadStatusRequestDescriptor instead')
const GetUploadStatusRequest$json = {
  '1': 'GetUploadStatusRequest',
  '2': [
    {'1': 'client_request_id', '3': 1, '4': 1, '5': 9, '10': 'clientRequestId'},
  ],
};

/// Descriptor for `GetUploadStatusRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List getUploadStatusRequestDescriptor = $convert.base64Decode(
    'ChZHZXRVcGxvYWRTdGF0dXNSZXF1ZXN0EioKEWNsaWVudF9yZXF1ZXN0X2lkGAEgASgJUg9jbG'
    'llbnRSZXF1ZXN0SWQ=');

@$core.Deprecated('Use getUploadStatusResponseDescriptor instead')
const GetUploadStatusResponse$json = {
  '1': 'GetUploadStatusResponse',
  '2': [
    {'1': 'upload_id', '3': 1, '4': 1, '5': 9, '10': 'uploadId'},
    {'1': 'bytes_received', '3': 2, '4': 1, '5': 3, '10': 'bytesReceived'},
    {'1': 'next_offset', '3': 3, '4': 1, '5': 3, '10': 'nextOffset'},
    {'1': 'total_size_bytes', '3': 4, '4': 1, '5': 3, '10': 'totalSizeBytes'},
    {'1': 'completed', '3': 5, '4': 1, '5': 8, '10': 'completed'},
    {'1': 'message_id', '3': 6, '4': 1, '5': 9, '10': 'messageId'},
  ],
};

/// Descriptor for `GetUploadStatusResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List getUploadStatusResponseDescriptor = $convert.base64Decode(
    'ChdHZXRVcGxvYWRTdGF0dXNSZXNwb25zZRIbCgl1cGxvYWRfaWQYASABKAlSCHVwbG9hZElkEi'
    'UKDmJ5dGVzX3JlY2VpdmVkGAIgASgDUg1ieXRlc1JlY2VpdmVkEh8KC25leHRfb2Zmc2V0GAMg'
    'ASgDUgpuZXh0T2Zmc2V0EigKEHRvdGFsX3NpemVfYnl0ZXMYBCABKANSDnRvdGFsU2l6ZUJ5dG'
    'VzEhwKCWNvbXBsZXRlZBgFIAEoCFIJY29tcGxldGVkEh0KCm1lc3NhZ2VfaWQYBiABKAlSCW1l'
    'c3NhZ2VJZA==');

@$core.Deprecated('Use completeUploadRequestDescriptor instead')
const CompleteUploadRequest$json = {
  '1': 'CompleteUploadRequest',
  '2': [
    {'1': 'client_request_id', '3': 1, '4': 1, '5': 9, '10': 'clientRequestId'},
    {'1': 'duration_ms', '3': 2, '4': 1, '5': 3, '10': 'durationMs'},
    {'1': 'poster_jpeg', '3': 3, '4': 1, '5': 12, '10': 'posterJpeg'},
  ],
};

/// Descriptor for `CompleteUploadRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List completeUploadRequestDescriptor = $convert.base64Decode(
    'ChVDb21wbGV0ZVVwbG9hZFJlcXVlc3QSKgoRY2xpZW50X3JlcXVlc3RfaWQYASABKAlSD2NsaW'
    'VudFJlcXVlc3RJZBIfCgtkdXJhdGlvbl9tcxgCIAEoA1IKZHVyYXRpb25NcxIfCgtwb3N0ZXJf'
    'anBlZxgDIAEoDFIKcG9zdGVySnBlZw==');

@$core.Deprecated('Use completeUploadResponseDescriptor instead')
const CompleteUploadResponse$json = {
  '1': 'CompleteUploadResponse',
  '2': [
    {'1': 'message', '3': 1, '4': 1, '5': 11, '6': '.media.v1.Message', '10': 'message'},
  ],
};

/// Descriptor for `CompleteUploadResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List completeUploadResponseDescriptor = $convert.base64Decode(
    'ChZDb21wbGV0ZVVwbG9hZFJlc3BvbnNlEisKB21lc3NhZ2UYASABKAsyES5tZWRpYS52MS5NZX'
    'NzYWdlUgdtZXNzYWdl');

@$core.Deprecated('Use getMessageRequestDescriptor instead')
const GetMessageRequest$json = {
  '1': 'GetMessageRequest',
  '2': [
    {'1': 'message_id', '3': 1, '4': 1, '5': 9, '10': 'messageId'},
  ],
};

/// Descriptor for `GetMessageRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List getMessageRequestDescriptor = $convert.base64Decode(
    'ChFHZXRNZXNzYWdlUmVxdWVzdBIdCgptZXNzYWdlX2lkGAEgASgJUgltZXNzYWdlSWQ=');

@$core.Deprecated('Use getMessageResponseDescriptor instead')
const GetMessageResponse$json = {
  '1': 'GetMessageResponse',
  '2': [
    {'1': 'message', '3': 1, '4': 1, '5': 11, '6': '.media.v1.Message', '10': 'message'},
  ],
};

/// Descriptor for `GetMessageResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List getMessageResponseDescriptor = $convert.base64Decode(
    'ChJHZXRNZXNzYWdlUmVzcG9uc2USKwoHbWVzc2FnZRgBIAEoCzIRLm1lZGlhLnYxLk1lc3NhZ2'
    'VSB21lc3NhZ2U=');

@$core.Deprecated('Use listMessagesRequestDescriptor instead')
const ListMessagesRequest$json = {
  '1': 'ListMessagesRequest',
  '2': [
    {'1': 'after_sequence', '3': 1, '4': 1, '5': 3, '10': 'afterSequence'},
    {'1': 'limit', '3': 2, '4': 1, '5': 5, '10': 'limit'},
  ],
};

/// Descriptor for `ListMessagesRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List listMessagesRequestDescriptor = $convert.base64Decode(
    'ChNMaXN0TWVzc2FnZXNSZXF1ZXN0EiUKDmFmdGVyX3NlcXVlbmNlGAEgASgDUg1hZnRlclNlcX'
    'VlbmNlEhQKBWxpbWl0GAIgASgFUgVsaW1pdA==');

@$core.Deprecated('Use listMessagesResponseDescriptor instead')
const ListMessagesResponse$json = {
  '1': 'ListMessagesResponse',
  '2': [
    {'1': 'messages', '3': 1, '4': 3, '5': 11, '6': '.media.v1.Message', '10': 'messages'},
    {'1': 'latest_sequence', '3': 2, '4': 1, '5': 3, '10': 'latestSequence'},
  ],
};

/// Descriptor for `ListMessagesResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List listMessagesResponseDescriptor = $convert.base64Decode(
    'ChRMaXN0TWVzc2FnZXNSZXNwb25zZRItCghtZXNzYWdlcxgBIAMoCzIRLm1lZGlhLnYxLk1lc3'
    'NhZ2VSCG1lc3NhZ2VzEicKD2xhdGVzdF9zZXF1ZW5jZRgCIAEoA1IObGF0ZXN0U2VxdWVuY2U=');

@$core.Deprecated('Use watchMessagesRequestDescriptor instead')
const WatchMessagesRequest$json = {
  '1': 'WatchMessagesRequest',
  '2': [
    {'1': 'after_sequence', '3': 1, '4': 1, '5': 3, '10': 'afterSequence'},
    {'1': 'limit', '3': 2, '4': 1, '5': 5, '10': 'limit'},
  ],
};

/// Descriptor for `WatchMessagesRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List watchMessagesRequestDescriptor = $convert.base64Decode(
    'ChRXYXRjaE1lc3NhZ2VzUmVxdWVzdBIlCg5hZnRlcl9zZXF1ZW5jZRgBIAEoA1INYWZ0ZXJTZX'
    'F1ZW5jZRIUCgVsaW1pdBgCIAEoBVIFbGltaXQ=');

@$core.Deprecated('Use messageDescriptor instead')
const Message$json = {
  '1': 'Message',
  '2': [
    {'1': 'message_id', '3': 1, '4': 1, '5': 9, '10': 'messageId'},
    {'1': 'sequence', '3': 2, '4': 1, '5': 3, '10': 'sequence'},
    {'1': 'kind', '3': 3, '4': 1, '5': 14, '6': '.media.v1.MediaKind', '10': 'kind'},
    {'1': 'filename', '3': 4, '4': 1, '5': 9, '10': 'filename'},
    {'1': 'content_type', '3': 5, '4': 1, '5': 9, '10': 'contentType'},
    {'1': 'size_bytes', '3': 6, '4': 1, '5': 3, '10': 'sizeBytes'},
    {'1': 'duration_ms', '3': 7, '4': 1, '5': 3, '10': 'durationMs'},
    {'1': 'media_url', '3': 8, '4': 1, '5': 9, '10': 'mediaUrl'},
    {'1': 'poster_url', '3': 9, '4': 1, '5': 9, '10': 'posterUrl'},
    {'1': 'client_request_id', '3': 10, '4': 1, '5': 9, '10': 'clientRequestId'},
    {'1': 'created_unix_ms', '3': 11, '4': 1, '5': 3, '10': 'createdUnixMs'},
  ],
};

/// Descriptor for `Message`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List messageDescriptor = $convert.base64Decode(
    'CgdNZXNzYWdlEh0KCm1lc3NhZ2VfaWQYASABKAlSCW1lc3NhZ2VJZBIaCghzZXF1ZW5jZRgCIA'
    'EoA1IIc2VxdWVuY2USJwoEa2luZBgDIAEoDjITLm1lZGlhLnYxLk1lZGlhS2luZFIEa2luZBIa'
    'CghmaWxlbmFtZRgEIAEoCVIIZmlsZW5hbWUSIQoMY29udGVudF90eXBlGAUgASgJUgtjb250ZW'
    '50VHlwZRIdCgpzaXplX2J5dGVzGAYgASgDUglzaXplQnl0ZXMSHwoLZHVyYXRpb25fbXMYByAB'
    'KANSCmR1cmF0aW9uTXMSGwoJbWVkaWFfdXJsGAggASgJUghtZWRpYVVybBIdCgpwb3N0ZXJfdX'
    'JsGAkgASgJUglwb3N0ZXJVcmwSKgoRY2xpZW50X3JlcXVlc3RfaWQYCiABKAlSD2NsaWVudFJl'
    'cXVlc3RJZBImCg9jcmVhdGVkX3VuaXhfbXMYCyABKANSDWNyZWF0ZWRVbml4TXM=');

const $core.Map<$core.String, $core.dynamic> MediaServiceBase$json = {
  '1': 'MediaService',
  '2': [
    {'1': 'BeginUpload', '2': '.media.v1.BeginUploadRequest', '3': '.media.v1.BeginUploadResponse'},
    {'1': 'UploadChunk', '2': '.media.v1.UploadChunkRequest', '3': '.media.v1.UploadChunkResponse'},
    {'1': 'GetUploadStatus', '2': '.media.v1.GetUploadStatusRequest', '3': '.media.v1.GetUploadStatusResponse'},
    {'1': 'CompleteUpload', '2': '.media.v1.CompleteUploadRequest', '3': '.media.v1.CompleteUploadResponse'},
    {'1': 'GetMessage', '2': '.media.v1.GetMessageRequest', '3': '.media.v1.GetMessageResponse'},
    {'1': 'ListMessages', '2': '.media.v1.ListMessagesRequest', '3': '.media.v1.ListMessagesResponse'},
    {'1': 'WatchMessages', '2': '.media.v1.WatchMessagesRequest', '3': '.media.v1.Message', '6': true},
  ],
};

@$core.Deprecated('Use mediaServiceDescriptor instead')
const $core.Map<$core.String, $core.Map<$core.String, $core.dynamic>> MediaServiceBase$messageJson = {
  '.media.v1.BeginUploadRequest': BeginUploadRequest$json,
  '.media.v1.BeginUploadResponse': BeginUploadResponse$json,
  '.media.v1.UploadChunkRequest': UploadChunkRequest$json,
  '.media.v1.UploadChunkResponse': UploadChunkResponse$json,
  '.media.v1.GetUploadStatusRequest': GetUploadStatusRequest$json,
  '.media.v1.GetUploadStatusResponse': GetUploadStatusResponse$json,
  '.media.v1.CompleteUploadRequest': CompleteUploadRequest$json,
  '.media.v1.CompleteUploadResponse': CompleteUploadResponse$json,
  '.media.v1.Message': Message$json,
  '.media.v1.GetMessageRequest': GetMessageRequest$json,
  '.media.v1.GetMessageResponse': GetMessageResponse$json,
  '.media.v1.ListMessagesRequest': ListMessagesRequest$json,
  '.media.v1.ListMessagesResponse': ListMessagesResponse$json,
  '.media.v1.WatchMessagesRequest': WatchMessagesRequest$json,
};

/// Descriptor for `MediaService`. Decode as a `google.protobuf.ServiceDescriptorProto`.
final $typed_data.Uint8List mediaServiceDescriptor = $convert.base64Decode(
    'CgxNZWRpYVNlcnZpY2USSgoLQmVnaW5VcGxvYWQSHC5tZWRpYS52MS5CZWdpblVwbG9hZFJlcX'
    'Vlc3QaHS5tZWRpYS52MS5CZWdpblVwbG9hZFJlc3BvbnNlEkoKC1VwbG9hZENodW5rEhwubWVk'
    'aWEudjEuVXBsb2FkQ2h1bmtSZXF1ZXN0Gh0ubWVkaWEudjEuVXBsb2FkQ2h1bmtSZXNwb25zZR'
    'JWCg9HZXRVcGxvYWRTdGF0dXMSIC5tZWRpYS52MS5HZXRVcGxvYWRTdGF0dXNSZXF1ZXN0GiEu'
    'bWVkaWEudjEuR2V0VXBsb2FkU3RhdHVzUmVzcG9uc2USUwoOQ29tcGxldGVVcGxvYWQSHy5tZW'
    'RpYS52MS5Db21wbGV0ZVVwbG9hZFJlcXVlc3QaIC5tZWRpYS52MS5Db21wbGV0ZVVwbG9hZFJl'
    'c3BvbnNlEkcKCkdldE1lc3NhZ2USGy5tZWRpYS52MS5HZXRNZXNzYWdlUmVxdWVzdBocLm1lZG'
    'lhLnYxLkdldE1lc3NhZ2VSZXNwb25zZRJNCgxMaXN0TWVzc2FnZXMSHS5tZWRpYS52MS5MaXN0'
    'TWVzc2FnZXNSZXF1ZXN0Gh4ubWVkaWEudjEuTGlzdE1lc3NhZ2VzUmVzcG9uc2USRAoNV2F0Y2'
    'hNZXNzYWdlcxIeLm1lZGlhLnYxLldhdGNoTWVzc2FnZXNSZXF1ZXN0GhEubWVkaWEudjEuTWVz'
    'c2FnZTAB');

