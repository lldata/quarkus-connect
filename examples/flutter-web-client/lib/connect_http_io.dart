import 'dart:io' as io;

import 'package:connectrpc/connect.dart' show HttpClient;
import 'package:connectrpc/io.dart' as connect_io;

HttpClient createDemoHttpClient() => connect_io.createHttpClient(io.HttpClient());
