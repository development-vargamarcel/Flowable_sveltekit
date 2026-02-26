#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "🔧 Installing frontend dependencies (npm ci)..."
cd "$ROOT_DIR/frontend"
npm ci

echo "🔧 Resolving backend Maven dependencies for offline usage..."
cd "$ROOT_DIR/backend"
./mvnw -B -q -DskipTests dependency:go-offline

echo "✅ Bootstrap complete: frontend and backend dependencies are installed."
