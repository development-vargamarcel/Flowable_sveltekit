#!/usr/bin/env bash
set -euo pipefail

# Quick environment diagnostics used before running heavy quality gates.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf 'Repository: %s\n' "$ROOT_DIR"
printf 'Node: %s\n' "$(node --version)"
printf 'npm: %s\n' "$(npm --version)"
printf 'Java: %s\n' "$(java --version 2>&1 | head -n 1)"
if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
  printf 'Preferred backend JDK: %s\n' "/usr/lib/jvm/java-21-openjdk-amd64"
fi
printf 'Maven Wrapper: %s\n' "$("$ROOT_DIR/backend/mvnw" --version 2>/dev/null | head -n 1)"

echo "✅ Environment diagnostics completed."
