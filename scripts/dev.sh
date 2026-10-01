#!/bin/sh

# Monorepo内の全アプリへ同じローカル環境変数を渡して起動します。
set -eu

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

exec node node_modules/concurrently/dist/bin/concurrently.js \
  -k \
  -n core-api,speech,web \
  -c blue,magenta,green \
  "./gradlew :core-api:bootRun" \
  "./gradlew :speech-service:bootRun" \
  "npm run dev --prefix apps/web"
