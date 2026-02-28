#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_VERIFY_INCLUDE_BOOTSTRAP:=0}"

ensure_repo_root
ensure_standard_dirs
install_error_trap

start="$(start_timer)"

verify_bootstrap() { "$ROOT_DIR/scripts/bootstrap.sh"; }
verify_doctor() { "$ROOT_DIR/scripts/doctor.sh"; }
verify_frontend() { "$ROOT_DIR/scripts/verify-frontend.sh"; }
verify_backend() { "$ROOT_DIR/scripts/verify-backend.sh"; }

log_section "Repository verification"

if [ "$BPM_VERIFY_INCLUDE_BOOTSTRAP" = "1" ]; then
  run_step "Run dependency bootstrap" verify_bootstrap
fi
run_step "Run diagnostics" verify_doctor
run_step "Run frontend verification" verify_frontend
run_step "Run backend verification" verify_backend

log_info "Full-stack verification completed in $(elapsed_seconds "$start")s"
if [ -n "$RUN_LOG_FILE" ]; then
  log_info "Run log file: $RUN_LOG_FILE"
fi
print_summary
