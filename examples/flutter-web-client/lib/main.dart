import 'package:connectrpc/connect.dart' show Headers;
import 'package:connectrpc/protobuf.dart';
import 'package:connectrpc/protocol/connect.dart' as protocol;
import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'connect_http.dart';
import 'src/gen/demo/v1/demo.connect.client.dart';
import 'src/gen/demo/v1/demo.pb.dart';

/// Flutter web client talking Connect (JSON) to examples/quarkus-kotlin-server.
/// Same transport works for iOS/Android (swap `package:connectrpc/web.dart` for
/// `package:connectrpc/io.dart` or `http2.dart` on mobile).
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

  DemoServiceClient _client() {
    final transport = protocol.Transport(
      baseUrl: _baseUrlController.text.trim(),
      codec: const JsonCodec(),
      httpClient: createDemoHttpClient(),
    );
    return DemoServiceClient(transport);
  }

  Headers _authHeaders() {
    final h = Headers();
    h.add('authorization', 'Bearer $_token');
    return h;
  }

  Future<void> _echo() async {
    setState(() => _busy = true);
    try {
      _seq += 1;
      final res = await _client().echo(
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
      final res = await _client().whoAmI(
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
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: _busy ? null : _echo,
                    child: const Text('Echo'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton(
                    onPressed: _busy ? null : _whoAmI,
                    child: const Text('WhoAmI'),
                  ),
                ),
              ],
            ),
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
