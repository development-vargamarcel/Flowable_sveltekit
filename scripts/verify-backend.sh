#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

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

log_section "Backend verification"
run_step "Configure backend Java runtime" configure_java_home
run_step "Backend Maven test suite" backend_tests
run_step "Backend packaging sanity check" backend_package

log_info "Backend verification completed in $(elapsed_seconds "$start")s"
print_summary
