import 'dart:convert';
import 'dart:typed_data';

import 'package:connectrpc/connect.dart' show Headers;
import 'package:connectrpc/protobuf.dart';
import 'package:connectrpc/protocol/connect.dart' as protocol;
import 'package:fixnum/fixnum.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import 'connect_http.dart';
import 'src/gen/media/v1/media.connect.client.dart';
import 'src/gen/media/v1/media.pb.dart';

/// Chunked unary upload with resume via persisted [clientRequestId].
class UploadSession {
  UploadSession({
    required this.baseUrl,
    required this.token,
    this.chunkSize = 64 * 1024,
  });

  final String baseUrl;
  final String token;
  final int chunkSize;

  MediaServiceClient _client() {
    final transport = protocol.Transport(
      baseUrl: baseUrl,
      codec: const JsonCodec(),
      httpClient: createDemoHttpClient(),
    );
    return MediaServiceClient(transport);
  }

  Headers _auth() {
    final h = Headers();
    h.add('authorization', 'Bearer $token');
    return h;
  }

  static const _prefsKey = 'media_upload_v1';

  Future<String> _ensureClientRequestId() async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getString('$_prefsKey.clientRequestId');
    if (existing != null && existing.isNotEmpty) return existing;
    final id = const Uuid().v4();
    await prefs.setString('$_prefsKey.clientRequestId', id);
    return id;
  }

  Future<void> clearPersisted() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('$_prefsKey.clientRequestId');
    await prefs.remove('$_prefsKey.meta');
  }

  Future<Message> upload({
    required Uint8List bytes,
    required String filename,
    required String contentType,
    required MediaKind kind,
    int durationMs = 0,
    void Function(int sent, int total)? onProgress,
  }) async {
    final clientRequestId = await _ensureClientRequestId();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      '$_prefsKey.meta',
      jsonEncode({
        'filename': filename,
        'contentType': contentType,
        'totalSizeBytes': bytes.length,
        'kind': kind.name,
      }),
    );

    final begin = await _client().beginUpload(
      BeginUploadRequest(
        clientRequestId: clientRequestId,
        filename: filename,
        contentType: contentType,
        totalSizeBytes: Int64(bytes.length),
        kind: kind,
      ),
      headers: _auth(),
    );

    var offset = begin.nextOffset.toInt();
    onProgress?.call(offset, bytes.length);

    // Derive chunk index from offset for resume.
    var chunkIndex = offset ~/ chunkSize;
    while (offset < bytes.length) {
      final end = (offset + chunkSize).clamp(0, bytes.length);
      final slice = bytes.sublist(offset, end);
      final res = await _client().uploadChunk(
        UploadChunkRequest(
          clientRequestId: clientRequestId,
          offset: Int64(offset),
          chunkIndex: chunkIndex,
          data: slice,
        ),
        headers: _auth(),
      );
      offset = res.nextOffset.toInt();
      chunkIndex++;
      onProgress?.call(offset, bytes.length);
    }

    final complete = await _client().completeUpload(
      CompleteUploadRequest(
        clientRequestId: clientRequestId,
        durationMs: Int64(durationMs),
      ),
      headers: _auth(),
    );
    await clearPersisted();
    return complete.message;
  }

  Future<ListMessagesResponse> listMessages({Int64? afterSequence}) {
    return _client().listMessages(
      ListMessagesRequest(
        afterSequence: afterSequence ?? Int64.ZERO,
        limit: 50,
      ),
      headers: _auth(),
    );
  }

  /// Connect server-streaming catch-up: yields messages then completes.
  /// Caller advances [afterSequence] and reconnects for continuous live updates.
  Stream<Message> watchMessages({Int64? afterSequence, int limit = 50}) {
    return _client().watchMessages(
      WatchMessagesRequest(
        afterSequence: afterSequence ?? Int64.ZERO,
        limit: limit,
      ),
      headers: _auth(),
    );
  }
}
