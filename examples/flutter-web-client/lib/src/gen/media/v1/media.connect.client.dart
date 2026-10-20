//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
//

import "package:connectrpc/connect.dart" as connect;
import "media.pb.dart" as mediav1media;
import "media.connect.spec.dart" as specs;

/// Chunked unary upload + message listing (polling). No client streaming required.
/// Same @GrpcService is exposed as gRPC + Connect + gRPC-Web.
extension type MediaServiceClient (connect.Transport _transport) {
  /// Create or resume an upload keyed by client_request_id (idempotent).
  Future<mediav1media.BeginUploadResponse> beginUpload(
    mediav1media.BeginUploadRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.beginUpload,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  /// Append one chunk. Re-sending the same chunk_index with identical bytes is a no-op.
  Future<mediav1media.UploadChunkResponse> uploadChunk(
    mediav1media.UploadChunkRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.uploadChunk,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  /// Query resume offset after drop / app restart.
  Future<mediav1media.GetUploadStatusResponse> getUploadStatus(
    mediav1media.GetUploadStatusRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.getUploadStatus,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  /// Finalize into a Message. Idempotent for the same client_request_id.
  Future<mediav1media.CompleteUploadResponse> completeUpload(
    mediav1media.CompleteUploadRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.completeUpload,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  Future<mediav1media.GetMessageResponse> getMessage(
    mediav1media.GetMessageRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.getMessage,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  /// Polling live updates: return messages newer than after_sequence.
  Future<mediav1media.ListMessagesResponse> listMessages(
    mediav1media.ListMessagesRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).unary(
      specs.MediaService.listMessages,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }

  /// Connect server streaming (`application/connect+json|proto`): catch-up then complete.
  /// Client reconnects with the last seen sequence for continuous live updates.
  Stream<mediav1media.Message> watchMessages(
    mediav1media.WatchMessagesRequest input, {
    connect.Headers? headers,
    connect.AbortSignal? signal,
    Function(connect.Headers)? onHeader,
    Function(connect.Headers)? onTrailer,
  }) {
    return connect.Client(_transport).server(
      specs.MediaService.watchMessages,
      input,
      signal: signal,
      headers: headers,
      onHeader: onHeader,
      onTrailer: onTrailer,
    );
  }
}
