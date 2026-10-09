#!/usr/bin/env python3
"""Compile once, then measure fresh JVMs outside Gradle. Inputs are never written/copied.

JAVA_HOME=/path/to/jdk21 python3 dev/measure_performance.py /tmp/brmodelo-perf/before
Repeat with /tmp/brmodelo-perf/after after editing production code.
Set BRMODELO_BENCH_SAMPLES to add your own diagrams to the repo fixtures.
"""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

repo = Path(__file__).resolve().parent.parent
out = Path(sys.argv[1]).resolve()
out.mkdir(parents=True, exist_ok=True)
baseline_jar = os.environ.get("BRMODELO_BENCH_JAR")
subprocess.run([str(repo / "gradlew"), *([] if baseline_jar else ["jar"]), "devClasses"], cwd=repo, check=True)
java = Path(os.environ["JAVA_HOME"]) / "bin/java"
cp = os.pathsep.join([str(repo / "build/classes/java/dev"), baseline_jar or str(repo / "build/libs/brModelo.jar")])
samples = sorted((repo / "test-resources/fixtures").glob("*.brM3"))
# Extra diagrams from outside the repo, read in place: BRMODELO_BENCH_SAMPLES=dir1:dir2 (recursive).
for folder in filter(None, os.environ.get("BRMODELO_BENCH_SAMPLES", "").split(os.pathsep)):
    samples += sorted(Path(folder).expanduser().rglob("*.brM3"))
hashes = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in samples}
(out / "inputs.json").write_text(json.dumps(hashes, ensure_ascii=False, indent=2))
viewport_only = "--viewport" in sys.argv[2:]
modes = [("viewport", 3)] if viewport_only else [("startup", 7)] if "--startup" in sys.argv[2:] else [("startup", 7), ("profile", 3)]
for kind, count in modes:
    for i in range(count):
        # A rerun must not prompt to recover the previous benchmark's autosave.
        home = Path(tempfile.mkdtemp(prefix=f"{kind}-{i}-home-", dir=out))
        env = os.environ.copy()
        for var, folder in [("XDG_CONFIG_HOME", "config"), ("XDG_STATE_HOME", "state"), ("XDG_DATA_HOME", "data")]:
            env[var] = str(home / folder)
        cmd = [str(java), f"-Duser.home={home}"]
        if kind != "startup":
            cmd += [f"-XX:StartFlightRecording=filename={out / f'profile-{i}.jfr'},settings=profile,dumponexit=true"]
        cmd += ["-cp", cp, "ViewportPerformance" if viewport_only else "Performance"]
        if kind != "startup": cmd += [str(p) for p in samples]
        with (out / f"{kind}-{i}.csv").open("w") as stdout, (out / f"{kind}-{i}.stderr").open("w") as stderr:
            result = subprocess.run(cmd, env=env, cwd=repo, text=True, stdout=stdout, stderr=stderr, timeout=600)
        print(kind, i, result.returncode, flush=True)
        result.check_returncode()
assert hashes == {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in samples}
