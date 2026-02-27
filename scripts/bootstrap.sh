#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
start="$(start_timer)"

log_section "Frontend dependency bootstrap"
cd "$ROOT_DIR/frontend"
log_info "Installing frontend dependencies with npm ci"
npm_safe ci

log_section "Backend dependency bootstrap"
cd "$ROOT_DIR/backend"
log_info "Resolving backend Maven dependencies for offline use"
./mvnw -B -q -DskipTests dependency:go-offline

log_info "Bootstrap complete in $(elapsed_seconds "$start")s"
