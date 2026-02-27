#!/usr/bin/env bash
# Shared helpers for repository automation scripts.
# Keeping these utilities centralized ensures all scripts behave consistently.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Automation behavior can be tuned through environment variables without editing scripts.
: "${BPM_RUNNER_LOG_LEVEL:=info}"
: "${BPM_RUNNER_NO_COLOR:=0}"
: "${BPM_RUNNER_DRY_RUN:=0}"
: "${BPM_RUNNER_SUMMARY:=1}"
: "${BPM_RUNNER_CONTINUE_ON_ERROR:=0}"
: "${BPM_FRONTEND_DIR:=$ROOT_DIR/frontend}"
: "${BPM_BACKEND_DIR:=$ROOT_DIR/backend}"

if [ "$BPM_RUNNER_NO_COLOR" = "1" ] || [ ! -t 1 ]; then
  COLOR_RESET=''
  COLOR_BLUE=''
  COLOR_GREEN=''
  COLOR_YELLOW=''
  COLOR_RED=''
else
  COLOR_RESET='\033[0m'
  COLOR_BLUE='\033[34m'
  COLOR_GREEN='\033[32m'
  COLOR_YELLOW='\033[33m'
  COLOR_RED='\033[31m'
fi

declare -a STEP_RESULTS=()
declare -a STEP_DURATIONS=()
declare -a STEP_MESSAGES=()

log_section() {
  echo
  printf '%b========== %s ==========%b\n' "$COLOR_BLUE" "$*" "$COLOR_RESET"
}

is_log_level_enabled() {
  local level="$1"
  case "$BPM_RUNNER_LOG_LEVEL" in
    debug) return 0 ;;
    info) [ "$level" != "debug" ] ;;
    warn) [ "$level" = "warn" ] || [ "$level" = "error" ] ;;
    error) [ "$level" = "error" ] ;;
    *) return 0 ;;
  esac
}

log_msg() {
  local level="$1"
  shift
  is_log_level_enabled "$level" || return 0

  local color="$COLOR_GREEN"
  local label="INFO"
  case "$level" in
    debug)
      color="$COLOR_BLUE"
      label="DEBUG"
      ;;
    warn)
      color="$COLOR_YELLOW"
      label="WARN"
      ;;
    error)
      color="$COLOR_RED"
      label="ERROR"
      ;;
  esac
  printf '%b[%s] [%s] %s%b\n' "$color" "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" "$label" "$*" "$COLOR_RESET" >&2
}

log_info() { log_msg info "$@"; }
log_warn() { log_msg warn "$@"; }
log_error() { log_msg error "$@"; }
log_debug() { log_msg debug "$@"; }

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    log_error "Missing required command: $cmd"
    return 1
  fi
}

require_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    log_error "Required file not found: $file"
    return 1
  fi
}

require_dir() {
  local dir="$1"
  if [ ! -d "$dir" ]; then
    log_error "Required directory not found: $dir"
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

run_cmd() {
  local cmd="$*"
  if [ "$BPM_RUNNER_DRY_RUN" = "1" ]; then
    log_info "[dry-run] $cmd"
    return 0
  fi

  log_debug "Executing command: $cmd"
  eval "$cmd"
}

record_step() {
  local name="$1"
  local status="$2"
  local duration="$3"
  local message="$4"

  STEP_RESULTS+=("$status")
  STEP_DURATIONS+=("$duration")
  STEP_MESSAGES+=("$name|$message")
}

run_step() {
  local name="$1"
  shift

  local start
  start="$(start_timer)"
  log_info "Starting: $name"

  if [ "$BPM_RUNNER_DRY_RUN" = "1" ]; then
    record_step "$name" "PASS" "0" "dry-run"
    log_info "Completed: $name (dry-run)"
    return 0
  fi

  if "$@"; then
    local duration
    duration="$(elapsed_seconds "$start")"
    record_step "$name" "PASS" "$duration" ""
    log_info "Completed: $name (${duration}s)"
    return 0
  fi

  local duration
  duration="$(elapsed_seconds "$start")"
  record_step "$name" "FAIL" "$duration" "Command failed"
  log_error "Failed: $name (${duration}s)"

  if [ "$BPM_RUNNER_CONTINUE_ON_ERROR" = "1" ]; then
    return 0
  fi
  return 1
}

print_summary() {
  [ "$BPM_RUNNER_SUMMARY" = "1" ] || return 0
  [ "${#STEP_RESULTS[@]}" -gt 0 ] || return 0

  echo
  log_section "Execution summary"

  local failures=0
  local i
  for i in "${!STEP_RESULTS[@]}"; do
    local status="${STEP_RESULTS[$i]}"
    local duration="${STEP_DURATIONS[$i]}"
    local name="${STEP_MESSAGES[$i]%%|*}"
    local msg="${STEP_MESSAGES[$i]#*|}"
    if [ "$status" = "FAIL" ]; then
      failures=$((failures + 1))
      printf '%b- %s: %s (%ss) %s%b\n' "$COLOR_RED" "$status" "$name" "$duration" "$msg" "$COLOR_RESET"
    else
      printf '%b- %s: %s (%ss)%b\n' "$COLOR_GREEN" "$status" "$name" "$duration" "$COLOR_RESET"
    fi
  done

  if [ "$failures" -gt 0 ]; then
    log_error "Summary: $failures step(s) failed"
    return 1
  fi

  log_info "Summary: all steps passed"
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

ensure_standard_dirs() {
  require_dir "$BPM_FRONTEND_DIR"
  require_dir "$BPM_BACKEND_DIR"
}

check_lockfiles() {
  require_file "$ROOT_DIR/package-lock.json"
  require_file "$BPM_FRONTEND_DIR/package-lock.json"
}

handle_error_trap() {
  local exit_code="$1"
  local line="$2"
  local cmd="$3"
  log_error "Command failed at line $line with exit code $exit_code: $cmd"
}

install_error_trap() {
  trap 'handle_error_trap $? $LINENO "$BASH_COMMAND"' ERR
}
