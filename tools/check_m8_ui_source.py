#!/usr/bin/env python3
"""Static source guards for current R2H-Sirr privacy and interaction flows.

These checks look for important implementation contracts; they are not a Kotlin
compiler, Compose UI test, or Android build.
"""
from pathlib import Path
import re
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/r2h/sirr"


def source(name: str) -> str:
    return (APP / name).read_text(encoding="utf-8")


checks = 0
failures = []


def require(label: str, condition: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not condition:
        failures.append(f"{label}" + (f": {detail}" if detail else ""))


def contains(label: str, text: str, *fragments: str) -> None:
    missing = [fragment for fragment in fragments if fragment not in text]
    require(label, not missing, "missing: " + ", ".join(missing))


def matches(label: str, text: str, pattern: str) -> None:
    require(label, re.search(pattern, text, flags=re.DOTALL) is not None)


main = source("MainActivity.kt")
quiz = source("QuizScreen.kt")
scratch = source("ScratchScreenV2.kt")
dice = source("DiceScreen.kt")
experience = source("ExperienceScreen.kt")
whispers = source("WhispersScreen.kt")
effects = source("GameEffects.kt")
remote = source("RemoteScreen.kt")
local_store = source("LocalSessionStore.kt")
transfer = source("RemoteChunkTransfer.kt")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

# Adult access remains a user-attested opt-in stored through the encrypted vault.
contains(
    "adult gate persists explicit consent",
    main,
    'vault.readPrivate("adult-consent-v1")',
    'vault.writePrivate("adult-consent-v1", "accepted")',
    "enabled = ageVerified && consent",
)

# Both Android/system back and the in-app header route through guarded navigation.
contains(
    "back navigation uses the guarded route",
    main,
    "val immersiveRoutes = remember { setOf(",
    "if (route in immersiveRoutes) askToLeave = true else route = \"home\"",
)
matches(
    "header back button uses the guarded route",
    main,
    r'TextButton\(\s*onClick\s*=\s*\{\s*navigateHome\(\)\s*\}',
)
matches(
    "system back handler stays behind the guarded route",
    main,
    r'BackHandler\s*\(\s*enabled\s*=\s*confirmedAdult\s*&&\s*route\s*!=\s*"home"\s*\)\s*\{\s*navigateHome\(\)\s*\}',
)
contains(
    "leaving an active game requires a clear confirmation",
    main,
    'title = { Text("عايز تخرج من اللعبة؟") }',
    'TextButton(onClick = { askToLeave = false; route = "home" })',
    'TextButton(onClick = { askToLeave = false })',
)

# Clearing local data removes both the encrypted vault and temporary media.
contains(
    "privacy clear removes encrypted records and temporary media",
    main,
    "LocalVault(appContext).clear()",
    'filter { it.name.startsWith("sirr_media_") }',
)
contains(
    "app opts out of backup and cleartext networking",
    manifest,
    'android:allowBackup="false"',
    'android:usesCleartextTraffic="false"',
)

# Quiz progress and partner handoff remain encrypted and consent-gated.
contains(
    "quiz session restores, saves, and clears through the vault store",
    quiz,
    "QuizSessionStore.restore(vault)",
    "QuizSessionStore.save(vault, next)",
    "QuizSessionStore.clear(vault)",
)
contains(
    "quiz start remains gated by content consent",
    quiz,
    "enabled = acceptedContent",
)
matches(
    "second-player handoff requires that player's own consent",
    quiz,
    r'QuizStage\.HANDOFF\s*->\s*\{[\s\S]*?checked\s*=\s*acceptedPartner[\s\S]*?enabled\s*=\s*acceptedPartner[\s\S]*?persist\(run\.startPartner\(\)\)',
)
contains(
    "quiz skips remain explicit rather than forced answers",
    quiz,
    "run.predict(-1)",
    "run.answer(-1)",
)

# Local game records are encrypted and validated when restored.
contains(
    "local game records use the private vault",
    local_store,
    "vault.writePrivate(name,json.put(\"turns\",turns).toString())",
    "vault.readPrivate(name)",
)
contains(
    "restored local game records are range-checked",
    local_store,
    "require(version in 1..4 && json.getBoolean(\"group\")==expectedGroup)",
    "require(ts.length() in 6..18)",
    "require(scores==expectedScores && skipped==expectedSkipped)",
)

# All players opt in before a game; active rounds renew consent and persist state.
matches(
    "new game cannot start while any player is still deciding",
    experience,
    r'enabled\s*=\s*!invalidNames[\s\S]{0,300}waiting\.isEmpty\(\)',
)
matches(
    "active challenge requires actor consent and partner consent when applicable",
    experience,
    r'val bothConsent\s*=\s*roundConsent\s*&&\s*\(current\.competition\s*==\s*null\s*\|\|\s*partnerConsent\)',
)
contains(
    "withdrawing consent pauses a running round clock",
    experience,
    "if (!it) pauseOnConsentWithdrawal()",
    "current.pauseRoundClock(System.currentTimeMillis())",
)
contains(
    "game start and progress are saved before being shown as active",
    experience,
    "LocalSessionStore.save(vault, gameName, newGame)",
    "LocalSessionStore.save(vault, gameName, next)",
)
contains(
    "reveal motion respects the user setting",
    experience,
    "if (effects.animate)",
    "else EnterTransition.None",
)

# Scratch cards use bounded memory, hide unrevealed copy from accessibility, and
# require the receiver to opt in before revealing a handed-off secret.
contains(
    "scratch mask is bounded and reveals only after sufficient coverage",
    scratch,
    "BooleanArray(columns * rows)",
    "val coverage = coveredCells / (columns * rows).toFloat()",
    "coverage >= 0.58f",
)
contains(
    "covered scratch text stays hidden from accessibility services",
    scratch,
    "Modifier.clearAndSetSemantics {}",
)
contains(
    "scratch input is bounded to the card and handles continuous gestures",
    scratch,
    ".pointerInput(dimensions)",
    "detectDragGestures(",
    "change.consume()",
    "if (revealed) return",
)
contains(
    "secret handoff requires receiver readiness",
    scratch,
    "checked = viewerReady",
    "enabled = viewerReady",
)

# User-selected audio stops on backgrounding and is released/cleared on exit.
contains(
    "whispers audio pauses when the app is backgrounded",
    whispers,
    "event == Lifecycle.Event.ON_STOP",
    "player.pause()",
)
contains(
    "whispers media and imported photo are cleaned up",
    whispers,
    "player.release()",
    "vault.delete(photoId)",
)
contains(
    "sound effects honor opt-in and saved preferences",
    effects,
    "if (!p.sound || p.volume <= 0) return",
    'vault.writePrivate("game-effects"',
    "playSample(cue, p)",
)

# Personal dice data is loaded and saved through the active encrypted store.
if "TripleDiceStore.load(vault)" in dice:
    contains(
        "three-cube dice state uses its dedicated persistence store",
        dice,
        "TripleDiceStore.load(vault)",
        "TripleDiceStore.save(vault, next)",
        "TripleDiceStore.resetToBlank(vault)",
    )
else:
    contains(
        "two-cube dice state uses its dedicated persistence store",
        dice,
        "PersonalDiceStore.load(vault)",
        "PersonalDiceStore.save(vault, next)",
        "PersonalDiceStore.clear(vault)",
    )

# Remote media must be encrypted, integrity-checked, bounded, and never transfer
# private one-use cards through the general media endpoint.
contains(
    "remote upload fingerprints and rechecks the file before completion",
    transfer,
    'MessageDigest.getInstance("SHA-256")',
    "expectedDigest=fingerprint(context,uri,prefix,size)",
    "if(actual!=expectedDigest)",
)
contains(
    "remote chunks are size-checked and private cards are rejected",
    transfer,
    'require(mime!="application/vnd.sirr.card+json")',
    "require(decoded.size==expected)",
)
contains(
    "remote actions are bounded and retried until acknowledged",
    remote,
    "pendingActions.size>=32",
    "sendPending(forceRetry=true)",
    'pendingActions.remove(data.optString("eventId"))',
)

# Validate the bundled short cues as playable mono 16-bit PCM WAV assets.
sounds_dir = ROOT / "app/src/main/res/raw"
for event in ("round", "reveal", "success", "skip", "complete"):
    cue_file = sounds_dir / f"sirr_{event}.wav"
    require(f"{event} cue exists", cue_file.is_file(), str(cue_file))
    if cue_file.is_file():
        try:
            with wave.open(str(cue_file), "rb") as cue:
                require(
                    f"{event} cue is playable mono PCM",
                    cue.getnchannels() == 1 and cue.getsampwidth() == 2,
                )
                require(
                    f"{event} cue duration is short",
                    cue.getframerate() == 16000
                    and 0.12 <= cue.getnframes() / cue.getframerate() <= 1.3,
                )
        except (wave.Error, OSError) as exc:
            require(f"{event} cue can be opened", False, str(exc))

if failures:
    print(f"FAILED: {len(failures)} of {checks} static source guards")
    for failure in failures:
        print(f" - {failure}")
    sys.exit(1)

print(
    f"R2H-Sirr privacy/consent/session/transfer guards PASS "
    f"({checks} static checks; NOT an Android build)"
)
