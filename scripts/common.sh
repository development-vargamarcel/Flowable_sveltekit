#!/usr/bin/env bash
# Shared helpers for repository automation scripts.
# Keeping these utilities centralized ensures all scripts behave consistently.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

log_section() {
  echo
  echo "========== $* =========="
}

log_info() {
  printf '[%s] [INFO] %s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" "$*"
}

log_error() {
  printf '[%s] [ERROR] %s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" "$*" >&2
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    log_error "Missing required command: $cmd"
    return 1
  fi
}

start_timer() {
  date +%s
}

elapsed_seconds() {
  local start="$1"
  local end
  end="$(date +%s)"
  echo "$((end - start))"
}

npm_safe() {
  # npm 10+/11+ emits noisy warnings when legacy hyphenated proxy env vars are inherited.
  # Using env -u keeps script output clean without mutating the user's shell profile.
  env -u npm_config_http-proxy \
      -u npm_config_https-proxy \
      -u npm_config_proxy \
      -u npm_config_noproxy \
      -u NPM_CONFIG_HTTP-PROXY \
      -u NPM_CONFIG_HTTPS-PROXY \
      NPM_CONFIG_LOGLEVEL=error npm "$@"
}

ensure_repo_root() {
  if [ ! -d "$ROOT_DIR/.git" ]; then
    log_error "Expected git repository root at $ROOT_DIR"
    return 1
  fi
}
