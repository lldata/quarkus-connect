//
//  Generated code. Do not modify.
//  source: media/v1/media.proto
//
// @dart = 2.12

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_final_fields
// ignore_for_file: unnecessary_import, unnecessary_this, unused_import

import 'dart:core' as $core;

import 'package:protobuf/protobuf.dart' as $pb;

class MediaKind extends $pb.ProtobufEnum {
  static const MediaKind MEDIA_KIND_UNSPECIFIED = MediaKind._(0, _omitEnumNames ? '' : 'MEDIA_KIND_UNSPECIFIED');
  static const MediaKind MEDIA_KIND_VIDEO = MediaKind._(1, _omitEnumNames ? '' : 'MEDIA_KIND_VIDEO');
  static const MediaKind MEDIA_KIND_AUDIO = MediaKind._(2, _omitEnumNames ? '' : 'MEDIA_KIND_AUDIO');

  static const $core.List<MediaKind> values = <MediaKind> [
    MEDIA_KIND_UNSPECIFIED,
    MEDIA_KIND_VIDEO,
    MEDIA_KIND_AUDIO,
  ];

  static final $core.Map<$core.int, MediaKind> _byValue = $pb.ProtobufEnum.initByValue(values);
  static MediaKind? valueOf($core.int value) => _byValue[value];

  const MediaKind._($core.int v, $core.String n) : super(v, n);
}


const _omitEnumNames = $core.bool.fromEnvironment('protobuf.omit_enum_names');
