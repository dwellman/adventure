#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"
source "$DIR/scripts/common.sh"

MVN_BIN="${MVN_BIN:-mvn}"
QUIET=1

usage() {
  cat <<'EOF'
GameBuilder wrapper.
Usage: builder <input.yaml> [--out FILE|--stdout] [--skip-validate] [--bom] [--report] [--verbose] [--help]
  --verbose   : show Maven output (default suppressed)
  --help      : show this help
Additional:
  architect <file.md> ... : see ArchitectCli for converting plot lists to YAML.
EOF
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --verbose)
      QUIET=0
      ;;
    *)
      ARGS+=("$1")
      ;;
  esac
  shift
done

if [[ ${#ARGS[@]} -eq 0 ]]; then
  echo "Input YAML is required." >&2
  usage
  exit 1
fi

CMD=("${MVN_BIN}")
if [[ "${ARGS[0]}" == "architect" ]]; then
  ARGS=("${ARGS[@]:1}")
  build_maven_exec_cmd "$QUIET" 1 "com.demo.adventure.authoring.cli.ArchitectCli" "${ARGS[*]}"
else
  build_maven_exec_cmd "$QUIET" 1 "com.demo.adventure.authoring.cli.GameBuilderCli" "${ARGS[*]}"
fi

exec "${CMD[@]}"
