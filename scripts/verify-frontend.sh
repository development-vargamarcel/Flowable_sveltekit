#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_FRONTEND_SKIP_FORMAT:=0}"
: "${BPM_FRONTEND_SKIP_LINT:=0}"
: "${BPM_FRONTEND_SKIP_TYPECHECK:=0}"
: "${BPM_FRONTEND_SKIP_TESTS:=0}"
: "${BPM_FRONTEND_SKIP_BUILD:=0}"
: "${BPM_FRONTEND_ENABLE_COVERAGE:=0}"

ensure_repo_root
ensure_standard_dirs
install_error_trap

start="$(start_timer)"
cd "$BPM_FRONTEND_DIR"

log_section "Frontend verification"

frontend_format_check() { npm_safe run format:check; }
frontend_lint() { npm_safe run lint; }
frontend_type_check() { npm_safe run check; }
frontend_unit_tests() { npm_safe run test:ci; }
frontend_build() { npm_safe run build; }
frontend_coverage() { npm_safe run test:coverage; }

if [ "$BPM_FRONTEND_SKIP_FORMAT" = "0" ]; then
  run_step "Frontend formatting check" frontend_format_check
else
  record_step "Frontend formatting check" "SKIP" "0" "BPM_FRONTEND_SKIP_FORMAT=1"
fi

if [ "$BPM_FRONTEND_SKIP_LINT" = "0" ]; then
  run_step "Frontend lint checks" frontend_lint
else
  record_step "Frontend lint checks" "SKIP" "0" "BPM_FRONTEND_SKIP_LINT=1"
fi

if [ "$BPM_FRONTEND_SKIP_TYPECHECK" = "0" ]; then
  run_step "Frontend Svelte type checks" frontend_type_check
else
  record_step "Frontend Svelte type checks" "SKIP" "0" "BPM_FRONTEND_SKIP_TYPECHECK=1"
fi

if [ "$BPM_FRONTEND_SKIP_TESTS" = "0" ]; then
  run_step "Frontend unit tests" frontend_unit_tests
else
  record_step "Frontend unit tests" "SKIP" "0" "BPM_FRONTEND_SKIP_TESTS=1"
fi

if [ "$BPM_FRONTEND_SKIP_BUILD" = "0" ]; then
  run_step "Frontend production build" frontend_build
else
  record_step "Frontend production build" "SKIP" "0" "BPM_FRONTEND_SKIP_BUILD=1"
fi

if [ "$BPM_FRONTEND_ENABLE_COVERAGE" = "1" ]; then
  # Coverage can be expensive, so it is opt-in and intentionally separate from the default verification flow.
  run_step "Frontend coverage report" frontend_coverage
fi

log_info "Frontend verification completed in $(elapsed_seconds "$start")s"
print_summary
