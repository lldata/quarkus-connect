import 'dart:typed_data';

import 'package:connectrpc/connect.dart' show Headers;
import 'package:connectrpc/protobuf.dart';
import 'package:connectrpc/protocol/connect.dart' as protocol;
import 'package:file_picker/file_picker.dart';
import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'connect_http.dart';
import 'src/gen/demo/v1/demo.connect.client.dart';
import 'src/gen/demo/v1/demo.pb.dart';
import 'src/gen/media/v1/media.pb.dart';
import 'upload_session.dart';

void main() {
  runApp(const DemoApp());
}

class DemoApp extends StatelessWidget {
  const DemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'quarkus-connect demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1B4D3E)),
        useMaterial3: true,
      ),
      home: const DemoHomePage(),
    );
  }
}

class DemoHomePage extends StatefulWidget {
  const DemoHomePage({super.key});

  @override
  State<DemoHomePage> createState() => _DemoHomePageState();
}

class _DemoHomePageState extends State<DemoHomePage> {
  final _baseUrlController = TextEditingController(text: 'http://localhost:8080');
  final _messageController = TextEditingController(text: 'hello from flutter');
  String _token = 'demo-user';
  String _log = '';
  int _seq = 0;
  bool _busy = false;
  double _uploadProgress = 0;
  Int64 _pollAfter = Int64.ZERO;

  DemoServiceClient _demoClient() {
    final transport = protocol.Transport(
      baseUrl: _baseUrlController.text.trim(),
      codec: const JsonCodec(),
      httpClient: createDemoHttpClient(),
    );
    return DemoServiceClient(transport);
  }

  UploadSession _uploadSession() => UploadSession(
        baseUrl: _baseUrlController.text.trim(),
        token: _token,
      );

  Headers _authHeaders() {
    final h = Headers();
    h.add('authorization', 'Bearer $_token');
    return h;
  }

