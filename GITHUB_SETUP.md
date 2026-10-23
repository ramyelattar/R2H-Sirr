# R2H Sirr — GitHub repository setup

Intended destination: `https://github.com/ramyelattar/R2H-Sirr`

This is a **proposed** repository address, not evidence that the remote exists.

## Create the remote repository

1. Open https://github.com/new while signed into the `ramyelattar` account.
2. Repository name: `R2H-Sirr`.
3. Visibility: **Private** (the project handles private adult-only interactions).
4. Do not initialize with a README, license, or `.gitignore` — all project files are supplied here.
5. Select **Create repository**.

## Push using Git from the included source

```bash
git init -b main
git add .
git -c user.name="R2H Sirr Import" -c user.email="import@users.noreply.github.com" commit -m "chore: import M4 source (no build)"
git remote add origin https://github.com/ramyelattar/R2H-Sirr.git
git push -u origin main
```

If uploading through the GitHub web UI instead, upload the full source folder contents (not the ZIP file alone). Do not upload API keys, .env files, signing keystores or private user content.

## Verification status

This source is M4, unbuilt and not operationally verified on Android. Server-only tests were reported separately; do not treat them as Android verification. No APK, release, or CI build has been performed as part of repository preparation.
