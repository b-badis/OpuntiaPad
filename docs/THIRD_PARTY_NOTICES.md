# Third-party notices

OpuntiaPad is licensed under the [Apache License 2.0](../LICENSE) — Copyright 2026 Badis Barbouch and the OpuntiaPad contributors ([NOTICE](../NOTICE)).
It includes or depends on the following software and data:

| Component | License | In the APK |
|---|---|---|
| [Sora Editor](https://github.com/Rosemoe/sora-editor) (`io.github.rosemoe:editor`, `language-textmate`) — © 2020-2025 Rosemoe | LGPL-2.1 | ✓ |
| [Eclipse TM4E](https://github.com/eclipse/tm4e) (bundled in Sora's TextMate module) — © Angelo ZERR and others | EPL-2.0 | ✓ |
| [Joni](https://github.com/jruby/joni) / [JCodings](https://github.com/jruby/jcodings) — JRuby project | MIT | ✓ |
| [Gson](https://github.com/google/gson) — © 2008 Google Inc. | Apache-2.0 | ✓ |
| [SnakeYAML Engine](https://bitbucket.org/snakeyaml/snakeyaml-engine) — © 2018 SnakeYAML (JSON ↔ YAML conversion) | Apache-2.0 | ✓ |
| AndroidX, Jetpack Compose, Material components & icons — The Android Open Source Project | Apache-2.0 | ✓ |
| Kotlin standard library & kotlinx.coroutines — JetBrains and contributors | Apache-2.0 | ✓ |
| 88 TextMate grammars and 14 themes via [shikijs/textmate-grammars-themes](https://github.com/shikijs/textmate-grammars-themes) | MIT (packaging) + each upstream license, see [THIRD_PARTY_GRAMMARS.md](THIRD_PARTY_GRAMMARS.md) | ✓ |
| [Inter](https://rsms.me/inter/) font — only used at build time to draw the icon labels as outlines | OFL-1.1 | artwork only |

## How the obligations are met

- **License texts and notices travel with the app.** `app/src/main/assets/licenses/` contains the
  Apache-2.0, LGPL-2.1, EPL-2.0 and MIT texts plus every upstream grammar/theme notice; they are shown in
  *Settings → About → Open-source licenses*.
- **LGPL-2.1 / EPL-2.0**: Sora Editor and TM4E are used unmodified, as published on Maven Central; their
  complete source code is available at the links above, and OpuntiaPad's own source is public, so the
  app can be rebuilt with a modified version of these libraries.
- **Grammars**: `tools/generate-textmate-assets.mjs` only bundles grammars with a known license. Files not
  covered by the package's NOTICE were checked by hand at the exact upstream commit
  ([tools/license-overrides](../tools/license-overrides)); grammars with no upstream license (e.g. GLSL)
  are excluded, and the generator fails if a bundled grammar ever lacks a notice.
