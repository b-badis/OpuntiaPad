# Publishing OpuntiaPad on Google Play

Once set up, **every tag pushed (`vX.Y.Z`) is uploaded to Google Play automatically** by the release workflow,
in addition to the GitHub Release. Without the Play secret, that step is simply skipped.

```
git tag v1.2.0 ─► GitHub Actions ─► tests ─► signed AAB ─► GitHub Release
                                                       └─► Google Play (internal → closed → production)
```

## 1. One-time setup (≈ 1 hour, mostly in the Play Console)

1. **Developer account** — <https://play.google.com/console/signup> (one-time US$25 fee, identity verification).
   New *personal* accounts must run a **closed test with at least 12 testers for 14 days** before they can publish
   to production; plan for it.
2. **Create the app** in the Play Console: name *OpuntiaPad*, free, app (not game).
   The API cannot create apps, and **the very first AAB must be uploaded manually**:
   download `opuntiapad-x.y.z.aab` from a GitHub Release and upload it to *Testing → Internal testing*.
   This also registers the package name `io.github.bbadis.opuntiapad`.
3. **Play App Signing** (default): keep it. Your `opuntiapad-release.jks` becomes the *upload key*; Google holds
   the app signing key. (If you ever lose the upload key, Google can reset it — unlike for GitHub/F-Droid APKs.)
4. **Store listing** — texts and graphics are ready in `fastlane/metadata/android/`:
   `title.txt`, `short_description.txt`, `full_description.txt`, `images/icon.png` (512×512),
   `images/featureGraphic.png` (1024×500). Add 2–8 phone screenshots.
5. **App content** declarations:
   - Privacy policy URL: `https://github.com/b-badis/OpuntiaPad/blob/main/PRIVACY.md`
   - Data safety: *No data collected, no data shared*.
   - Ads: *No*. Target audience: 18+ (or 13+), not designed for children. Content rating questionnaire: *Utility*.
6. **Service account for CI**:
   1. In Google Cloud Console, create a project (or reuse one) and enable the **Google Play Android Developer API**.
   2. *IAM → Service accounts → Create*, then *Keys → Add key → JSON*; download the file.
   3. In Play Console → *Users and permissions → Invite new users*, invite the service-account e-mail with the
      permissions *Release apps to testing tracks* (+ *Release to production…* if you want CI to publish to production),
      limited to OpuntiaPad.
7. **GitHub configuration** (*Settings → Secrets and variables → Actions*):

   | Kind | Name | Value |
   |---|---|---|
   | Secret | `PLAY_SERVICE_ACCOUNT_JSON` | the whole JSON key file content |
   | Variable | `PLAY_TRACK` | `internal` (default), `alpha` (closed testing), `beta` (open testing) or `production` |
   | Variable | `PLAY_RELEASE_STATUS` | `draft` **until the app has been published once**, then `completed` (or `inProgress` for a staged rollout) |
   | Variable | `PLAY_ROLLOUT_FRACTION` | e.g. `0.2` for 20 % — only with `PLAY_RELEASE_STATUS=inProgress` |

   ```bash
   gh secret set PLAY_SERVICE_ACCOUNT_JSON < play-service-account.json
   gh variable set PLAY_TRACK --body internal
   gh variable set PLAY_RELEASE_STATUS --body draft
   ```
   Then delete the JSON file from your computer (keep a copy in a password manager if needed).

## 2. Releasing

1. Update `CHANGELOG.md` (the section for the version becomes both the GitHub notes and Play's *What's new*,
   truncated to 500 characters).
2. `git tag v1.2.0 && git push origin v1.2.0`
3. The workflow uploads the AAB with its R8 mapping file (readable crash reports in Play Vitals):
   - `vX.Y.Z-beta.N` / `-rc.N` → always the **internal** track;
   - `vX.Y.Z` → the track in `PLAY_TRACK`.
4. Promote between tracks (internal → closed → production) in the Play Console, or set `PLAY_TRACK=production`
   once you trust the pipeline. Use a staged rollout (`inProgress` + `PLAY_ROLLOUT_FRACTION`) for big changes.

## Troubleshooting

| Error | Fix |
|---|---|
| `Package not found` | Upload the first AAB manually (step 2) and check the package name. |
| `Only releases with status draft may be created on draft app` | Set `PLAY_RELEASE_STATUS=draft` until the first manual publication. |
| `APK specifies a version code that has already been used` | Each tag must be a new, higher version (`versionCode` is derived from the tag). |
| `The caller does not have permission` | Check the service account's permissions in Play Console → Users and permissions. |
| Upload key mismatch | The AAB must be signed with the upload key registered in Play App Signing. |
