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
: "${BPM_RUNNER_SUMMARY_FORMAT:=table}"
: "${BPM_RUNNER_CONTINUE_ON_ERROR:=0}"
: "${BPM_RUNNER_LOG_TO_FILE:=0}"
: "${BPM_RUNNER_ARTIFACTS_DIR:=$ROOT_DIR/.automation}"
: "${BPM_RUNNER_TIMEOUT_SECONDS:=0}"
: "${BPM_RUNNER_RETRY_COUNT:=2}"
: "${BPM_RUNNER_RETRY_DELAY_SECONDS:=2}"
: "${BPM_FRONTEND_DIR:=$ROOT_DIR/frontend}"
: "${BPM_BACKEND_DIR:=$ROOT_DIR/backend}"

normalize_path() {
  local path="$1"
  if [ -d "$path" ]; then
    (cd "$path" && pwd)
    return 0
  fi
  if [ -f "$path" ]; then
    local parent
    parent="$(cd "$(dirname "$path")" && pwd)"
    echo "$parent/$(basename "$path")"
    return 0
  fi
  return 1
}

validate_toggle() {
  local value="$1"
  local name="$2"
  if [ "$value" != "0" ] && [ "$value" != "1" ]; then
    echo "Invalid $name value '$value'. Expected 0 or 1." >&2
    return 1
  fi
}

