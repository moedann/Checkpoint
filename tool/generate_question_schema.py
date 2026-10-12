"""Embed the trusted pack schema; runtime never loads an external schema."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
schema = (root / 'formats/checkpoint-question-pack.schema.json').read_text(encoding='utf-8')
json.loads(schema)
if "'''" in schema:
    raise ValueError('Schema cannot be embedded as a raw Dart literal')
(root / 'lib/question_engine/schema_data.dart').write_text(
    "// Generated from formats/checkpoint-question-pack.schema.json.\n"
    "part of 'question_engine.dart';\n"
    "final Map<String, dynamic> _packSchema = jsonDecode(r'''" + schema +
    "''') as Map<String, dynamic>;\n", encoding='utf-8')
