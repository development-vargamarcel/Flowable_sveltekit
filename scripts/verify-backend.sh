#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

: "${BPM_BACKEND_SKIP_TESTS:=0}"
: "${BPM_BACKEND_SKIP_PACKAGE:=0}"
: "${BPM_BACKEND_ENABLE_VERIFY:=0}"

ensure_repo_root
ensure_standard_dirs
install_error_trap

start="$(start_timer)"
cd "$BPM_BACKEND_DIR"

configure_java_home() {
  if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    export PATH="$JAVA_HOME/bin:$PATH"
    log_info "Using JAVA_HOME=$JAVA_HOME"
  else
    log_warn "Java 21 path not found. Falling back to active Java runtime."
  fi
}

backend_tests() { ./mvnw -B test; }
backend_package() { ./mvnw -B -DskipTests package; }
backend_verify() { ./mvnw -B -DskipTests verify; }

log_section "Backend verification"
run_step "Configure backend Java runtime" configure_java_home

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
