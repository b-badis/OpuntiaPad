# Releasing OpuntiaPad

Releases are built, signed and published by [`.github/workflows/release.yml`](../.github/workflows/release.yml)
whenever a tag `vX.Y.Z` (or `vX.Y.Z-beta.N` / `-alpha.N` / `-rc.N` for a pre-release) is pushed.

## One-time setup: the signing key

Android only installs updates signed with the **same key**, so create it once and keep it safe.

```bash
./scripts/generate-keystore.sh            # creates opuntiapad-release.jks (git-ignored)
```

Then add four repository secrets (*Settings → Secrets and variables → Actions → New repository secret*),
or with the GitHub CLI:

```bash
base64 -w0 opuntiapad-release.jks | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_PASSWORD
gh secret set KEY_ALIAS --body opuntiapad
```

> ⚠️ Back up `opuntiapad-release.jks` and its password (password manager + offline copy).
> If they are lost, existing users will have to uninstall the app to install new versions.

To sign locally, copy `keystore.properties.example` to `keystore.properties` and run `./gradlew assembleRelease`.

## Publishing a version

1. Move the `[Unreleased]` entries of `CHANGELOG.md` to a new `## [X.Y.Z] - YYYY-MM-DD` section
   (this text becomes the release notes) and commit to `main`.
2. Tag and push:

   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

3. The workflow then:
   - derives `versionName` (`X.Y.Z`) and a monotonic `versionCode` from the tag
     (`X·1000000 + Y·10000 + Z·100 + N`, where `N` is the pre-release number or `99` for a final release);
   - runs the unit tests, builds the signed **APK** and **AAB**, verifies the signature;
   - publishes a GitHub Release with the APK, AAB, `SHA256SUMS.txt`, the changelog section and
     auto-generated notes, plus a signed build-provenance attestation.

Version numbers: `MINOR` and `PATCH` must stay ≤ 99.

## Optional next steps

- **F-Droid**: the app has no proprietary dependencies, and store texts live in `fastlane/metadata/android`,
  the format F-Droid reads. Open a request at <https://gitlab.com/fdroid/rfp>.
- **Google Play**: fully automated once configured — see [PLAY_STORE.md](PLAY_STORE.md).
