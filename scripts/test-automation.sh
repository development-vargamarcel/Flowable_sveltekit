#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
ensure_standard_dirs
install_error_trap
ensure_git_clean_if_required

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
  local summary_file
  summary_file="$(find "$artifacts_dir" -name 'summary-*.json' -print -quit)"
  test -n "$summary_file"
  node -e '
    const fs = require("node:fs");
    const payload = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    if (!payload.overallStatus || !payload.totals || typeof payload.totals.pass !== "number") {
      process.exit(1);
    }
  ' "$summary_file"
}


summary_markdown_smoke_test() {
  local artifacts_dir
  artifacts_dir="$(mktemp -d)"
  BPM_RUNNER_SUMMARY_FORMAT=markdown BPM_RUNNER_ARTIFACTS_DIR="$artifacts_dir" "$ROOT_DIR/scripts/doctor.sh" >/dev/null
  test -n "$(find "$artifacts_dir" -name 'summary-*.md' -print -quit)"
}

artifact_retention_smoke_test() {
  local artifacts_dir stale_file
  artifacts_dir="$(mktemp -d)"
  stale_file="$artifacts_dir/stale.txt"
  printf 'old\n' >"$stale_file"
  touch -d '20 days ago' "$stale_file"
  BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_ARTIFACTS_DIR="$artifacts_dir" BPM_RUNNER_ARTIFACT_RETENTION_DAYS=7 "$ROOT_DIR/scripts/doctor.sh" >/dev/null
  test ! -f "$stale_file"
}

clean_git_guard_smoke_test() {
  # Repository is expected to be dirty during local development; guard should fail in that case.
  if (cd "$ROOT_DIR" && BPM_RUNNER_REQUIRE_CLEAN_GIT=1 "$ROOT_DIR/scripts/doctor.sh" >/dev/null 2>&1); then
    log_error "Expected clean-git guard to fail on dirty working tree"
    return 1
  fi
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
run_step "Verify Markdown summary renderer" summary_markdown_smoke_test
run_step "Verify artifact retention pruning" artifact_retention_smoke_test
run_step "Verify clean-git guard behavior" clean_git_guard_smoke_test
run_step "Verify retry helper behavior" retry_helper_smoke_test

print_summary
