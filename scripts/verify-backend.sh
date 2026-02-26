#!/usr/bin/env bash
set -euo pipefail

# Backend verification runs the full Maven test lifecycle with explicit batch output.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/backend"

# Prefer JDK 21 when available to avoid Mockito/ByteBuddy incompatibilities on newer JDKs.
if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
  export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

./mvnw -B test

echo "✅ Backend verification completed."
