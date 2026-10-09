#!/usr/bin/env bash
# Render the main window for a matrix of look-and-feels / font scales.
# Usage: dev/snap.sh <out-dir> [label] (Gradle builds the dev tools automatically)
# Set JAVA_HOME to JDK 21 if it is not already your default JDK.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="${1:?Usage: dev/snap.sh <out-dir> [label]}"
LABEL="${2:-snap}"
mkdir -p "$DIR"
DIR="$(cd "$DIR" && pwd)"

# Metal at 1.0 is the baseline; larger scales simulate bigger theme fonts.
for spec in metal:1.0 metal:1.5 metal:2.0 gtk:1.0; do
    laf="${spec%%:*}"; sc="${spec##*:}"
    "$ROOT/gradlew" -p "$ROOT" snap -PsnapLaf="$laf" -PsnapScale="$sc" \
        -PsnapOutput="$DIR/${LABEL}_${laf}_${sc}.png"
done
ls "$DIR"
