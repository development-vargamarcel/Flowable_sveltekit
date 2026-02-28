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

summary_json_smoke_test() {
  local artifacts_dir
  artifacts_dir="$(mktemp -d)"
  BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_ARTIFACTS_DIR="$artifacts_dir" "$ROOT_DIR/scripts/doctor.sh" >/dev/null
  test -n "$(find "$artifacts_dir" -name 'summary-*.json' -print -quit)"
}

retry_helper_smoke_test() {
  local counter_file
  counter_file="$(mktemp)"
  printf '0' >"$counter_file"

  flake_once() {
    local count
    count="$(cat "$counter_file")"
    count=$((count + 1))
    printf '%s' "$count" >"$counter_file"
    if [ "$count" -lt 2 ]; then
      return 1
    fi
    return 0
  }

  run_with_retry 2 1 flake_once
}

run_step "Parse-check automation scripts" common_shellcheck
run_step "Verify dry-run orchestration mode" dry_run_verify_all
run_step "Verify JSON summary renderer" summary_json_smoke_test
run_step "Verify retry helper behavior" retry_helper_smoke_test

print_summary
