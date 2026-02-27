#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
ensure_standard_dirs
install_error_trap

log_section "Automation smoke tests"

common_shellcheck() {
  # Parse-check all automation scripts with bash -n to catch syntax regressions quickly.
  local script
  for script in "$ROOT_DIR"/scripts/*.sh; do
    bash -n "$script"
  done
}

dry_run_verify_all() {
  BPM_RUNNER_DRY_RUN=1 "$ROOT_DIR/scripts/verify-all.sh" >/dev/null
}

summary_smoke_test() {
  # Validate that summary reporting remains callable when no steps are present.
  local previous_summary_setting="$BPM_RUNNER_SUMMARY"
  BPM_RUNNER_SUMMARY=1
  print_summary >/dev/null
  BPM_RUNNER_SUMMARY="$previous_summary_setting"
}

run_step "Parse-check automation scripts" common_shellcheck
run_step "Verify dry-run orchestration mode" dry_run_verify_all
run_step "Verify summary renderer" summary_smoke_test

print_summary
