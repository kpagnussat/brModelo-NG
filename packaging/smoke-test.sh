#!/usr/bin/env bash
set -euo pipefail
# Every application write stays inside a fresh temporary home and working directory.
root=$(cd "$(dirname "$0")/.." && pwd)
smoke=$(mktemp -d "${TMPDIR:-/tmp}/brmodelo-ng-smoke.XXXXXX")
trap 'echo "Smoke artifacts: $smoke"' EXIT
mkdir -p "$smoke"/{config,state,data,preferences,work}
launcher="$root/build/jpackage/app-image/brmodelo-ng/bin/brmodelo-ng"
files=("$root/test-resources/fixtures/conceitual.brM3" "$root/test-resources/fixtures/logico.brM3")
cd "$smoke/work"
set +e
GSETTINGS_BACKEND=memory XDG_CONFIG_HOME="$smoke/config" XDG_STATE_HOME="$smoke/state" XDG_DATA_HOME="$smoke/data" \
JAVA_TOOL_OPTIONS="-Duser.home=$smoke -Djava.util.prefs.userRoot=$smoke/preferences" \
timeout 15 "$launcher" "${files[@]}" > "$smoke/launcher.log" 2>&1
status=$?
set -e
cat "$smoke/launcher.log"
[ "$status" = 124 ] || { echo "Expected timeout 124, got $status" >&2; exit 1; }
for file in "${files[@]}"; do
    grep -F "Opened diagram: $file (" "$smoke/launcher.log"
done
echo 'Launcher remained running until timeout 15; both command-line fixtures opened.'
