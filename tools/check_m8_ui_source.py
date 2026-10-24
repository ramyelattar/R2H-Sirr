#!/usr/bin/env python3
"""Source-level UX regression checks. Not an Android or Compose compilation test."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/r2h/sirr"

def source(name):
    return (APP / name).read_text(encoding="utf-8")

main = source("MainActivity.kt")
quiz = source("QuizScreen.kt")
scratch = source("ScratchScreenV2.kt")
dice = source("DiceScreen.kt")
visuals = source("SirrVisuals.kt")
logic = source("core/ExperienceLogic.kt")
experience = source("ExperienceScreen.kt")
whispers = source("WhispersScreen.kt")

assert 'readPrivate("adult-consent-v1")' in main
assert 'writePrivate("adult-consent-v1", "accepted")' in main
assert 'LocalVault(context.applicationContext).clear()' in main
assert 'Text("→ رجوع"' in main
assert 'else if (phase == 2 && pairing)' in quiz
assert 'phase == 2 && pairing && !acceptedB' not in quiz, "Don't hide consent handoff before start"
assert 'selectedIndices[revealStep]' in quiz
assert 'answersB[revealStep]' in quiz
assert 'trace(change.position - dragAmount, change.position)' in scratch
assert '.pointerInput(dimensions)' in scratch
assert 'coverage = (touched.size / 192f)' in scratch
assert 'ChallengeRevealCard(faces.joinToString(" ") { it.text })' in dice
assert 'MysteryDie(' in dice and 'MysteryHeader(' in dice
assert 'ChallengeRevealCard(' in visuals
assert 'listOf("قول", "اسأل عن", "اقترح", "اختار", "احكي", "اكتب")' in logic


assert 'SealedChallengeCard(onReveal = {' in experience
assert 'if (!isRevealed)' in experience and 'if (isRevealed)' in experience
assert 'Text("مش دلوقتي… عَدّي")' in experience
assert 'fun toggleAudio()' in whispers
assert 'شغّل موسيقى المفاجأة' in whispers
assert 'effects.cue(GameCue.REVEAL)' in scratch
assert 'Modifier.fillMaxWidth().heightIn(min = 184.dp)' in main
assert 'محتاج سيرفر آمن خاص' in main

print("M8 UI source regression guards PASS (static checks only, NOT Android build)")
