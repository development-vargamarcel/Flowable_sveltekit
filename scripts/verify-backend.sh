#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_repo_root
start="$(start_timer)"
cd "$ROOT_DIR/backend"

if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
  export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

log_section "Backend verification"
log_info "Running backend Maven test suite"
./mvnw -B test

log_info "Backend verification completed in $(elapsed_seconds "$start")s"
