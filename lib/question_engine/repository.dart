import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'question_engine.dart';

/// Native local storage, in a dedicated app-private directory supplied by the
/// host. JSON pack data only; attempts and existing Kotlin books remain separate.
class QuestionPackRepository {
  QuestionPackRepository(
    this.directory, {
    this.codec = const QuestionPackCodec(),
  });
  final Directory directory;
  final QuestionPackCodec codec;
  static final Map<String, Future<void>> _queues = {};

  File _file(String packId) {
    if (RegExp(
          r'^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$',
        ).matchAsPrefix(packId)?.end !=
        packId.length) {
      throw ArgumentError.value(packId, 'packId', 'Invalid ID');
    }
    // Hex UTF-8 filenames avoid traversal, case collisions and Windows ':' rules.
    final name =
        utf8
            .encode(packId)
            .map((b) => b.toRadixString(16).padLeft(2, '0'))
            .join();
    final split =
        name.length > 32
            ? '${name.substring(0, 32)}${Platform.pathSeparator}${name.substring(32)}'
            : name;
    return File('${directory.path}${Platform.pathSeparator}$split.cpq.json');
  }

  Future<QuestionPack?> load(String packId) => _serialized(() => _load(packId));
  Future<QuestionPack?> _load(String packId) async {
    final file = _file(packId);
    if (await FileSystemEntity.type(file.path, followLinks: false) ==
        FileSystemEntityType.link) {
      throw StateError('Symlink target rejected');
    }
    if (!await file.exists()) return null;
    final pack = codec.decode(await _readBounded(file));
    if (pack.packId != packId) throw StateError('Stored pack ID mismatch');
    return pack;
  }

  Future<String> _readBounded(File file) async {
    final bytes = <int>[];
    await for (final chunk in file.openRead()) {
      if (bytes.length + chunk.length > codec.maxBytes) {
        throw PackValidationException([
          const ValidationIssue(r'$', 'size_limit', 'JSONの容量上限を超えています'),
        ]);
      }
      bytes.addAll(chunk);
    }
    try {
      return utf8.decode(bytes);
    } on FormatException {
      throw PackValidationException([
        const ValidationIssue(r'$', 'invalid_utf8', 'UTF-8で保存してください'),
      ]);
    }
  }

  /// Preview never saves; UI must explicitly call save after user confirmation.
  Future<QuestionPack> preview(File source) async =>
      codec.decode(await _readBounded(source));

  /// New IDs only by default. Replacing requires the expected stored revision and
  /// a strictly newer incoming revision; duplicate imports never silently mutate.
  Future<void> save(
    QuestionPack pack, {
    int? expectedRevision,
  }) => _serialized(() async {
    final source = codec.encode(pack);
    await directory.create(recursive: true);
    // Lock a dedicated file across repository instances and native processes.
    final lock = await File(
      '${directory.path}${Platform.pathSeparator}.write.lock',
    ).open(mode: FileMode.append);
    Directory? staging;
    try {
      await lock.lock(FileLock.blockingExclusive);
      final existing = await _load(pack.packId);
      if (existing == null && expectedRevision != null) {
        throw StateError('Pack no longer exists');
      }
      if (existing != null &&
          (expectedRevision != existing.revision ||
              pack.revision <= existing.revision)) {
        throw StateError('Duplicate pack or revision conflict');
      }
      final target = _file(pack.packId);
      await target.parent.create(recursive: true);
      if (await FileSystemEntity.type(target.path, followLinks: false) ==
          FileSystemEntityType.link) {
        throw StateError('Symlink target rejected');
      }
      staging = await directory.createTemp('.pack-');
      final temp = File('${staging.path}${Platform.pathSeparator}pack.tmp');
      await temp.writeAsString(source, encoding: utf8, flush: true);
      // Same filesystem atomic rename: old file survives failed validation/write.
      await temp.rename(target.path);
    } finally {
      if (staging != null && await staging.exists()) {
        await staging.delete(recursive: true);
      }
      await lock.close();
    }
  });
  Future<T> _serialized<T>(Future<T> Function() action) async {
    await directory.create(recursive: true);
    final key = await directory.resolveSymbolicLinks();
    final result = (_queues[key] ?? Future<void>.value()).then((_) => action());
    final tail = result.then<void>(
      (_) {},
      onError: (Object e, StackTrace s) {},
    );
    _queues[key] = tail;
    try {
      return await result;
    } finally {
      if (identical(_queues[key], tail)) _queues.remove(key);
    }
  }
}
