# M8 — Egyptian Arabic + Mystery-first Android UX

## Brief
- Arabic UI written as everyday Egyptian colloquial speech, not formal Arabic.
- A mysterious, inviting adult-only visual identity; sensual in mood without explicit imagery.
- A clear primary action on the first screen and progressively disclosed settings.
- Private, consensual games: skip/stop controls and explicit consent remain visible.
- Avoid promotional counts, implementation jargon, or technical descriptions in core gameplay.

## Source changes
- `MainActivity.kt`: dark wine, ink and champagne palette; new welcome gate; hero-first home screen; feature hierarchy, visual rings and differentiated cards; privacy copy.
- `ExperienceScreen.kt`: choose mood and rounds before starting; advanced settings, custom prompts and participant names are collapsed; consent, skip and stop remain available; one challenge per round.
- `QuizScreen.kt`: Egyptian colloquial headers and one question per step, with large response controls.
- `GameEffects.kt`: motion, sound, vibration preferences are collapsed by default, with reduced-motion behavior preserved.
- `DiceScreen.kt`, `ScratchScreenV2.kt`, `WhispersScreen.kt`: Egyptian colloquial microcopy while preserving game behavior.

## Voice rules
- Prefer: "يلا نبدأ", "مش دلوقتي… عَدّي", "سيبها على النرد", "اختار المود", "خدوا راحتكم".
- Avoid mechanical/formal headings: "إكمال التحدي", "تنتهي صلاحية العرض", "الإعدادات السينمائية".
- Keep instructions short and decisive. Never imply participation is obligatory.
- No misleading claims that privacy features prevent forwarding, photographing, or a recipient keeping media.

## Acceptance checkpoints — NOT YET VERIFIED
- Build `feature/m8-egyptian-experience` with Gradle only after owner opts for another Android test.
- Visually inspect 360dp and larger phone screens, RTL layout, long Egyptian Arabic labels, font scaling and safe-area insets.
- Verify options, consent, custom prompts, game round progression, reveal interactions, audio, haptics, privacy, and WebSocket flow remain functional.
- Photograph/render home, consent, couple setup, one challenge and quiz. Check against image references before calling UI release-ready.

## Remaining design work
- Dedicated 3D dice/artwork, scratch media scene and personalized gift reveal.
- Remote room UI, deeper Arabic copy editing of remaining screens and content itself.
- Native typography/fonts and asset system; accessibility contrast; RTL motion.
- Better original content with coherent play progression rather than number of generic prompts.
- Full Android compile, UI test and device acceptance. The changes in this branch are **source-only**.
