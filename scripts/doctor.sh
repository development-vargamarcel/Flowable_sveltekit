#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
ensure_standard_dirs
install_error_trap

log_section "Environment diagnostics"

validate_required_commands() {
  require_cmd git
  require_cmd node
  require_cmd npm
  require_cmd java
  require_executable_file "$BPM_BACKEND_DIR/mvnw"
}

validate_metadata_files() {
  require_file "$BPM_BACKEND_DIR/pom.xml"
  check_lockfiles
}

validate_script_executability() {
  local script
  for script in "$ROOT_DIR"/scripts/*.sh; do
    require_executable_file "$script"
  done
}

print_repo_metadata() {
  log_info "Repository: $ROOT_DIR"
  log_info "Branch: $(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
  log_info "Commit: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
  log_info "Run ID: $RUN_ID"
}

print_toolchain_versions() {
  log_info "Node: $(node --version)"
  log_info "npm: $(npm_safe --version)"
  log_info "Java: $(java --version 2>&1 | head -n 1)"
  log_info "Maven Wrapper: $($BPM_BACKEND_DIR/mvnw --version 2>/dev/null | head -n 1)"
}

print_system_diagnostics() {
  log_info "OS: $(uname -s)"
  log_info "Kernel: $(uname -r)"
  log_info "Architecture: $(uname -m)"
  if command -v nproc >/dev/null 2>&1; then
    log_info "CPU cores: $(nproc)"
  fi
  if command -v free >/dev/null 2>&1; then
    log_info "Memory: $(free -h | awk 'NR==2 {print $2 " total / " $7 " available"}')"
  fi
  if command -v df >/dev/null 2>&1; then
    log_info "Disk (root): $(df -h / | awk 'NR==2 {print $4 " free of " $2}')"
  fi
}

validate_npm_lock_consistency() {
  (
    cd "$BPM_FRONTEND_DIR"
    npm_safe ls --package-lock-only >/dev/null
  )
  log_info "Frontend lockfile consistency check passed"
}

print_runtime_guidance() {
  if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    log_info "Preferred backend JDK: /usr/lib/jvm/java-21-openjdk-amd64"
  else
    log_warn "Preferred backend JDK not detected; continuing with active JAVA_HOME/runtime"
  fi

  if [ "$BPM_RUNNER_SUMMARY_FORMAT" = "json" ]; then
    log_info "JSON summary format enabled; artifacts will be saved to: $BPM_RUNNER_ARTIFACTS_DIR"
  fi

  log_info "Environment diagnostics completed successfully."
}

run_step "Validate required commands" validate_required_commands
run_step "Validate project metadata files" validate_metadata_files
run_step "Validate script executability" validate_script_executability
run_step "Print repository metadata" print_repo_metadata
run_step "Print toolchain versions" print_toolchain_versions
run_step "Print system diagnostics" print_system_diagnostics
run_step "Validate frontend lockfile consistency" validate_npm_lock_consistency
run_step "Print runtime guidance" print_runtime_guidance

print_summary
