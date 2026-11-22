# Architecture

Single-activity Android app, Kotlin + Jetpack Compose, MVVM.

```
app/src/main/java/io/github/b-badis/OpuntiaPad/
├── OpuntiaPadApp.kt              Application: singletons, background grammar loading
├── MainActivity.kt           Handles VIEW / EDIT / SEND / SEND_MULTIPLE intents, keyboard shortcuts
├── core/                     Mostly pure Kotlin, unit-tested
│   ├── text/                 TextCodec (encoding & BOM detection, binary heuristic), LineEnding, JsonFormatter
│   ├── language/             Language model, LanguageDetector (name/ext/MIME/content), LanguageCatalog (assets)
│   ├── files/                DocumentRepository: SAF / content-provider reads & writes, permissions
│   └── settings/             SettingsRepository (DataStore): editor preferences + recent files
├── editor/                   Bridge to Sora Editor
│   ├── TextMateSupport.kt    Grammar & theme registries
│   ├── EditorDocument.kt     One open tab (text model with its own undo history, encoding, dirty state…)
│   ├── EditorController.kt   Compose-observable editor state & commands (search, undo, zoom…)
│   └── CodeEditorHost.kt     AndroidView hosting the CodeEditor and wiring events
└── ui/                       Compose UI
    ├── MainViewModel.kt      Tabs, open/save/close flows, document operations, one-shot UI events
    ├── OpuntiaPadRoot.kt         Theme + screen switching
    ├── screens/              Editor, Settings, About
    └── components/           Tabs, status bar, symbol bar, search panel, dialogs, home & files panel
```

## Key decisions

- **Sora Editor + TextMate** rather than a custom text widget: native performance on large files, selection
  handles, pinch zoom, and access to the VS Code grammar ecosystem. Grammars are bundled (no network).
- **One editor view, many documents**: each tab owns a Sora `Content` (with its own undo stack) that is swapped
  into a single `CodeEditor`, keeping memory low and tab switches instant.
- **Storage Access Framework only**: no storage permission. Files opened through the picker get persistable
  permissions (recent files survive restarts); files received from other apps use temporary grants and fall back
  to "Save as…" if they are read-only.
- **Adaptive layout** via `BoxWithConstraints`: compact → navigation drawer; ≥ 600 dp → more toolbar actions and
  tabs always visible; ≥ 840 dp → permanent files panel.
- **Generated assets**: `tools/generate-textmate-assets.mjs` produces `assets/textmate/` (grammars, VS Code-style
  language configurations, themes, and `catalog.json` with display names/extensions), so adding a language is a
  one-line change.
