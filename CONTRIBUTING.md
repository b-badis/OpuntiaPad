# Contributing to OpuntiaPad

Thanks for helping! Bug reports, ideas, translations and code are all welcome.

## Reporting bugs / requesting features

Use the [issue templates](https://github.com/b-badis/OpuntiaPad/issues/new/choose). For bugs, include the
OpuntiaPad version (Settings → About), your Android version and device, and — when possible — a sample file.
**Do not attach files containing secrets or personal data.**

## Development setup

1. JDK 17+ (21 recommended) and the Android SDK, or simply Android Studio.
2. `./gradlew assembleDebug` builds `app/build/outputs/apk/debug/opuntiapad-debug.apk`
   (the debug build installs next to the release build: its id ends with `.debug`).
3. Before opening a pull request, run:

   ```bash
   ./gradlew lintDebug testDebugUnitTest
   ```

   If you have a device or emulator: `./gradlew connectedDebugAndroidTest`.

## Code guidelines

- Kotlin, official code style (`.editorconfig`), 140 columns max.
- Keep the UI in Jetpack Compose and put logic that can be unit-tested in `core/` (pure Kotlin, no Android).
- Every user-visible string goes in `res/values/strings.xml` — never hard-code text. Add the English
  string; translators handle the rest (adding them yourself is appreciated).
- OpuntiaPad must keep working **offline and without permissions**. Don't add network access, analytics or
  tracking libraries.
- Add or update tests for behaviour changes; update `CHANGELOG.md` under `[Unreleased]`.

## Commits and pull requests

- One topic per pull request, with a clear description and screenshots for UI changes.
- Write commit messages in the imperative mood (“Add TOML detection”), ideally following
  [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`…).
- CI (lint, unit tests, emulator tests) must be green before merging.

## Adding a language or an editor theme

Grammars and themes come from the `tm-grammars` / `tm-themes` npm packages. Edit the `LANGUAGES` or
`THEMES` list in [`tools/generate-textmate-assets.mjs`](tools/generate-textmate-assets.mjs) and run:

```bash
./tools/update-textmate-assets.sh
```

Commit the regenerated files under `app/src/main/assets/textmate/` and `docs/THIRD_PARTY_GRAMMARS.md`.

## App icon

The logo (prickly-pear pad with file-type badges) is drawn in code by
[`tools/generate-icons.py`](tools/generate-icons.py), which writes the adaptive launcher icon, the themed-icon
silhouette, the in-app logo and `docs/images/icon.svg`. Edit the script, never the generated files:

```bash
python3 -m pip install fonttools shapely
python3 tools/generate-icons.py
```

## Translations

See [docs/TRANSLATING.md](docs/TRANSLATING.md).