validate_env() {
  validate_toggle "$BPM_RUNNER_NO_COLOR" "BPM_RUNNER_NO_COLOR"
  validate_toggle "$BPM_RUNNER_DRY_RUN" "BPM_RUNNER_DRY_RUN"
  validate_toggle "$BPM_RUNNER_SUMMARY" "BPM_RUNNER_SUMMARY"
  validate_toggle "$BPM_RUNNER_CONTINUE_ON_ERROR" "BPM_RUNNER_CONTINUE_ON_ERROR"
  validate_toggle "$BPM_RUNNER_LOG_TO_FILE" "BPM_RUNNER_LOG_TO_FILE"

  case "$BPM_RUNNER_LOG_LEVEL" in
    debug|info|warn|error) ;;
    *)
      echo "Invalid BPM_RUNNER_LOG_LEVEL '$BPM_RUNNER_LOG_LEVEL'. Expected debug|info|warn|error." >&2
      return 1
      ;;
  esac

  case "$BPM_RUNNER_SUMMARY_FORMAT" in
    table|json)
      ;;
    *)
      echo "Invalid BPM_RUNNER_SUMMARY_FORMAT '$BPM_RUNNER_SUMMARY_FORMAT'. Expected table|json." >&2
      return 1
      ;;
  esac

  if ! [[ "$BPM_RUNNER_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]]; then
    echo "Invalid BPM_RUNNER_TIMEOUT_SECONDS '$BPM_RUNNER_TIMEOUT_SECONDS'. Expected a non-negative integer." >&2
    return 1
  fi

  if ! [[ "$BPM_RUNNER_RETRY_COUNT" =~ ^[0-9]+$ ]]; then
    echo "Invalid BPM_RUNNER_RETRY_COUNT '$BPM_RUNNER_RETRY_COUNT'. Expected a non-negative integer." >&2
    return 1
  fi

  if ! [[ "$BPM_RUNNER_RETRY_DELAY_SECONDS" =~ ^[0-9]+$ ]]; then
    echo "Invalid BPM_RUNNER_RETRY_DELAY_SECONDS '$BPM_RUNNER_RETRY_DELAY_SECONDS'. Expected a non-negative integer." >&2
    return 1
  fi

  if ! BPM_FRONTEND_DIR="$(normalize_path "$BPM_FRONTEND_DIR")"; then
    echo "Unable to resolve BPM_FRONTEND_DIR: $BPM_FRONTEND_DIR" >&2
    return 1
  fi

  if ! BPM_BACKEND_DIR="$(normalize_path "$BPM_BACKEND_DIR")"; then
    echo "Unable to resolve BPM_BACKEND_DIR: $BPM_BACKEND_DIR" >&2
    return 1
  fi
}

validate_env

# Use deterministic locale/timezone for reproducible script output.
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
export TZ=UTC

RUN_ID="$(date -u +'%Y%m%dT%H%M%SZ')-$$"
RUN_STARTED_AT="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
RUN_HOSTNAME="$(hostname 2>/dev/null || echo unknown-host)"
RUN_SHELL="${BASH_VERSION:-bash-unknown}"

if [ "$BPM_RUNNER_LOG_TO_FILE" = "1" ]; then
  mkdir -p "$BPM_RUNNER_ARTIFACTS_DIR"
  RUN_LOG_FILE="$BPM_RUNNER_ARTIFACTS_DIR/run-$RUN_ID.log"
else
  RUN_LOG_FILE=''
fi

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
declare -a STEP_NAMES=()
declare -a STEP_INDICES=()
STEP_COUNTER=0

write_log_file() {
  local line="$1"
  if [ -n "$RUN_LOG_FILE" ]; then
    printf '%s\n' "$line" >>"$RUN_LOG_FILE"
  fi
}

log_section() {
  echo
  printf '%b========== %s ==========%b\n' "$COLOR_BLUE" "$*" "$COLOR_RESET"
  write_log_file "========== $* =========="
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

  local timestamp
  timestamp="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  local rendered="[$timestamp] [$label] $*"
  printf '%b%s%b\n' "$color" "$rendered" "$COLOR_RESET" >&2
  write_log_file "$rendered"
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

require_executable_file() {
  local file="$1"
  require_file "$file"
  if [ ! -x "$file" ]; then
    log_error "Required executable permission is missing: $file"
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
  if [ "$BPM_RUNNER_TIMEOUT_SECONDS" -gt 0 ] && command -v timeout >/dev/null 2>&1; then
    timeout "$BPM_RUNNER_TIMEOUT_SECONDS" bash -lc "$cmd"
  else
    eval "$cmd"
  fi
}

run_with_retry() {
  local attempts="$1"
  local delay="$2"
  shift 2
  local cmd=("$@")
  local try=1
  while :; do
    if "${cmd[@]}"; then
      return 0
    fi

    if [ "$try" -ge "$attempts" ]; then
      log_error "Command failed after $attempts attempt(s): ${cmd[*]}"
      return 1
    fi

    log_warn "Attempt $try/$attempts failed. Retrying in ${delay}s: ${cmd[*]}"
    sleep "$delay"
    try=$((try + 1))
  done
}

record_step() {
  local name="$1"
  local status="$2"
  local duration="$3"
  local message="$4"

  STEP_COUNTER=$((STEP_COUNTER + 1))
  STEP_INDICES+=("$STEP_COUNTER")
  STEP_NAMES+=("$name")
  STEP_RESULTS+=("$status")
  STEP_DURATIONS+=("$duration")
  STEP_MESSAGES+=("$message")
}

run_step() {
  local name="$1"
  shift

  local start
  start="$(start_timer)"
  log_info "Starting: $name"

  if [ "$BPM_RUNNER_DRY_RUN" = "1" ]; then
    record_step "$name" "SKIP" "0" "dry-run"
    log_info "Skipped (dry-run): $name"
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

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "$value"
}

print_summary_table() {
  local failures=0
  local i
  for i in "${!STEP_RESULTS[@]}"; do
    local index="${STEP_INDICES[$i]}"
    local name="${STEP_NAMES[$i]}"
    local status="${STEP_RESULTS[$i]}"
    local duration="${STEP_DURATIONS[$i]}"
    local msg="${STEP_MESSAGES[$i]}"

    local color="$COLOR_GREEN"
    if [ "$status" = "FAIL" ]; then
      color="$COLOR_RED"
      failures=$((failures + 1))
    elif [ "$status" = "SKIP" ]; then
      color="$COLOR_YELLOW"
    fi

    if [ -n "$msg" ]; then
      printf '%b- [%s] %s: %s (%ss) %s%b\n' "$color" "$index" "$status" "$name" "$duration" "$msg" "$COLOR_RESET"
    else
      printf '%b- [%s] %s: %s (%ss)%b\n' "$color" "$index" "$status" "$name" "$duration" "$COLOR_RESET"
    fi
  done

  if [ "$failures" -gt 0 ]; then
    log_error "Summary: $failures step(s) failed"
    return 1
  fi

  log_info "Summary: all non-skipped steps passed"
}

print_summary_json() {
  mkdir -p "$BPM_RUNNER_ARTIFACTS_DIR"
  local summary_file="$BPM_RUNNER_ARTIFACTS_DIR/summary-$RUN_ID.json"
  {
    printf '{\n'
    printf '  "runId": "%s",\n' "$(json_escape "$RUN_ID")"
    printf '  "startedAt": "%s",\n' "$(json_escape "$RUN_STARTED_AT")"
    printf '  "host": "%s",\n' "$(json_escape "$RUN_HOSTNAME")"
    printf '  "shell": "%s",\n' "$(json_escape "$RUN_SHELL")"
    printf '  "steps": [\n'
    local i
    for i in "${!STEP_RESULTS[@]}"; do
      local comma=','
      if [ "$i" -eq $((${#STEP_RESULTS[@]} - 1)) ]; then
        comma=''
      fi
      printf '    {"index": %s, "name": "%s", "status": "%s", "durationSeconds": %s, "message": "%s"}%s\n' \
        "${STEP_INDICES[$i]}" \
        "$(json_escape "${STEP_NAMES[$i]}")" \
        "$(json_escape "${STEP_RESULTS[$i]}")" \
        "${STEP_DURATIONS[$i]}" \
        "$(json_escape "${STEP_MESSAGES[$i]}")" \
        "$comma"
    done
    printf '  ]\n'
    printf '}\n'
  } >"$summary_file"

  log_info "Wrote JSON summary: $summary_file"
}

print_summary() {
  [ "$BPM_RUNNER_SUMMARY" = "1" ] || return 0
  [ "${#STEP_RESULTS[@]}" -gt 0 ] || return 0

  echo
  log_section "Execution summary"

  if [ "$BPM_RUNNER_SUMMARY_FORMAT" = "json" ]; then
    print_summary_table
    print_summary_json
    return $?
  fi

  print_summary_table
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
