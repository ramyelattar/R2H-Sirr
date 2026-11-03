# M25.2 — Rebuilt Game Card Art Direction

## Cause diagnosed from on-device screenshots
- The old six game tiles reused 124dp rectangular containers, tiny Unicode/emoji-like symbols and an "↗" glyph, with a decorative circle that never communicated the game.
- The home hero laid its key drawing behind its Arabic title and CTA, producing an actual collision.
- Very low-opacity glass (roughly 52–61% in lower/normal layers) exposed saturated pink wallpaper through long Arabic labels, reducing contrast.

## Implementation
- Added **R2HCardArtwork.kt**: authored vector Canvas artwork for each game: couples (two profiles / heart), party (group), dice (3D cube and pips), quiz (sculpted question), whispers (sealed letter), scratch (keyhole), plus remote rooms and music. Nothing is a Unicode font icon, image from the internet or third-party icon pack.
- Rebuilt **R2HModeCard** around a 90dp bespoke scene and a separate 2-line text zone, 182dp minimum height, 24dp radius and contextual edge colors; every option remains fully clickable.
- Rebuilt **main hero** with a horizontal composition: an independent 108dp secret illustration, separate Arabic heading and real CTA. No icon/title overlap, no absolute positioning.
- Rebuilt utility rows with vector marks, larger tap areas and a single engraved chevron.
- Rebalanced **R2HGlass** alpha to 82–93% in selected surfaces to protect text against the bright wallpaper while retaining a visibly translucent layered gradient and glowing rim, specular highlight and native shadow.
- Preserved wallpaper image, title, +18, Play All The Day, status-bar safe area, existing animation, routes, user content, privacy, game logic and local storage. All illustrations are Compose vectors rather than heavyweight bitmaps.

## Verification boundaries
Source-only edits; no APK build, compilation, installation, GitHub Actions or merge. Reviewer must compare real Android screenshot and check small fonts / landscape after first local build.
