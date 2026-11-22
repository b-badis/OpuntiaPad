<div align="center">

<img src="docs/images/icon.svg" width="96" alt="OpuntiaPad logo" />

# OpuntiaPad

**A fast, friendly code & text editor for Android — open source, no ads, no tracking.**

Open YAML, JSON, SQL, XML, Markdown, logs and 70+ other formats straight from your file manager,
mail or chat app, with real syntax highlighting, tabs, search & replace and pinch-to-zoom.

[![CI](https://github.com/b-badis/OpuntiaPad/actions/workflows/ci.yml/badge.svg)](https://github.com/b-badis/OpuntiaPad/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/b-badis/OpuntiaPad?include_prereleases&sort=semver)](https://github.com/b-badis/OpuntiaPad/releases)
[![License](https://img.shields.io/github/license/b-badis/OpuntiaPad)](LICENSE)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=b-badis_OpuntiaPad&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=b-badis_OpuntiaPad)
![Min Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

[**Download the latest APK**](https://github.com/b-badis/OpuntiaPad/releases/latest) · [Development build](https://github.com/b-badis/OpuntiaPad/releases/tag/dev) · [Français](README.fr.md)

</div>

---

## Features

| | |
|---|---|
| 📂 **Opens files from any app** | “Open with…” and “Share → OpuntiaPad” from file managers, Gmail, Outlook, WhatsApp, Telegram, Drive, browsers' downloads… Also works with the system file picker. |
| 🎨 **Real syntax highlighting** | 73 languages powered by VS Code's TextMate grammars: YAML, JSON/JSONC/JSON5, SQL & PL/SQL, XML, HTML, CSS, Markdown, Python, Java, Kotlin, JS/TS, Go, Rust, C/C++, C#, PHP, Shell, PowerShell, Dockerfile, Terraform, TOML, INI/properties, `.env`, logs, CSV… |
| 🧠 **Automatic language detection** | From the file name (`Dockerfile`, `.bashrc`…), extension, MIME type or the content itself (JSON, YAML, SQL, XML, shebangs…). Can be changed manually at any time. |
| ✏️ **Real editing** | Undo/redo per file, selection handles, indent/unindent, auto-indent, auto-closing brackets, optional word completion, Tab & symbol bar above the keyboard. |
| 🔎 **Find & replace** | Match case, whole word, regular expressions, match counter, replace one / all. |
| 🗂️ **Tabs** | Notepad++-style tabs with unsaved indicator; recent files list. |
| 🔢 **Line numbers & navigation** | Line numbers (optionally pinned), go to line, current-line highlight, sticky scroll, bracket matching, whitespace rendering. |
| 🔍 **Zoom** | Pinch-to-zoom, zoom menu & `Ctrl +/-`; the size is remembered. |
| 📱 **Adapts to every screen** | Phones, foldables and tablets: on large screens the files panel stays open next to the editor. Light/dark themes, Material You colours, 14 editor colour themes. |
| 🌍 **Multilingual** | English, Français, العربية (RTL), Español, Deutsch — per-app language setting. |
| 🧰 **Developer goodies** | Format / minify JSON, encoding detection (UTF-8, UTF-16, BOM, Windows-1252…) & reopen with encoding, LF/CRLF conversion, hardware keyboard shortcuts. |
| 🔒 **Private** | No INTERNET permission, no analytics, no ads. Files are accessed only through Android's permission grants. |

### Keyboard shortcuts (hardware keyboard)

`Ctrl+S` save · `Ctrl+Shift+S` save as · `Ctrl+O` open · `Ctrl+N` new · `Ctrl+W` close tab · `Ctrl+Tab` next tab ·
`Ctrl+F` find · `Ctrl+G` go to line · `Ctrl +/-` zoom · `Ctrl+Z/Y` undo/redo · `Ctrl+A/C/X/V`

## Install

1. Download `opuntiapad-x.y.z.apk` from the [latest release](https://github.com/b-badis/OpuntiaPad/releases/latest).
2. Open it on your phone and allow installation from this source when Android asks.
3. Verify the download if you wish: `sha256sum -c SHA256SUMS.txt`, or
   `gh attestation verify opuntiapad-x.y.z.apk --repo b-badis/OpuntiaPad` (every release has signed build provenance).

Requires Android 8.0 (API 26) or newer.

## Build from source

Requirements: JDK 17+ (21 recommended) and the Android SDK (or Android Studio).

```bash
git clone https://github.com/b-badis/OpuntiaPad.git
cd OpuntiaPad
./gradlew assembleDebug          # → app/build/outputs/apk/debug/opuntiapad-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android lint
./gradlew connectedDebugAndroidTest  # instrumented tests (device or emulator)
```

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single activity, MVVM (`ViewModel` + Compose state).
- **[Sora Editor](https://github.com/Rosemoe/sora-editor)** — high-performance native code editor view (selection, pinch-zoom, large files).
- **TextMate grammars & themes** from [shikijs/textmate-grammars-themes](https://github.com/shikijs/textmate-grammars-themes), bundled offline.
- **DataStore** for settings, **Storage Access Framework** for files, **AppCompat** per-app languages.
- **GitHub Actions**: lint, unit & emulator tests and SonarQube Cloud analysis on every push/PR; signed APK + AAB, checksums and build provenance on every tag; Dependabot updates merged automatically when CI is green ([docs/QUALITY.md](docs/QUALITY.md)).

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a tour of the code.

## Releasing

Releases are fully automated: update `CHANGELOG.md`, then push a tag.

```bash
git tag v1.0.0
git push origin v1.0.0
```

The one-time signing setup is described in [docs/RELEASING.md](docs/RELEASING.md); publishing to Google Play
is automated too — see [docs/PLAY_STORE.md](docs/PLAY_STORE.md). Privacy policy: [PRIVACY.md](PRIVACY.md).

## Contributing

Bug reports, translations and pull requests are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md)
and [docs/TRANSLATING.md](docs/TRANSLATING.md). By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

OpuntiaPad is licensed under the [Apache License 2.0](LICENSE).
Copyright 2026 b-badis and the OpuntiaPad contributors — see [NOTICE](NOTICE).
It bundles third-party software and data under their own licenses — see [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md);
all license texts are also available in the app (*About → Open-source licenses*).