  Future<void> _echo() async {
    setState(() => _busy = true);
    try {
      _seq += 1;
      final res = await _demoClient().echo(
        EchoRequest(message: _messageController.text, clientSeq: Int64(_seq)),
        headers: _authHeaders(),
      );
      setState(() {
        _log =
            'Echo OK\nmessage=${res.message}\nclientSeq=${res.clientSeq}\nserverTimeUnixMs=${res.serverTimeUnixMs}';
      });
    } catch (e) {
      setState(() => _log = 'Echo error\n$e');
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _whoAmI() async {
    setState(() => _busy = true);
    try {
      final res = await _demoClient().whoAmI(
        WhoAmIRequest(),
        headers: _authHeaders(),
      );
      setState(() => _log = 'WhoAmI OK\nsubject=${res.subject}');
    } catch (e) {
      setState(() => _log = 'WhoAmI error\n$e');
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _pickAndUpload() async {
    final result = await FilePicker.pickFiles(withData: true);
    if (result == null || result.files.isEmpty) return;
    final file = result.files.first;
    final bytes = file.bytes;
    if (bytes == null) {
      setState(() => _log = 'Could not read file bytes (web requires withData)');
      return;
    }
    setState(() {
      _busy = true;
      _uploadProgress = 0;
    });
    try {
      final msg = await _uploadSession().upload(
        bytes: Uint8List.fromList(bytes),
        filename: file.name,
        contentType: file.extension == 'mp3' || file.extension == 'm4a'
            ? 'audio/mpeg'
            : 'application/octet-stream',
        kind: file.extension == 'mp3' || file.extension == 'm4a'
            ? MediaKind.MEDIA_KIND_AUDIO
            : MediaKind.MEDIA_KIND_VIDEO,
        durationMs: 0,
        onProgress: (sent, total) {
          setState(() => _uploadProgress = total == 0 ? 0 : sent / total);
        },
      );
      setState(() {
        _log =
            'Upload OK\nid=${msg.messageId}\nsize=${msg.sizeBytes}\nmedia=${msg.mediaUrl}\nposter=${msg.posterUrl}\n'
            'Resume tip: client_request_id is cleared after complete; interrupt mid-upload and reload to resume.';
      });
    } catch (e) {
      setState(() => _log = 'Upload error\n$e\n(Reload the page to resume from GetUploadStatus.)');
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _pollMessages() async {
    setState(() => _busy = true);
    try {
      final res = await _uploadSession().listMessages(afterSequence: _pollAfter);
      _pollAfter = res.latestSequence;
      final buf = StringBuffer('ListMessages (polling)\nlatest=${res.latestSequence}\n');
      for (final m in res.messages) {
        buf.writeln('- ${m.messageId} ${m.filename} ${m.sizeBytes}B seq=${m.sequence}');
      }
      setState(() => _log = buf.toString());
    } catch (e) {
      setState(() => _log = 'ListMessages error\n$e');
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _watchMessages() async {
    setState(() => _busy = true);
    final buf = StringBuffer('WatchMessages (Connect stream, up to 3 reconnects)\n');
    var total = 0;
    try {
      for (var attempt = 1; attempt <= 3; attempt++) {
        buf.writeln('— attempt $attempt after=$_pollAfter');
        var count = 0;
        try {
          await for (final m in _uploadSession().watchMessages(afterSequence: _pollAfter)) {
            count++;
            total++;
            if (m.sequence > _pollAfter) {
              _pollAfter = m.sequence;
            }
            buf.writeln('- ${m.messageId} ${m.filename} ${m.sizeBytes}B seq=${m.sequence}');
          }
          buf.writeln('stream completed ($count messages this attempt)');
          // Catch-up streams end when backlog is drained; stop unless empty on first try.
          if (count > 0 || attempt == 1) break;
        } catch (e) {
          buf.writeln('attempt $attempt error: $e');
          if (attempt == 3) rethrow;
          await Future<void>.delayed(Duration(milliseconds: 200 * attempt));
        }
      }
      buf.writeln('done; next after=$_pollAfter (total $total)');
      setState(() => _log = buf.toString());
    } catch (e) {
      buf.writeln('WatchMessages failed\n$e');
      setState(() => _log = buf.toString());
    } finally {
      setState(() => _busy = false);
    }
  }

  void _clearLog() => setState(() => _log = '');

  @override
  void dispose() {
    _baseUrlController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Connect → quarkus-connect')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _baseUrlController,
              decoration: const InputDecoration(
                labelText: 'Base URL',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _messageController,
              decoration: const InputDecoration(
                labelText: 'Echo message',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _token,
              decoration: const InputDecoration(
                labelText: 'Bearer token',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'demo-user', child: Text('demo-user (full)')),
                DropdownMenuItem(value: 'demo-guest', child: Text('demo-guest (Echo only)')),
                DropdownMenuItem(value: 'bad', child: Text('bad (UNAUTHENTICATED)')),
              ],
              onChanged: (v) => setState(() => _token = v ?? 'demo-user'),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton(onPressed: _busy ? null : _echo, child: const Text('Echo')),
                OutlinedButton(onPressed: _busy ? null : _whoAmI, child: const Text('WhoAmI')),
                FilledButton.tonal(
                  onPressed: _busy ? null : _pickAndUpload,
                  child: const Text('Upload file (chunked)'),
                ),
                OutlinedButton(
                  onPressed: _busy ? null : _pollMessages,
                  child: const Text('Poll messages'),
                ),
                FilledButton.tonal(
                  onPressed: _busy ? null : _watchMessages,
                  child: const Text('Watch stream'),
                ),
                TextButton(onPressed: _busy ? null : _clearLog, child: const Text('Clear')),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Live cursor after_sequence=$_pollAfter',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            if (_busy && _uploadProgress > 0) ...[
              const SizedBox(height: 12),
              LinearProgressIndicator(value: _uploadProgress),
            ],
            const SizedBox(height: 16),
            Expanded(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  border: Border.all(color: Theme.of(context).dividerColor),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(12),
                  child: SelectableText(
                    _log.isEmpty ? 'Responses appear here.' : _log,
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
