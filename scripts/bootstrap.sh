#!/usr/bin/env bash
set -euo pipefail

# Bootstrap all local dependencies for both frontend and backend workstreams.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR/frontend"
npm install

cd "$ROOT_DIR/backend"
./mvnw -q -DskipTests dependency:go-offline

echo "✅ Bootstrap complete: frontend npm deps and backend Maven deps are installed."
