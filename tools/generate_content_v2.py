#!/usr/bin/env python3
"""Generate v4 Kotlin, Android, and web challenge/quiz banks from canonical Egyptian JSON.

Run --check in CI to verify all clients use byte-identical catalog representations.
No external dependencies or Android/Gradle needed.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
K = ROOT / "app/src/main/java/com/r2h/sirr/core"
challenge = json.loads((ROOT / "content/challenges_ar_v2.json").read_text(encoding="utf-8"))
quizzes = json.loads((ROOT / "content/quizzes_ar_v4.json").read_text(encoding="utf-8"))

assert challenge["version"] == quizzes["version"] == 4
assert set(challenge) == {"version", "couple", "party"}
prompts = []
for scope in ("couple", "party"):
    assert set(challenge[scope]) == {"SOFT", "NORMAL", "SEXY", "HOT", "HARD"}
    for level, entries in challenge[scope].items():
        assert len(entries) >= (20 if scope == "couple" else 10)
        assert len(entries) == len(set(entries))
        assert all(isinstance(x, str) and 20 <= len(x) <= 240 for x in entries)
        prompts.extend(entries)
assert len(prompts) == len(set(prompts)) == 230

assert len(quizzes["packs"]) == 7
assert [p["id"] for p in quizzes["packs"]] == [f"quiz_{i:02d}" for i in range(1, 8)]
for pack in quizzes["packs"]:
    assert len(pack["questions"]) == 25
    assert len(set(q["text"] for q in pack["questions"])) == 25
    assert all(
        len(q["choices"]) == 3
        and 5 <= len(q["text"]) <= 240
        and all(isinstance(s, str) and s.strip() for s in q["choices"])
        for q in pack["questions"]
    )

def kt(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'

output = {}
clines = [
    "package com.r2h.sirr.core", "",
    "/** Generated from content/challenges_ar_v2.json. Do not edit by hand. */",
    "internal object AdultChallengesV2 {",
]
for scope in ("couple", "party"):
    clines.append(f"    val {scope}: Map<Intensity, List<String>> = mapOf(")
    for level, items in challenge[scope].items():
        clines.append(f"        Intensity.{level} to listOf(")
        clines += [f"            {kt(text)}," for text in items]
        clines.append("        ),")
    clines.append("    )")
clines += ["}", ""]
output[K / "AdultChallengesV2.kt"] = "\n".join(clines)

qlines = [
    "package com.r2h.sirr.core", "",
    "/** Generated from content/quizzes_ar_v4.json by tools/generate_content_v2.py. */",
    "internal object EgyptianQuizCatalog {",
    "    val packs: List<Quiz> = listOf(",
]
for pack in quizzes["packs"]:
    qlines.append(f"        Quiz({kt(pack['name'])}, listOf(")
    for q in pack["questions"]:
        choices = ", ".join(map(kt, q["choices"]))
        qlines.append(f"            Question({kt(q['text'])}, listOf({choices})),")
    qlines.append("        )),")
qlines += ["    )", "}", ""]
output[K / "EgyptianQuizCatalog.kt"] = "\n".join(qlines)

output[ROOT / "backend/public/quizzes.json"] = json.dumps(quizzes, ensure_ascii=False, indent=2) + "\n"
output[ROOT / "backend/public/challenges.json"] = json.dumps(challenge, ensure_ascii=False, indent=2) + "\n"
outdated = []
for path, text in output.items():
    if "--check" in sys.argv:
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            outdated.append(str(path.relative_to(ROOT)))
    else:
        path.write_text(text, encoding="utf-8")
if outdated:
    raise SystemExit("Out-of-date generated content: " + ", ".join(outdated))
print(f"Content v4: {len(prompts)} unique challenges and "
      f"{sum(len(p['questions']) for p in quizzes['packs'])} questions "
      f"({'verified' if '--check' in sys.argv else 'generated'})")
