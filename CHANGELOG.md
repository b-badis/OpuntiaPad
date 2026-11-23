# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The release workflow publishes the section matching the tag.

## [Unreleased]

### Added
- Tools sheet: format JSON/XML/SQL, minify JSON/XML, JSON ↔ YAML conversion, sort/dedupe/trim lines, case changes, tabs ↔ spaces, Base64/URL encode-decode, checksums. Works on the selection or the whole document, as a single undo step, off the main thread.
- Line actions: comment/uncomment, duplicate, move up/down (symbol bar and `Ctrl+/`, `Ctrl+D`, `Alt+↑/↓`); `Ctrl+Shift+F` formats.
- Tabs and unsaved changes are restored after Android closes the app (setting “Reopen files on launch”).
- “Open in OpuntiaPad” in the text-selection menu of other apps; “New file” launcher shortcut.
- Supply-chain hardening: Gradle dependency verification (SHA-256), GitHub Actions pinned to commit SHAs, automatic verification-metadata updates on Dependabot PRs.
- "Open-source licenses" screen (About) with all bundled license texts and upstream grammar/theme notices; NOTICE file with the copyright statement.
- SonarQube Cloud analysis in CI with JaCoCo unit-test coverage and Android Lint import.
- Automatic merge of Dependabot minor/patch updates once CI passes; majors are labelled for review.
- Optional automatic publishing to Google Play (internal/closed/production tracks, staged rollouts, R8 mapping upload); store listing graphics and privacy policy.
- Automatic "Development build" pre-release (debug APK) published on every push to `main`.
- Shared public debug signing key so development builds can update each other.

### Changed
- New prickly-pear logo (cactus pad with .txt / JSON / YAML / SQL badges), themed-icon support, in-app logo and cactus-green colour palette.
- GitHub Actions updated to their latest major versions.

### Performance
- Grammars are loaded on demand instead of all 88 at start-up (~0.5 s CPU on desktop, ~2 s on phones, saved at every launch).
- Syntax colouring is opt-in for very large files (YAML/Markdown > 256 KB, others > 2 MB), with a banner to turn it on.

### Fixed
- "Open with OpuntiaPad" was not offered for 15 extensions that Android labels with unrelated MIME types (`.ts`, `.rs`, `.dart`, `.bat`, `.svg`, `.xsl`, `.m`, `.pls`…); an emulator test now checks every supported extension.
- SonarQube Cloud quality gate: security findings fixed (pinned actions, non-forgeable bot checks, cleartext traffic disabled), coverage measured on unit-testable code.
- License compliance: the GLSL grammar (no upstream license) is no longer bundled; grammars without a NOTICE entry were verified by hand, and the asset generator now refuses unlicensed grammars.
- Markdown, PHP, Ruby, Perl, LaTeX, Vue, Svelte, Erlang, F#, HTTP and XML highlighting crashed the TextMate engine (malformed captures in the XML grammar); grammars are now sanitised at generation time and every bundled language is tested.
- CI instrumented tests failing because of the above; failing instrumented tests are now printed in the CI log.
- Crash when opening any file with several editor themes (incl. the default light theme): shorthand colours such as `#fff` are now normalised, and a broken theme falls back to default colours instead of crashing.

## [0.1.0] - 2026-11-22

### Added
- First public version of OpuntiaPad.
- Syntax highlighting for 73 languages (TextMate grammars) and 14 editor colour themes.
- Open files from other apps ("Open with", "Share", multiple files) and from the system file picker.
- Automatic language detection by file name, extension, MIME type and content.
- Tabs, recent files, unsaved-changes protection.
- Find & replace with case, whole-word and regex options.
- Line numbers, go to line, word wrap, whitespace rendering, sticky scroll, pinch-to-zoom.
- Symbol bar (Tab, brackets, quotes…) and hardware keyboard shortcuts.
- JSON format / minify, encoding detection and "reopen with encoding", LF/CRLF conversion.
- Adaptive layout for phones, foldables and tablets; light/dark themes and Material You.
- Translations: English, French, Arabic, Spanish, German.
- CI (lint, unit & emulator tests) and automated signed releases on GitHub.

[Unreleased]: https://github.com/b-badis/OpuntiaPad/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/b-badis/OpuntiaPad/releases/tag/v0.1.0
