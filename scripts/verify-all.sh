#!/usr/bin/env bash
set -euo pipefail

# Single-entrypoint quality gate script for local and CI usage.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT_DIR/scripts/doctor.sh"
"$ROOT_DIR/scripts/verify-frontend.sh"
"$ROOT_DIR/scripts/verify-backend.sh"

echo "✅ Full-stack verification completed."
