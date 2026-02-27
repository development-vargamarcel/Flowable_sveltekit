#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
log_section "Environment diagnostics"

for cmd in git node npm java; do
  require_cmd "$cmd"
done

require_cmd "$ROOT_DIR/backend/mvnw"

log_info "Repository: $ROOT_DIR"
log_info "Branch: $(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
log_info "Commit: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
log_info "Node: $(node --version)"
log_info "npm: $(npm_safe --version)"
log_info "Java: $(java --version 2>&1 | head -n 1)"

if [ -f "$ROOT_DIR/frontend/package-lock.json" ]; then
  log_info "Frontend lockfile: present"
else
  log_error "Frontend lockfile missing at frontend/package-lock.json"
  exit 1
fi

if [ -f "$ROOT_DIR/backend/pom.xml" ]; then
  log_info "Backend Maven descriptor: present"
else
  log_error "Backend pom.xml missing"
  exit 1
fi

if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
  log_info "Preferred backend JDK: /usr/lib/jvm/java-21-openjdk-amd64"
else
  log_info "Preferred backend JDK: not detected (using active JAVA_HOME/runtime)"
fi

log_info "Maven Wrapper: $($ROOT_DIR/backend/mvnw --version 2>/dev/null | head -n 1)"
log_info "Environment diagnostics completed successfully."
