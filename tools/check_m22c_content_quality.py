#!/usr/bin/env python3
"""Non-Gradle static QA for canonical R2H-Play content. No external deps."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHALLENGES = json.loads((ROOT / "content/challenges_ar_v2.json").read_text(encoding="utf-8"))
QUIZZES = json.loads((ROOT / "content/quizzes_ar_v4.json").read_text(encoding="utf-8"))

def normalized(text: str) -> set[str]:
    text = re.sub(r"[\u064b-\u065f\u0670\u0640]", "", text)
    text = re.sub("[أإآ]", "ا", text).replace("ى", "ي").replace("ة", "ه")
    text = re.sub(r"[^\w\s]", " ", text)
    return set(text.split())

def near_duplicate_pairs(texts: list[str], threshold: float = 0.73) -> int:
    tokens = [normalized(text) for text in texts]
    count = 0
    for i, left in enumerate(tokens):
        for right in tokens[i + 1:]:
            denominator = len(left | right)
            if denominator and len(left & right) / denominator >= threshold:
                count += 1
    return count

assert CHALLENGES["version"] == QUIZZES["version"] == 4
assert set(CHALLENGES) == {"version", "couple", "party"}
levels = {"SOFT", "NORMAL", "SEXY", "HOT", "HARD"}
assert set(CHALLENGES["couple"]) == set(CHALLENGES["party"]) == levels
for mode, expected in (("couple", 26), ("party", 20)):
    assert all(len(CHALLENGES[mode][level]) == expected for level in levels)
challenge_texts = [text for mode in ("couple", "party")
                   for level in ("SOFT", "NORMAL", "SEXY", "HOT", "HARD")
                   for text in CHALLENGES[mode][level]]
assert len(challenge_texts) == len(set(challenge_texts)) == 230
assert all(20 <= len(x) <= 240 for x in challenge_texts)
assert len(QUIZZES["packs"]) == 7
questions = [q for pack in QUIZZES["packs"] for q in pack["questions"]]
assert all(len(pack["questions"]) == 25 for pack in QUIZZES["packs"])
assert len(questions) == len({q["text"] for q in questions}) == 175
assert all(len(q["choices"]) == 3 and 5 <= len(q["text"]) <= 240 for q in questions)
choices = [choice for q in questions for choice in q["choices"]]
assert len(choices) == 525
assert all(isinstance(x, str) and len(x.strip()) > 4 for x in choices)
candidate_challenges = near_duplicate_pairs(challenge_texts)
candidate_questions = near_duplicate_pairs([q["text"] for q in questions])
assert candidate_challenges == 0, f"Review {candidate_challenges} similar challenge pairs"
assert candidate_questions == 0, f"Review {candidate_questions} similar question pairs"
print("M22-C static QA: 230 challenges, 175 questions, 525 contextual choices; no flagged near-duplicate pairs")
