import 'package:flutter/material.dart';
import '../bridge.dart';
import '../validation.dart';
import '../widgets/app_ui.dart';

class PurposeManagementScreen extends StatefulWidget {
  const PurposeManagementScreen({super.key, required this.bridge});
  final NanishiniBridge bridge;
  @override
  State<PurposeManagementScreen> createState() =>
      _PurposeManagementScreenState();
}

class _PurposeManagementScreenState extends State<PurposeManagementScreen> {
  AppSnapshot? snapshot;
  Object? error;
  bool busy = false;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final value = await widget.bridge.loadSnapshot();
      if (mounted) {
        setState(() {
          snapshot = value;
          error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await action();
      await _load();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> _edit([PurposeItem? item]) async {
    final data = snapshot;
    if (data == null) return;
    final draft = await showDialog<PurposeItemDraft>(
      context: context,
      builder:
          (_) => PurposeEditorDialog(
            categories: data.purposeCategories,
            initial: item,
          ),
    );
    if (draft != null) {
      await _run(
        () =>
            item == null
                ? widget.bridge.addPurposeItem(draft)
                : widget.bridge.updatePurposeItem(
                  item.copyWith(
                    label: draft.label,
                    categoryId: draft.categoryId,
                  ),
                ),
      );
    }
  }

  Future<void> _delete(PurposeItem item) async {
    final ok = await _confirmDelete(context, '「${item.label}」を削除しますか？');
    if (ok) await _run(() => widget.bridge.deletePurposeItem(item.id));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('利用目的管理')),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: busy || snapshot == null ? null : () => _edit(),
      icon: const Icon(Icons.add_rounded),
      label: const Text('追加'),
    ),
    body: _ManagementBody(
      error: error,
      loading: snapshot == null,
      onRetry: _load,
      child:
          snapshot == null
              ? const SizedBox()
              : ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 92),
                children: [
                  AppSectionCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        for (
                          var i = 0;
                          i < snapshot!.purposeItems.length;
                          i++
                        ) ...[
                          _EditableTile(
                            title: snapshot!.purposeItems[i].label,
                            subtitle: snapshot!.categoryNameById(
                              snapshot!.purposeItems[i].categoryId,
                            ),
                            locked: snapshot!.purposeItems[i].isDefault,
                            onEdit: () => _edit(snapshot!.purposeItems[i]),
                            onDelete: () => _delete(snapshot!.purposeItems[i]),
                          ),
                          if (i < snapshot!.purposeItems.length - 1)
                            const Divider(height: 1, indent: 16),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
    ),
  );
}

class ActivityManagementScreen extends StatefulWidget {
  const ActivityManagementScreen({super.key, required this.bridge});
  final NanishiniBridge bridge;
  @override
  State<ActivityManagementScreen> createState() =>
      _ActivityManagementScreenState();
}

class _ActivityManagementScreenState extends State<ActivityManagementScreen> {
  AppSnapshot? snapshot;
  Object? error;
  bool busy = false;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final value = await widget.bridge.loadSnapshot();
      if (mounted) {
        setState(() {
          snapshot = value;
          error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await action();
      await _load();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> _edit([ActivityCategory? item]) async {
    final draft = await showDialog<ActivityCategoryDraft>(
      context: context,
      builder: (_) => ActivityEditorDialog(initial: item),
    );
    if (draft != null) {
      await _run(
        () =>
            item == null
                ? widget.bridge.addActivityCategory(draft)
                : widget.bridge.updateActivityCategory(
                  item.copyWith(name: draft.name),
                ),
      );
    }
  }

  Future<void> _delete(ActivityCategory item) async {
    final ok = await _confirmDelete(context, '「${item.name}」を削除しますか？');
    if (ok) await _run(() => widget.bridge.deleteActivityCategory(item.id));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('行動ジャンル管理')),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: busy || snapshot == null ? null : () => _edit(),
      icon: const Icon(Icons.add_rounded),
      label: const Text('追加'),
    ),
    body: _ManagementBody(
      error: error,
      loading: snapshot == null,
      onRetry: _load,
      child:
          snapshot == null
              ? const SizedBox()
              : ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 92),
                children: [
                  AppSectionCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        for (
                          var i = 0;
                          i < snapshot!.activityCategories.length;
                          i++
                        ) ...[
                          _EditableTile(
                            title: snapshot!.activityCategories[i].name,
                            subtitle:
                                snapshot!.activityCategories[i].isDefault
                                    ? '初期ジャンル'
                                    : 'ユーザー追加',
                            locked: snapshot!.activityCategories[i].isDefault,
                            onEdit:
                                () => _edit(snapshot!.activityCategories[i]),
                            onDelete:
                                () => _delete(snapshot!.activityCategories[i]),
                          ),
                          if (i < snapshot!.activityCategories.length - 1)
                            const Divider(height: 1, indent: 16),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
    ),
  );
}

class _ManagementBody extends StatelessWidget {
  const _ManagementBody({
    required this.error,
    required this.loading,
    required this.onRetry,
    required this.child,
  });
  final Object? error;
  final bool loading;
  final VoidCallback onRetry;
  final Widget child;
  @override
  Widget build(BuildContext context) {
    if (error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('データを読み込めませんでした'),
            OutlinedButton(onPressed: onRetry, child: const Text('再試行')),
          ],
        ),
      );
    }
    if (loading) return const Center(child: CircularProgressIndicator());
    return child;
  }
}

class _EditableTile extends StatelessWidget {
  const _EditableTile({
    required this.title,
    required this.subtitle,
    required this.locked,
    required this.onEdit,
    required this.onDelete,
  });
  final String title, subtitle;
  final bool locked;
  final VoidCallback onEdit, onDelete;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: const EdgeInsets.fromLTRB(16, 7, 8, 7),
    title: Text(title),
    subtitle: Text(subtitle),
    trailing:
        locked
            ? const Tooltip(
              message: '初期項目は変更できません',
              child: Icon(Icons.lock_outline_rounded, color: Color(0xFF8A94A8)),
            )
            : Wrap(
              spacing: 0,
              children: [
                IconButton(
                  tooltip: '編集',
                  onPressed: onEdit,
                  icon: const Icon(Icons.edit_outlined),
                ),
                IconButton(
                  tooltip: '削除',
                  onPressed: onDelete,
                  icon: const Icon(Icons.delete_outline_rounded),
                ),
              ],
            ),
  );
}

