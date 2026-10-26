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
artwork = source("SirrDiscovery.kt")
sound = source("GameEffects.kt")
experience = source("ExperienceScreen.kt")
whispers = source("WhispersScreen.kt")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

assert 'readPrivate("adult-consent-v1")' in main
assert 'writePrivate("adult-consent-v1", "accepted")' in main
assert 'LocalVault(appContext).clear()' in main
assert 'Text("رجوع  ←"' in main
assert 'else if (phase == 2 && pairing)' in quiz
assert 'phase == 2 && pairing && !acceptedB' not in quiz, "Don't hide consent handoff before start"
assert 'selectedIndices[revealStep]' in quiz
assert 'answersB[revealStep]' in quiz
assert 'drawPath(path, color = Color.Transparent' in scratch
assert 'StrokeCap.Round' in scratch
assert 'append(change.position)' in scratch
assert 'mark(Offset(previous.x + dx * t, previous.y + dy * t))' in scratch
assert '.pointerInput(dimensions)' in scratch
assert 'coverage = (coveredCells / 192f)' in scratch
assert 'BooleanArray(192)' in scratch and 'coveredCells++' in scratch
assert 'ChallengeRevealCard(faces.mapIndexed' in dice and 'joinToString("\\n")' in dice
assert 'MysteryDie(' in dice and 'internal fun MysteryHeader(' in visuals
assert 'ChallengeRevealCard(' in visuals
assert 'private val soft = listOf(' in logic and 'Intensity.CUSTOM to soft' in logic


# M22+: test behavioral source contracts, not the exact copy from historical M8.
assert 'val group = secretGroups.random()' in scratch
assert 'stages = 1\n                },' not in scratch, "Ready-made scratch must preserve 1/2/3 layers"
assert 'val groupStart = (0 until (suggestions.size / 3)).random() * 3' in whispers
assert 'messages[page] = ""' in whispers
assert 'fun compareAnswered(a: List<Int>, b: List<Int>)' in source("core/QuizInsights.kt")
assert 'answerOrSkip(-1)' in quiz and 'if (first >= 0)' in quiz and 'if (second >= 0)' in quiz
assert 'QuizInsights.compareAnswered(answersA, answersB)' in quiz
assert 'enabled = acceptedContent' in quiz and 'acceptedContent = false' in quiz
assert 'enabled = roundConsent' in experience and 'checked = roundConsent' in experience
assert 'resetStartConsents()' in experience and 'consent[i] = false' in experience
assert 'onClick = { finishRound(false) }' in experience

assert 'SealedChallengeCard(onReveal = {' in experience
assert 'if (!isRevealed)' in experience and 'if (isRevealed)' in experience
assert 'onClick = { finishRound(false) }' in experience
assert 'fun toggleAudio()' in whispers
assert 'شغّل موسيقى المفاجأة' in whispers
assert 'effects.cue(GameCue.REVEAL)' in scratch
assert 'FeatureRow(' in main and 'MysteryHeader(' in visuals
assert 'محتاج سيرفر HTTPS خاص' in main

assert 'internal fun DiscoveryTile(' in artwork and 'DiscoveryMotif.DICE' in artwork
assert 'QuizTopicTile(' in quiz and 'internal fun QuizTopicTile(' in artwork
assert '"settings" -> GameSettingsScreen(' in main
assert 'EffectPreferencesPanel(effects, initiallyExpanded = true)' in main
assert 'FeatureRow("الصوت والخصوصية"' in main
assert 'assert' not in scratch[:200], "No dummy source assertions inside Compose"
assert 'savedPoints' in scratch and 'strokes.last().add(p)' in scratch
assert 'strokes[strokes.lastIndex] = strokes.last() + p' not in scratch
assert 'Brush.linearGradient' in artwork and 'drawPath' in scratch

assert 'DiscoveryMotif.WHISPER' in artwork and 'Canvas(' in artwork
assert 'SoundPool.Builder()' in sound
assert 'ToneGenerator' not in sound and 'ToneGenerator' not in dice
assert 'EnterTransition.None' in experience, "Reduced motion must disable reveal transition"
assert 'AnimatedVisibility(' in experience
assert 'playSample(cue, p)' in sound, "Cues must honor opt-in preferences"

# Validate each original bundled cue as a short playable RIFF/PCM WAV.
import wave
sounds_dir = ROOT / "app/src/main/res/raw"
for event in ("round", "reveal", "success", "skip", "complete"):
    cue_file = sounds_dir / f"sirr_{event}.wav"
    assert cue_file.is_file(), f"Missing audio cue {cue_file.name}"
    with wave.open(str(cue_file), "rb") as cue:
        assert cue.getnchannels() == 1 and cue.getsampwidth() == 2
        assert cue.getframerate() == 16000
        assert .12 <= cue.getnframes() / cue.getframerate() <= 1.3

# P0: avoid accidental loss of active rounds and encrypted local records.
assert 'BackHandler(enabled = confirmedAdult && route != "home") { navigateHome() }' in main
assert 'if (route in immersiveRoutes) askToLeave = true' in main
assert 'onClick = { navigateHome() }' in main
assert 'title = { Text("عايز تخرج من اللعبة؟") }' in main
assert 'var confirmClear by remember { mutableStateOf(false) }' in main
assert 'onClick = { confirmClear = true }' in main
assert 'title = { Text("متأكد إنك عايز تمسح بياناتك؟") }' in main
assert 'LocalVault(appContext).clear()' in main
assert 'it.name.startsWith("sirr_media_")' in main
assert 'android:allowBackup="false"' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest

# No user-selected soundtrack must survive Android backgrounding.
assert 'LifecycleEventObserver' in whispers
assert 'event == Lifecycle.Event.ON_STOP' in whispers
assert 'player.pause()' in whispers
assert 'activity?.lifecycle?.removeObserver(observer)' in whispers

remote = source("RemoteScreen.kt")
session = source("LocalSessionStore.kt")
transfer = source("RemoteChunkTransfer.kt")
assert 'rememberSaveable' in quiz and 'rememberSaveable' in main
assert 'LocalSessionStore.load(' in experience and 'LocalSessionStore.save(' in experience
assert 'fun writeReply(' in session and 'vault.writePrivate(name,json.toString())' in session
assert 'pendingActions' in remote and 'forceRetry=true' in remote
assert 'expectedDigest=fingerprint(' in transfer and 'actual!=expectedDigest' in transfer
assert 'val content=ByteArray(total);var copied=0' not in transfer
print("M23 UI/source regression guards PASS (static checks only, NOT Android build)")
