#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
start="$(start_timer)"
cd "$ROOT_DIR/frontend"

log_section "Frontend verification"
log_info "Running formatting check"
npm_safe run format:check

log_info "Running lint checks"
npm_safe run lint

log_info "Running Svelte type checks"
npm_safe run check

log_info "Running unit tests"
npm_safe run test:ci

log_info "Running production build"
npm_safe run build

log_info "Frontend verification completed in $(elapsed_seconds "$start")s"
