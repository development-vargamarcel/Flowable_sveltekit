#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
start="$(start_timer)"

log_section "Repository verification"
"$ROOT_DIR/scripts/doctor.sh"
"$ROOT_DIR/scripts/verify-frontend.sh"
"$ROOT_DIR/scripts/verify-backend.sh"

log_info "Full-stack verification completed in $(elapsed_seconds "$start")s"
