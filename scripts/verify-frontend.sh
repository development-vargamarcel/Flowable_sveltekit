#!/usr/bin/env bash
set -euo pipefail

# Frontend verification includes formatting, lint, type checks, tests, and production build.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

npm run format:check
npm run lint
npm run check
npm run test:ci
npm run build

echo "✅ Frontend verification completed."
