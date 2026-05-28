#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_BACKEND_SKIP_TESTS:=0}"
: "${BPM_BACKEND_SKIP_PACKAGE:=0}"
: "${BPM_BACKEND_ENABLE_VERIFY:=0}"

ensure_repo_root
ensure_standard_dirs
install_error_trap
ensure_git_clean_if_required

validate_toggle "$BPM_BACKEND_SKIP_TESTS" "BPM_BACKEND_SKIP_TESTS"
validate_toggle "$BPM_BACKEND_SKIP_PACKAGE" "BPM_BACKEND_SKIP_PACKAGE"
validate_toggle "$BPM_BACKEND_ENABLE_VERIFY" "BPM_BACKEND_ENABLE_VERIFY"

start="$(start_timer)"
cd "$BPM_BACKEND_DIR"

configure_java_runtime() {
  local java_version
  java_version="$(java -version 2>&1 | head -n 1 || true)"
  log_info "Using active Java runtime: ${java_version:-unknown}. Backend source/target compatibility is Java 17."
}

backend_tests() { ./mvnw -B test; }
backend_package() { ./mvnw -B -DskipTests package; }
backend_verify() { ./mvnw -B -DskipTests verify; }

log_section "Backend verification"
run_step "Confirm backend Java runtime" configure_java_runtime

if [ "$BPM_BACKEND_SKIP_TESTS" = "0" ]; then
  run_step "Backend Maven test suite" backend_tests
else
  record_step "Backend Maven test suite" "SKIP" "0" "BPM_BACKEND_SKIP_TESTS=1"
fi

if [ "$BPM_BACKEND_SKIP_PACKAGE" = "0" ]; then
  run_step "Backend packaging sanity check" backend_package
else
  record_step "Backend packaging sanity check" "SKIP" "0" "BPM_BACKEND_SKIP_PACKAGE=1"
fi

if [ "$BPM_BACKEND_ENABLE_VERIFY" = "1" ]; then
  # Optional deep validation stage for CI pipelines that need full lifecycle checks.
  run_step "Backend Maven verify stage" backend_verify
fi

log_info "Backend verification completed in $(elapsed_seconds "$start")s"
print_summary
