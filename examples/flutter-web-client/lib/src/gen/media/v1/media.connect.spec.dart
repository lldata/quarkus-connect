//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
//

import "package:connectrpc/connect.dart" as connect;
import "media.pb.dart" as mediav1media;

/// Chunked unary upload + message listing (polling). No client streaming required.
/// Same @GrpcService is exposed as gRPC + Connect + gRPC-Web.
abstract final class MediaService {
  /// Fully-qualified name of the MediaService service.
  static const name = 'media.v1.MediaService';

  /// Create or resume an upload keyed by client_request_id (idempotent).
  static const beginUpload = connect.Spec(
    '/$name/BeginUpload',
    connect.StreamType.unary,
    mediav1media.BeginUploadRequest.new,
    mediav1media.BeginUploadResponse.new,
  );

  /// Append one chunk. Re-sending the same chunk_index with identical bytes is a no-op.
  static const uploadChunk = connect.Spec(
    '/$name/UploadChunk',
    connect.StreamType.unary,
    mediav1media.UploadChunkRequest.new,
    mediav1media.UploadChunkResponse.new,
  );

  /// Query resume offset after drop / app restart.
  static const getUploadStatus = connect.Spec(
    '/$name/GetUploadStatus',
    connect.StreamType.unary,
    mediav1media.GetUploadStatusRequest.new,
    mediav1media.GetUploadStatusResponse.new,
  );

  /// Finalize into a Message. Idempotent for the same client_request_id.
  static const completeUpload = connect.Spec(
    '/$name/CompleteUpload',
    connect.StreamType.unary,
    mediav1media.CompleteUploadRequest.new,
    mediav1media.CompleteUploadResponse.new,
  );

  static const getMessage = connect.Spec(
    '/$name/GetMessage',
    connect.StreamType.unary,
    mediav1media.GetMessageRequest.new,
    mediav1media.GetMessageResponse.new,
  );

  /// Polling live updates: return messages newer than after_sequence.
  static const listMessages = connect.Spec(
    '/$name/ListMessages',
    connect.StreamType.unary,
    mediav1media.ListMessagesRequest.new,
    mediav1media.ListMessagesResponse.new,
  );
}
