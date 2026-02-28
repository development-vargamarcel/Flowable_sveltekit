#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
ensure_standard_dirs
install_error_trap

start="$(start_timer)"

bootstrap_frontend() {
  cd "$BPM_FRONTEND_DIR"
  log_info "Installing frontend dependencies with npm ci"
  run_with_retry "$BPM_RUNNER_RETRY_COUNT" "$BPM_RUNNER_RETRY_DELAY_SECONDS" npm_safe ci
}

bootstrap_backend() {
  cd "$BPM_BACKEND_DIR"
  log_info "Resolving backend Maven dependencies for offline use"
  run_with_retry "$BPM_RUNNER_RETRY_COUNT" "$BPM_RUNNER_RETRY_DELAY_SECONDS" ./mvnw -B -q -DskipTests dependency:go-offline
}

log_section "Dependency bootstrap"
run_step "Frontend dependency bootstrap" bootstrap_frontend
run_step "Backend dependency bootstrap" bootstrap_backend

log_info "Bootstrap complete in $(elapsed_seconds "$start")s"
print_summary
