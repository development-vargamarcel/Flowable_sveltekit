#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

echo "🔎 Running frontend format, lint, type-check, unit tests, and production build..."
npm run format:check
npm run lint
npm run check
npm run test:ci
npm run build

echo "✅ Frontend verification completed."
