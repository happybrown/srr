#!/usr/bin/env bash
# 로컬 서명 키(keystore.properties, keystore/upload.jks)를 GitHub Actions 시크릿으로 등록한다.
# 한 번만 실행하면 된다. 먼저 `gh auth login`으로 로그인되어 있어야 한다.
#
#   scripts/upload-signing-secrets.sh            # happybrown/srr
#   scripts/upload-signing-secrets.sh owner/repo
set -euo pipefail
cd "$(dirname "$0")/.."

REPO="${1:-happybrown/srr}"
PROPS="keystore.properties"
[ -f "$PROPS" ] || { echo "$PROPS 이 없습니다." >&2; exit 1; }

prop() { grep "^$1=" "$PROPS" | head -1 | cut -d= -f2-; }

STORE_FILE="$(prop storeFile)"
[ -f "$STORE_FILE" ] || { echo "키 파일이 없습니다: $STORE_FILE" >&2; exit 1; }

base64 < "$STORE_FILE" | tr -d '\n' | gh secret set KEYSTORE_BASE64 -R "$REPO"
printf %s "$(prop storePassword)" | gh secret set KEYSTORE_PASSWORD -R "$REPO"
printf %s "$(prop keyAlias)"      | gh secret set KEY_ALIAS -R "$REPO"
printf %s "$(prop keyPassword)"   | gh secret set KEY_PASSWORD -R "$REPO"

echo "✓ $REPO 에 서명 시크릿 4개를 등록했습니다."
gh secret list -R "$REPO"
