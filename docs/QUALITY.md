# Code quality & dependency automation

## Pipeline overview

| Workflow | Trigger | What it does |
|---|---|---|
| **CI** (`ci.yml`) | push to `main`, pull requests, manual | Lint, unit tests (incl. validation of all bundled grammars/themes), debug + R8 release builds, emulator tests, SonarQube Cloud analysis, "Development build" pre-release on `main` |
| **Release** (`release.yml`) | tag `vX.Y.Z` | Signed APK/AAB, GitHub Release, provenance, optional Google Play upload ([PLAY_STORE.md](PLAY_STORE.md)) |
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
