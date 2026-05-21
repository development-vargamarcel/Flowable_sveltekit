#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_VERIFY_INCLUDE_BOOTSTRAP:=0}"

ensure_repo_root
ensure_standard_dirs
install_error_trap
ensure_git_clean_if_required

validate_toggle "$BPM_VERIFY_INCLUDE_BOOTSTRAP" "BPM_VERIFY_INCLUDE_BOOTSTRAP"

start="$(start_timer)"

verify_bootstrap() { "$ROOT_DIR/scripts/bootstrap.sh"; }
verify_doctor() { "$ROOT_DIR/scripts/doctor.sh"; }
verify_frontend() { "$ROOT_DIR/scripts/verify-frontend.sh"; }
verify_backend() { "$ROOT_DIR/scripts/verify-backend.sh"; }
verify_automation() { "$ROOT_DIR/scripts/test-automation.sh"; }

run_full_stack() {
  log_section "Repository verification"

  if [ "$BPM_VERIFY_INCLUDE_BOOTSTRAP" = "1" ]; then
    run_step "Run dependency bootstrap" verify_bootstrap
  fi
  run_step "Run diagnostics" verify_doctor
  run_step "Run frontend verification" verify_frontend
  run_step "Run backend verification" verify_backend
}

run_subcommand() {
  local command="${1:-full}"
  case "$command" in
    full)
      run_full_stack
      ;;
    frontend)
      log_section "Repository verification (frontend)"
      run_step "Run frontend verification" verify_frontend
      ;;
    backend)
      log_section "Repository verification (backend)"
      run_step "Run backend verification" verify_backend
      ;;
    doctor)
      log_section "Repository verification (diagnostics)"
      run_step "Run diagnostics" verify_doctor
      ;;
    automation)
      log_section "Repository verification (automation)"
      run_step "Run automation smoke tests" verify_automation
      ;;
    bootstrap)
      log_section "Repository verification (bootstrap)"
      run_step "Run dependency bootstrap" verify_bootstrap
      ;;
    *)
      log_error "Unknown verify-all subcommand '$command'. Expected one of: full, frontend, backend, doctor, automation, bootstrap"
      exit 1
      ;;
  esac
}

run_subcommand "${1:-full}"

log_info "Verification command completed in $(elapsed_seconds "$start")s"
if [ -n "$RUN_LOG_FILE" ]; then
  log_info "Run log file: $RUN_LOG_FILE"
fi
print_summary
