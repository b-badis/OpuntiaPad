# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The release workflow publishes the section matching the tag.

## [Unreleased]

### Added
- Automatic "Development build" pre-release (debug APK) published on every push to `main`.
- Shared public debug signing key so development builds can update each other.

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
