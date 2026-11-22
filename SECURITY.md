# Security Policy

## Supported versions

Only the [latest release](https://github.com/b-badis/OpuntiaPad/releases/latest) receives security fixes.

## Reporting a vulnerability

Please **do not open a public issue**. Report it privately via
[GitHub Security Advisories](https://github.com/b-badis/OpuntiaPad/security/advisories/new).
You should get an answer within 7 days. Please include steps to reproduce and the affected version.

## Design notes

- OpuntiaPad requests **no permissions** (not even INTERNET). Files are reached only through URI grants given by
  the system file picker or by the app that shares/opens a file.
- `file://` URIs pointing into OpuntiaPad's private storage are refused.
- Release APKs are signed with a key kept outside the repository and every release ships SHA-256 checksums
  and a signed [build provenance attestation](https://docs.github.com/actions/security-guides/using-artifact-attestations-to-establish-provenance-for-builds):

  ```bash
  gh attestation verify opuntiapad-x.y.z.apk --repo b-badis/OpuntiaPad
  ```
