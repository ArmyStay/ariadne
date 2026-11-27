#!/usr/bin/env bash
# Does the documentation still describe this code?
#
# The one rule the prose in this repo has to keep is that it describes what the code
# actually does. Most of that is judgement, but four kinds of claim can be checked by a
# machine, and these are the four that have already gone wrong once:
#
#   1. a `Route.Name` the router does not have (the README once claimed a `TripShare`
#      route that never existed);
#   2. a hook into another runtime that is not defined anywhere (a `window.ariadne*`
#      recogniser hook existed only in a table);
#   3. an Android permission the manifest does not declare (or the reverse);
#   4. a relative link, or a backticked path, that does not resolve.
#
# No Gradle, no network, no JDK: it is greps and a `while read`.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

problems=0
note() { printf '  %s\n' "$1"; problems=$((problems + 1)); }

# Every markdown file in the repository, once.
mapfile -t DOCS < <(find . -name '*.md' -not -path './.git/*' -not -path './build/*' -not -path './node_modules/*' | sort)

echo "claim-check: ${#DOCS[@]} documents"

# ---------------------------------------------------------------------------
# 1. Routes
# ---------------------------------------------------------------------------
route_file="ui/app/src/commonMain/kotlin/com/ariadne/app/Route.kt"
mapfile -t ROUTES < <(grep -oE 'public (data object|data class) [A-Za-z]+' "$route_file" | awk '{print $4}')
if [ "${#ROUTES[@]}" -eq 0 ]; then
  note "$route_file: no routes found — has the file moved?"
else
  echo "claim-check: ${#ROUTES[@]} routes in the router"
  for doc in "${DOCS[@]}"; do
    while read -r name; do
      [ -n "$name" ] || continue
      if printf '%s\n' "${ROUTES[@]}" | grep -qx "$name"; then continue; fi
      # A document may name a route in order to say it was never built — anywhere on
      # some line that mentions it.
      if grep -F "Route.$name" "$doc" 2>/dev/null \
           | grep -qiE 'never existed|never built|no such|does not exist|removed'; then
        continue
      fi
      note "$doc: mentions Route.$name, which is not in the router"
    # A route is capitalised; `Route.kt` and `Route.tab()` are file names and functions.
    done < <(grep -oE 'Route\.[A-Z][A-Za-z0-9]*' "$doc" | sed 's/^Route\.//' | sort -u)
  done
fi

# ---------------------------------------------------------------------------
# 2. Hooks into the host runtime (`window.something`)
# ---------------------------------------------------------------------------
while read -r hook; do
  [ -n "$hook" ] || continue
  if ! grep -rqF "$hook" --include='*.kt' --include='*.html' --include='*.js' .; then
    # Naming a hook in order to say it does not exist is exactly what this file is for.
    line=$(grep -rhF -m1 "$hook" "${DOCS[@]}" 2>/dev/null || true)
    case "$line" in
      *'not implemented'*|*'does not exist'*|*'no such'*|*'never built'*|*absent*) continue ;;
    esac
    note "a document names $hook, but nothing in the code defines it"
  fi
# A glob (`window.ariadne*`) is a category, not a name: strip the star first.
done < <(grep -rhoE 'window\.[a-zA-Z_][a-zA-Z0-9_]*\*?' "${DOCS[@]}" 2>/dev/null \
           | sort -u | grep -v '\*$')

# ---------------------------------------------------------------------------
# 3. Android permissions
# ---------------------------------------------------------------------------
manifest="app/android/src/main/AndroidManifest.xml"
mapfile -t DECLARED < <(grep -oE 'android\.permission\.[A-Z_]+' "$manifest" | sed 's/^android\.permission\.//' | sort -u)
for doc in "${DOCS[@]}"; do
  while read -r perm; do
    [ -n "$perm" ] || continue
    printf '%s\n' "${DECLARED[@]}" | grep -qx "$perm" || note "$doc: mentions $perm, which the manifest does not declare"
  done < <(grep -oE 'android\.permission\.[A-Z_]+' "$doc" | sed 's/^android\.permission\.//' | sort -u)
done

# ---------------------------------------------------------------------------
# 4. Relative links, and backticked paths
# ---------------------------------------------------------------------------
for doc in "${DOCS[@]}"; do
  dir="$(dirname "$doc")"
  # Markdown links, minus anchors and absolute URLs.
  while read -r link; do
    [ -n "$link" ] || continue
    case "$link" in http*|mailto:*|\#*) continue ;; esac
    target="${link%%#*}"
    [ -n "$target" ] || continue
    [ -e "$dir/$target" ] || note "$doc: link to '$link' does not resolve"
  done < <(grep -oE '\]\([^)[:space:]]+\)' "$doc" | sed 's/^](//; s/)$//')
  # Backticked paths that name a real file type and are not ellipses or globs.
  while read -r path; do
    [ -n "$path" ] || continue
    case "$path" in *...*|*…*|*\**|*'<'*|*'|'*) continue ;; esac
    case "$path" in
      */|*.*) ;;
      *) continue ;;
    esac
    case "$path" in
      *.md|*.sh|*.kt|*.kts|*.yml|*.yaml|*.png|*.html|*.toml|*.xml|*.properties|*.json) ;;
      *) continue ;;
    esac
    # Only paths that name a place in this repository.
    case "$path" in
      docs/*|tools/*|core/*|ui/app/*|app/*|preview/*|recogniser-fixtures/*|.github/*) ;;
      *) continue ;;
    esac
    if [ ! -e "$path" ] && [ ! -e "$dir/$path" ]; then
      # A document may name something it is explicitly telling you is gone.
      line=$(grep -nF "\`$path\`" "$doc" | head -1)
      case "$line" in
        *removed*|*deleted*|*'no longer'*|*archived*) continue ;;
      esac
      note "$doc: names '$path', which does not exist"
    fi
  done < <(grep -oE '`[][A-Za-z0-9_./-]+`' "$doc" | tr -d '`')
done

# ---------------------------------------------------------------------------
if [ "$problems" -ne 0 ]; then
  echo "claim-check: $problems claim(s) the code does not keep" >&2
  exit 1
fi
echo "claim-check: the documentation's checkable claims all hold"