Future<bool> _confirmDelete(BuildContext context, String title) async =>
    await showDialog<bool>(
      context: context,
      builder:
          (_) => AlertDialog(
            title: Text(title),
            content: const Text('この操作は元に戻せません。'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('キャンセル'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('削除する'),
              ),
            ],
          ),
    ) ??
    false;

class PurposeEditorDialog extends StatefulWidget {
  const PurposeEditorDialog({
    super.key,
    required this.categories,
    this.initial,
  });
  final List<PurposeCategory> categories;
  final PurposeItem? initial;
  @override
  State<PurposeEditorDialog> createState() => _PurposeEditorDialogState();
}

class _PurposeEditorDialogState extends State<PurposeEditorDialog> {
  final formKey = GlobalKey<FormState>();
  late String label;
  String? category;
  @override
  void initState() {
    super.initState();
    label = widget.initial?.label ?? '';
    category =
        widget.initial?.categoryId ??
        (widget.categories.isEmpty ? null : widget.categories.first.id);
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(widget.initial == null ? '利用目的を追加' : '利用目的を編集'),
    content: Form(
      key: formKey,
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              initialValue: label,
              onChanged: (value) => label = value,
              decoration: const InputDecoration(labelText: '利用目的名'),
              maxLength: 60,
              validator: (v) => validatePurposeItemLabel(v ?? '').errorMessage,
            ),
            DropdownButtonFormField<String>(
              initialValue: category,
              decoration: const InputDecoration(labelText: '所属カテゴリ'),
              items:
                  widget.categories
                      .map(
                        (e) =>
                            DropdownMenuItem(value: e.id, child: Text(e.name)),
                      )
                      .toList(),
              onChanged: (v) => setState(() => category = v),
              validator: (v) => v == null ? 'カテゴリを選択してください' : null,
            ),
          ],
        ),
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('キャンセル'),
      ),
      FilledButton(
        onPressed: () {
          if (formKey.currentState?.validate() != true) return;
          Navigator.pop(
            context,
            PurposeItemDraft(label: label, categoryId: category!),
          );
        },
        child: const Text('保存'),
      ),
    ],
  );
}

class ActivityEditorDialog extends StatefulWidget {
  const ActivityEditorDialog({super.key, this.initial});
  final ActivityCategory? initial;
  @override
  State<ActivityEditorDialog> createState() => _ActivityEditorDialogState();
}

class _ActivityEditorDialogState extends State<ActivityEditorDialog> {
  final formKey = GlobalKey<FormState>();
  late String name;
  @override
  void initState() {
    super.initState();
    name = widget.initial?.name ?? '';
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(widget.initial == null ? 'ジャンルを追加' : 'ジャンルを編集'),
    content: Form(
      key: formKey,
      child: TextFormField(
        initialValue: name,
        onChanged: (value) => name = value,
        decoration: const InputDecoration(labelText: 'ジャンル名'),
        maxLength: 40,
        validator: (v) => validateActivityCategoryName(v ?? '').errorMessage,
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('キャンセル'),
      ),
      FilledButton(
        onPressed: () {
          if (formKey.currentState?.validate() != true) return;
          Navigator.pop(context, ActivityCategoryDraft(name: name));
        },
        child: const Text('保存'),
      ),
    ],
  );
}
