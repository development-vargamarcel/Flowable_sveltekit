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
  require_cmd "$BPM_BACKEND_DIR/mvnw"
}

validate_metadata_files() {
  require_file "$BPM_BACKEND_DIR/pom.xml"
  check_lockfiles
}

print_repo_metadata() {
  log_info "Repository: $ROOT_DIR"
  log_info "Branch: $(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
  log_info "Commit: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
}

print_toolchain_versions() {
  log_info "Node: $(node --version)"
  log_info "npm: $(npm_safe --version)"
  log_info "Java: $(java --version 2>&1 | head -n 1)"
  log_info "Maven Wrapper: $($BPM_BACKEND_DIR/mvnw --version 2>/dev/null | head -n 1)"
}

print_runtime_guidance() {
  if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    log_info "Preferred backend JDK: /usr/lib/jvm/java-21-openjdk-amd64"
  else
    log_warn "Preferred backend JDK not detected; continuing with active JAVA_HOME/runtime"
  fi
  log_info "Environment diagnostics completed successfully."
}

run_step "Validate required commands" validate_required_commands
run_step "Validate project metadata files" validate_metadata_files
run_step "Print repository metadata" print_repo_metadata
run_step "Print toolchain versions" print_toolchain_versions
run_step "Print runtime guidance" print_runtime_guidance

print_summary
