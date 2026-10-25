# M22-B — Question skip, scratch layer preservation, and dice result semantics

## Implementation
1. `QuizInsights.compareAnswered()` allows an explicit `-1` skip marker, includes only pairs with answers from BOTH people, and returns null when there are no joint answers. Existing `compare()` API remains untouched and strict for valid choices 0..2.
2. `QuizScreen.kt` adds per-question skip in solo/paired modes. One `-1` marker is appended on skip to keep the original ordered question indices and answers aligned across both participants. Reveal view safely handles skipped answers without indexing `choices[-1]`. End-of-game copy distinguishes no joint answers from 0 agreements, and reports actual jointly answered denominator.
3. `ScratchScreenV2.kt`: selecting a ready-made group no longer silently resets the user's chosen 1/2/3-layer setting. Scratch gesture engine and media/expiry logic untouched.
4. `DiceScreen.kt`: the random three face strings are shown as independently labeled **action / topic / manner** components, instead of asserting their concatenation is a grammatical sentence. Optional reroll, custom dice and results preserved; M22-C still needs a language/content rewrite for fully coherent combinations.
5. `QuizInsightsSkipTest.kt`: source-level JVM regression tests for skip+match, all skipped, and invalid answer indices. **Not executed.**

## Protected
- No edits to canonical challenges/quizzes JSON, generated Kotlin content, original whisper/scratch user text, dice face text, backend/web protocols, save keys, private media data, sound or scoring rules.
- Challenge per-round and quiz start opt-in from M22-A retained.
- No build, compilation, Gradle tests, APK install, CI run, emulator or merge.
