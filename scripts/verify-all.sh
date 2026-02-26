#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
START_TS="$(date +%s)"

"$ROOT_DIR/scripts/doctor.sh"
"$ROOT_DIR/scripts/verify-frontend.sh"
"$ROOT_DIR/scripts/verify-backend.sh"

END_TS="$(date +%s)"
DURATION="$((END_TS - START_TS))"

echo "✅ Full-stack verification completed in ${DURATION}s."
