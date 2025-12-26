#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VAWK_DIR="$ROOT_DIR/../vawk"
ADVENTURE_DIR="$ROOT_DIR"

print_banner() { printf '\n==== %s ====\n' "$1"; }
fail() { echo "ERROR: $*" >&2; exit 1; }

require_key() {
  if [[ -z "${OPENAI_API_KEY:-}" ]]; then
    fail "OPENAI_API_KEY is required for AI mode demos."
  fi
}

find_jar() {
  local dir="$1"
  local pattern="$2"
  local jar_path
  jar_path=$(ls "$dir"/target/$pattern 2>/dev/null | head -n1 || true)
  if [[ -z "$jar_path" ]]; then
    fail "No jar matching $pattern in $dir/target (did you run build?)."
  fi
  echo "$jar_path"
}

run_vawk_gen_autotest() {
  print_banner "VAWK Gen + Judge (--auto-test)"
  pushd "$VAWK_DIR" >/dev/null
  mkdir -p .vawk/chat .vawk/logs
  local vawk_jar
  vawk_jar=$(find_jar "$VAWK_DIR" "vawk-*.jar")
  SPRING_MAIN_WEB_APPLICATION_TYPE=none java -jar "$vawk_jar" gen --auto-test --tests-dir tests/sum_col3 "Write awk script to sum column 3"
  local test_log
  test_log=$(ls -t .vawk/logs/*.json 2>/dev/null | head -n1 || true)
  if [[ -z "$test_log" ]]; then
    popd >/dev/null
    fail "No test log written."
  fi
  echo "Receipts:"
  echo "  $test_log"
  echo "  $VAWK_DIR/spec.yaml"
  echo "  $VAWK_DIR/main.awk"
  popd >/dev/null
}

run_adventure_loop() {
  print_banner "BUUI Adventure Loop (AI mode)"
  pushd "$ADVENTURE_DIR" >/dev/null
  ai.translator.debug=true ai.narrator.debug=true ./adventure --mode=2025
  echo "BUUI run complete (on-screen receipts show translator/engine/narrator). Hand control back to presenter."
  popd >/dev/null
}

main() {
  require_key

  print_banner "Build (Adventure + VAWK)"
  pushd "$ADVENTURE_DIR" >/dev/null
  mvn -q package spring-boot:repackage
  popd >/dev/null
  pushd "$VAWK_DIR" >/dev/null
  mvn -q package spring-boot:repackage
  popd >/dev/null
  echo "Built:"
  echo "  $(find_jar "$ADVENTURE_DIR" "adventure-*.jar")"
  echo "  $(find_jar "$VAWK_DIR" "vawk-*.jar")"

  run_vawk_gen_autotest
  run_adventure_loop
}

main "$@"
