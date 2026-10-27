import 'dart:typed_data';

import 'package:connectrpc/connect.dart';
import 'package:connectrpc/protobuf.dart';
import 'package:connectrpc/protocol/connect.dart' as protocol;
import 'package:file_picker/file_picker.dart';
import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'connect_error_format.dart';
import 'connect_http.dart';
import 'idle_timeout.dart';
import 'src/gen/demo/v1/demo.connect.client.dart';
import 'src/gen/demo/v1/demo.pb.dart';
import 'src/gen/media/v1/media.pb.dart';
import 'token_provider.dart';
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
  final _tokens = TokenProvider('demo-user');
  final _idleTimeout = IdleAwareTimeout();
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
        token: _tokens.token,
      );

  Future<void> _echo() async {
    setState(() => _busy = true);
    try {
      _seq += 1;
      final cold = _idleTimeout.isColdStart;
      final timeout = _idleTimeout.nextTimeout;
      final res = await _demoClient().echo(
        EchoRequest(message: _messageController.text, clientSeq: Int64(_seq)),
        headers: _tokens.headers(),
        signal: _idleTimeout.signal(),
      );
      setState(() {
        _log =
            'Echo OK (timeout=${timeout.inSeconds}s${cold ? ', cold-start' : ''})\n'
            'message=${res.message}\nclientSeq=${res.clientSeq}\nserverTimeUnixMs=${res.serverTimeUnixMs}';
      });
    } catch (e) {
      setState(() => _log = formatConnectError(e, label: 'Echo error'));
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _whoAmI({Headers? overrideHeaders}) async {
    setState(() => _busy = true);
    try {
      final cold = _idleTimeout.isColdStart;
      final timeout = _idleTimeout.nextTimeout;
      final res = await _demoClient().whoAmI(
        WhoAmIRequest(),
        headers: _tokens.headers(overrideHeaders: overrideHeaders),
        signal: _idleTimeout.signal(),
      );
      setState(() {
        _log =
            'WhoAmI OK (timeout=${timeout.inSeconds}s${cold ? ', cold-start' : ''})\n'
            'subject=${res.subject}';
      });
    } catch (e) {
      // Guest WhoAmI → PERMISSION_DENIED with trailer retry-after-seconds (see server).
      final extra = StringBuffer(formatConnectError(e, label: 'WhoAmI error'));
      if (e is ConnectException) {
        final retry = e.metadata['retry-after-seconds'];
        if (retry != null) {
          extra.writeln('\ntrailer retry-after-seconds=$retry');
        }
      }
      setState(() => _log = extra.toString());
    } finally {
      setState(() => _busy = false);
    }
  }

  /// Forces Bearer demo-guest so trailers are visible even if the dropdown is
  /// still on demo-user (per-call Authorization override via TokenProvider).
  Future<void> _whoAmIAsGuest() {
    final override = Headers()..add('authorization', 'Bearer demo-guest');
    return _whoAmI(overrideHeaders: override);
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
            InputDecorator(
              decoration: const InputDecoration(
                labelText: 'Bearer token (TokenProvider)',
                border: OutlineInputBorder(),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  isExpanded: true,
                  value: _tokens.token,
                  items: const [
                    DropdownMenuItem(value: 'demo-user', child: Text('demo-user (full)')),
                    DropdownMenuItem(value: 'demo-guest', child: Text('demo-guest (Echo only)')),
                    DropdownMenuItem(value: 'bad', child: Text('bad (UNAUTHENTICATED)')),
                  ],
                  onChanged: (v) => setState(() => _tokens.token = v ?? 'demo-user'),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton(onPressed: _busy ? null : _echo, child: const Text('Echo')),
                OutlinedButton(onPressed: _busy ? null : () => _whoAmI(), child: const Text('WhoAmI')),
                OutlinedButton(
                  onPressed: _busy ? null : _whoAmIAsGuest,
                  child: const Text('WhoAmI as guest (trailers)'),
                ),
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
              'Live cursor after_sequence=$_pollAfter · next RPC timeout '
              '${_idleTimeout.nextTimeout.inSeconds}s'
              '${_idleTimeout.isColdStart ? ' (cold-start)' : ''}',
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
