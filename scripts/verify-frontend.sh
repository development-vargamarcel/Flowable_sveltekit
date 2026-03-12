#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_FRONTEND_SKIP_FORMAT:=0}"
: "${BPM_FRONTEND_SKIP_LINT:=0}"
: "${BPM_FRONTEND_SKIP_TYPECHECK:=0}"
: "${BPM_FRONTEND_SKIP_TESTS:=0}"
: "${BPM_FRONTEND_SKIP_BUILD:=0}"
: "${BPM_FRONTEND_ENABLE_COVERAGE:=0}"
: "${BPM_FRONTEND_ENABLE_BROWSER_SMOKE:=0}"
: "${BPM_FRONTEND_LINT_MAX_WARNINGS:=-1}"

ensure_repo_root
ensure_standard_dirs
install_error_trap
ensure_git_clean_if_required

validate_toggle "$BPM_FRONTEND_SKIP_FORMAT" "BPM_FRONTEND_SKIP_FORMAT"
validate_toggle "$BPM_FRONTEND_SKIP_LINT" "BPM_FRONTEND_SKIP_LINT"
validate_toggle "$BPM_FRONTEND_SKIP_TYPECHECK" "BPM_FRONTEND_SKIP_TYPECHECK"
validate_toggle "$BPM_FRONTEND_SKIP_TESTS" "BPM_FRONTEND_SKIP_TESTS"
validate_toggle "$BPM_FRONTEND_SKIP_BUILD" "BPM_FRONTEND_SKIP_BUILD"
validate_toggle "$BPM_FRONTEND_ENABLE_COVERAGE" "BPM_FRONTEND_ENABLE_COVERAGE"
validate_toggle "$BPM_FRONTEND_ENABLE_BROWSER_SMOKE" "BPM_FRONTEND_ENABLE_BROWSER_SMOKE"

if [ "$BPM_FRONTEND_LINT_MAX_WARNINGS" != "-1" ]; then
  if ! [[ "$BPM_FRONTEND_LINT_MAX_WARNINGS" =~ ^[0-9]+$ ]]; then
    log_error "Invalid BPM_FRONTEND_LINT_MAX_WARNINGS '$BPM_FRONTEND_LINT_MAX_WARNINGS'. Expected -1 or a non-negative integer."
    exit 1
  fi
fi

start="$(start_timer)"
cd "$BPM_FRONTEND_DIR"

log_section "Frontend verification"

frontend_format_check() { npm_safe run format:check; }
frontend_lint() {
  # Allow teams to progressively tighten lint quality gates without changing package scripts.
  if [ "$BPM_FRONTEND_LINT_MAX_WARNINGS" -ge 0 ]; then
    npm_safe run lint -- --max-warnings "$BPM_FRONTEND_LINT_MAX_WARNINGS"
    return
  fi
  npm_safe run lint
}
frontend_type_check() { npm_safe run check; }
frontend_unit_tests() { npm_safe run test:ci; }
frontend_build() {
  # Keep CI output clean and deterministic when Sentry auth is not configured.
  SENTRY_TELEMETRY=0 SENTRY_VITE_PLUGIN_TELEMETRY=0 npm_safe run build
}
frontend_coverage() { npm_safe run test:coverage; }
frontend_browser_console_smoke() { npm_safe run test:browser-console; }

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

if [ "$BPM_FRONTEND_ENABLE_BROWSER_SMOKE" = "1" ]; then
  # Optional browser-level smoke test that fails on uncaught client runtime/console errors.
  run_step "Frontend browser console smoke check" frontend_browser_console_smoke
fi

log_info "Frontend verification completed in $(elapsed_seconds "$start")s"
print_summary
