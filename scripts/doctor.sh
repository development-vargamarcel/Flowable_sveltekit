#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "❌ Missing required command: $cmd" >&2
    return 1
  fi
}

require_cmd node
require_cmd npm
require_cmd java

printf 'Repository: %s\n' "$ROOT_DIR"
printf 'Node: %s\n' "$(node --version)"
printf 'npm: %s\n' "$(npm --version)"
printf 'Java: %s\n' "$(java --version 2>&1 | head -n 1)"

if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
  printf 'Preferred backend JDK: %s\n' "/usr/lib/jvm/java-21-openjdk-amd64"
else
  printf 'Preferred backend JDK: %s\n' "not detected (using active JAVA_HOME/runtime)"
fi

printf 'Maven Wrapper: %s\n' "$("$ROOT_DIR/backend/mvnw" --version 2>/dev/null | head -n 1)"
echo "✅ Environment diagnostics completed."
