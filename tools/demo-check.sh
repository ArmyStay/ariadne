#!/usr/bin/env bash
# Is the demo's artifact actually a website, served where Pages will serve it?
#
# This exists because the deploy was green while the root URL answered 404. Two
# assumptions were doing the work that a check should: that Gradle puts index.html in
# `build/dist/wasmJs/productionExecutable`, and that a page which opens from a file path
# also opens from `https://armystay.github.io/ariadne/`. Neither is free.
#
# So: find index.html (never assume its directory), copy the folder that holds it into a
# stage directory whose root *is* the site, then serve that stage under `/ariadne/` — the
# prefix a project page really uses — and require a 200 with every `src`/`href` in the
# page resolving. An absolute `/asset` path works on a laptop and 404s on Pages; that is
# the failure this catches.
#
#   ./tools/demo-check.sh                 # check only, discarding the assembled site
#   ./tools/demo-check.sh --stage <dir>   # assemble into <dir> and check what is there
#
# Needs: python3 (any recent), no network, no JDK. The wasm distribution must exist:
#   ./gradlew :app:wasm:wasmJsBrowserDistribution
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

stage=""
while [ $# -gt 0 ]; do
  case "$1" in
    --stage) stage="${2:-}"; shift 2 ;;
    -h|--help) sed -n '2,22p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "demo-check: unknown argument $1" >&2; exit 2 ;;
  esac
done

dist="$ROOT/app/wasm/build/dist/wasmJs"
if [ ! -d "$dist" ]; then
  echo "demo-check: $dist is missing — build it first:" >&2
  echo "            ./gradlew :app:wasm:wasmJsBrowserDistribution" >&2
  exit 1
fi

# Production first: a developer's tree can also hold a development build.
index=$(find "$dist" -path '*productionExecutable*' -name index.html -print -quit 2>/dev/null)
[ -n "$index" ] || index=$(find "$dist" -name index.html -print -quit 2>/dev/null)
if [ -z "$index" ]; then
  echo "demo-check: no index.html anywhere under $dist" >&2
  find "$dist" -maxdepth 3 -printf '    %P\n' | head -30 >&2
  exit 1
fi

site="${stage:-$(mktemp -d)}"
rm -rf "$site"
mkdir -p "$site"
cp -a "$(dirname "$index")"/. "$site"/

echo "demo-check: page found at ${index#"$ROOT"/}"
if ! compgen -G "$site/*.wasm" > /dev/null; then
  echo "demo-check: no .wasm file beside index.html — that is not the app" >&2
  exit 1
fi
if ! compgen -G "$site/ariadne.js" > /dev/null; then
  echo "demo-check: no ariadne.js beside index.html — that is not the app" >&2
  exit 1
fi

python3 - "$site" <<'PY'
import functools
import http.server
import os
import re
import shutil
import sys
import tempfile
import threading
import urllib.error
import urllib.parse
import urllib.request

site = sys.argv[1]
prefix = "/ariadne/"
port = 8123

# A project page is served from a subdirectory, so the stage is mounted one level down:
# /ariadne/… is the page under test, and anything above it is not our business.
web = tempfile.mkdtemp()
mount = os.path.join(web, "ariadne")
shutil.copytree(site, mount)
class Quiet(http.server.SimpleHTTPRequestHandler):
    """Serves the stage without narrating every request to stderr."""

    def log_message(self, *args, **kwargs):  # noqa: D102, ANN002 - base class signature
        pass


handler = functools.partial(Quiet, directory=web)
server = http.server.ThreadingHTTPServer(("127.0.0.1", port), handler)
threading.Thread(target=server.serve_forever, daemon=True).start()

base = "http://127.0.0.1:%d%s" % (port, prefix)
try:
    html = urllib.request.urlopen(base, timeout=10).read().decode("utf-8", "replace")
except Exception as failure:  # noqa: BLE001 - the message is the report
    print("demo-check: %s did not answer: %s" % (base, failure), file=sys.stderr)
    server.shutdown()
    sys.exit(1)

problems = []
refs = [r for r in re.findall(r'(?:src|href)="([^"]+)"', html) if r]
for ref in refs:
    if re.match(r"^(?:[a-z]+:|//|#)", ref):
        continue
    if ref.startswith("/"):
        problems.append("%r is an absolute path, so it cannot resolve under %s" % (ref, prefix))
        continue
    url = urllib.parse.urljoin(base, ref)
    try:
        code = urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=10).status
    except urllib.error.HTTPError as failure:
        problems.append("%s: HTTP %d" % (ref, failure.code))
        continue
    except Exception as failure:  # noqa: BLE001
        problems.append("%s: %s" % (ref, failure))
        continue
    if code != 200:
        problems.append("%s: HTTP %d" % (ref, code))

server.shutdown()
if problems:
    print("demo-check: the site does not serve at %s" % prefix, file=sys.stderr)
    for problem in problems:
        print("  %s" % problem, file=sys.stderr)
    sys.exit(1)
print("demo-check: index.html and its %d reference(s) all resolve under %s" % (len(refs), prefix))
PY
status=$?
# Only the throwaway copy goes; a staged site is the caller's.
[ -n "$stage" ] || rm -rf "$site"
if [ $status -ne 0 ]; then
  exit $status
fi
if [ -n "$stage" ]; then
  echo "demo-check: staged at $stage ($(du -sh "$stage" | cut -f1))"
fi
