# Translating OpuntiaPad

OpuntiaPad is available in English (default), French, Arabic, Spanish and German. Adding a language takes about 30 minutes:

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<code>/strings.xml`
   (e.g. `values-it`, `values-pt-rBR`).
2. Translate every `<string>` and `<plurals>` entry. Remove the `app_name` line (it is not translatable).
   Keep placeholders such as `%1$s` / `%1$d` and escape apostrophes (`\'`) or use typographic ones (`’`).
3. Add the language code to `androidResources.localeFilters` in `app/build.gradle.kts`.
4. Add it to `APP_LANGUAGES` in `ui/screens/SettingsScreen.kt` with its native name.
5. Optionally translate the store texts in `fastlane/metadata/android/<locale>/`.
6. Run `./gradlew lintDebug` and open a pull request.

Right-to-left languages are supported: the UI mirrors automatically, the code area always stays left-to-right.
