#!/usr/bin/env bash
set -euo pipefail

# Shared helpers for CLI wrappers.

# Build a Maven exec command into the global CMD array.
# Args: quietFlag(0|1) skipTests(0|1) mainClass execArgsString
build_maven_exec_cmd() {
  local quiet="${1:-0}"
  local skip_tests="${2:-1}"
  local main_class="${3:-}"
  local exec_args="${4:-}"

  CMD=("${MVN_BIN:-mvn}")
  if [[ "$quiet" -eq 1 ]]; then
    CMD+=("-q")
  fi
  if [[ "$skip_tests" -eq 1 ]]; then
    CMD+=("-DskipTests=true")
  fi
  if [[ -n "$main_class" ]]; then
    CMD+=("-Dexec.mainClass=${main_class}")
  fi
  if [[ -n "$exec_args" ]]; then
    CMD+=("-Dexec.args=${exec_args}")
  fi
  CMD+=("compile" "exec:java")
}
