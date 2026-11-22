# Code quality & dependency automation

## Pipeline overview

| Workflow | Trigger | What it does |
|---|---|---|
| **CI** (`ci.yml`) | push to `main`, pull requests, manual | Lint, unit tests (incl. validation of all bundled grammars/themes), debug + R8 release builds, emulator tests, SonarQube Cloud analysis, "Development build" pre-release on `main` |
| **Release** (`release.yml`) | tag `vX.Y.Z` | Signed APK/AAB, GitHub Release, provenance, optional Google Play upload ([PLAY_STORE.md](PLAY_STORE.md)) |
| **Dependabot verification metadata** (`dependabot-verification.yml`) | Dependabot Gradle PR | Regenerates `gradle/verification-metadata.xml` for the new versions, pushes it to the PR and re-runs CI |
| **Dependabot auto-merge** (`dependabot-auto-merge.yml`) | CI finished on a Dependabot PR | Merges minor/patch updates when CI is green; labels major updates for review |

## SonarQube Cloud (one-time setup, ~5 minutes)

Free for public repositories.

1. Sign in at <https://sonarcloud.io> with GitHub and **import** the `b-badis/OpuntiaPad` repository
   (*+ → Analyze new project*). Note the **organization key** and **project key** it shows
   (by default `b-badis` and `b-badis_OpuntiaPad`, which the build already uses).
2. In the project: *Administration → Analysis Method* → **turn off Automatic Analysis**
   (the CI analysis replaces it and also sends test coverage and Android Lint results).
3. *My Account → Security* → generate a token, then add it to GitHub:

   ```bash
   gh secret set SONAR_TOKEN
   # only if your keys differ from the defaults:
   gh variable set SONAR_ORGANIZATION --body b-badis
   gh variable set SONAR_PROJECT_KEY --body b-badis_OpuntiaPad
   ```

4. Push a commit: the **SonarQube Cloud analysis** job in CI uploads the analysis (Kotlin code smells, bugs,
   vulnerabilities, duplications, JaCoCo unit-test coverage, Android Lint issues). Pull requests get a
   quality-gate check and a summary comment.

Without `SONAR_TOKEN` the job just logs a notice and succeeds, so forks and Dependabot PRs aren't blocked.

Run locally: `SONAR_TOKEN=… ./gradlew testDebugUnitTest createDebugUnitTestCoverageReport lintDebug sonar`.

## Automatic dependency updates (Dependabot)

- Every Monday, Dependabot checks Gradle dependencies (version catalog, plugins, Gradle wrapper) and
  GitHub Actions.
- **Minor and patch** updates are grouped into one pull request per ecosystem. When the full CI passes on it,
  `dependabot-auto-merge.yml` squash-merges it and re-runs CI on `main` (which refreshes the development build).
- **Major** updates (e.g. AGP 9 → 10) arrive as individual pull requests. CI still runs; when green they get the
  `major-update` label and a comment, and wait for you to merge them.
- A failing update is never merged: fix it or close the pull request.

To stop auto-merging, disable the *Dependabot auto-merge* workflow in the Actions tab.

## Supply-chain protections

- **Dependency verification**: Gradle checks the SHA-256 of every downloaded artifact against
  `gradle/verification-metadata.xml`; a tampered or unexpected artifact fails the build.
- **Pinned GitHub Actions**: every action is referenced by its full commit SHA (with the version in a comment);
  Dependabot updates the SHA and the comment together.
- **Least-privilege workflows**: read-only token by default; write permissions are granted per job only where
  needed, the Dependabot workflows trust only the immutable bot account id, and the metadata job builds without
  any write credential.

## Coverage scope

SonarQube measures unit-test coverage on the pure-Kotlin logic (`core/text`, `core/language` detection…).
Android-framework code (Compose UI, Activity, the editor view, ContentResolver, DataStore) is excluded from
coverage because it can't run in JVM unit tests; the instrumented tests exercise it on an emulator.
