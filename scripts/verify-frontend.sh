#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

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

run_step "Frontend formatting check" frontend_format_check
run_step "Frontend lint checks" frontend_lint
run_step "Frontend Svelte type checks" frontend_type_check
run_step "Frontend unit tests" frontend_unit_tests
run_step "Frontend production build" frontend_build

log_info "Frontend verification completed in $(elapsed_seconds "$start")s"
print_summary
