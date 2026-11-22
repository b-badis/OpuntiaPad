#!/usr/bin/env bash
# Creates the release signing key for OpuntiaPad and prints the values to store as
# GitHub Actions secrets. Run it ONCE, keep the .jks file and passwords somewhere safe
# (password manager / offline backup): losing them means users can't update the app.
set -euo pipefail

OUT="${1:-opuntiapad-release.jks}"
ALIAS="${KEY_ALIAS:-opuntiapad}"

if [ -e "$OUT" ]; then
  echo "error: $OUT already exists — refusing to overwrite a signing key." >&2
  exit 1
fi
command -v keytool >/dev/null || { echo "error: keytool not found (install a JDK)." >&2; exit 1; }

read -r -s -p "Keystore password (min. 6 chars): " STORE_PASS; echo
read -r -s -p "Confirm password: " STORE_PASS2; echo
[ "$STORE_PASS" = "$STORE_PASS2" ] || { echo "error: passwords differ" >&2; exit 1; }
read -r -p "Your name or organisation (certificate CN): " CN

keytool -genkeypair -v \
  -keystore "$OUT" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
  -dname "CN=${CN}, O=OpuntiaPad"

echo
echo "✓ Keystore created: $OUT"
echo
echo "Add these repository secrets (Settings → Secrets and variables → Actions),"
echo "or run the following commands with the GitHub CLI:"
echo
echo "  base64 -w0 \"$OUT\" | gh secret set KEYSTORE_BASE64"
echo "  gh secret set KEYSTORE_PASSWORD   # paste the keystore password"
echo "  gh secret set KEY_PASSWORD        # same password (PKCS12 uses one password)"
echo "  gh secret set KEY_ALIAS --body \"$ALIAS\""
echo
echo "Never commit $OUT. It is already listed in .gitignore."
