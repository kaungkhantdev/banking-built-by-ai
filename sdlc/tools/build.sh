#!/usr/bin/env bash
# One-command build: render all diagrams, embed them, produce the combined PDF.
# Self-healing: auto-discovers chrome-headless-shell + puppeteer-core so
# version bumps / cache cleans don't hardcode-break the pipeline.
# Usage: tools/build.sh   (run from anywhere)
#
# Layout:
#   <repo>/docs/*.md                  human-readable chapters
#   <repo>/docs/diagrams/*.png|svg     rendered diagrams (committed)
#   <repo>/docs/diagrams/src/*.mmd     mermaid sources
#   <repo>/tools/*                     this script + build machinery
#   <repo>/Digital-Banking-Platform-SDLC.pdf   the deliverable (repo root)
set -euo pipefail
cd "$(dirname "$0")"                 # tools/
REPO="$(cd .. && pwd)"
DIAG="$REPO/docs/diagrams"
SRC="$DIAG/src"

# --- auto-discover chrome-headless-shell (newest match wins) ---
discover_chrome() {
  local hits
  hits=$(find "$HOME/.cache/puppeteer" -type f -name 'chrome-headless-shell' 2>/dev/null | sort -V)
  [ -z "$hits" ] && hits=$(find "$HOME/.cache/puppeteer" -type f -name 'chrome' 2>/dev/null | sort -V)
  echo "$hits" | tail -1
}

# --- auto-discover the npx node_modules dir holding puppeteer-core + mermaid ---
discover_npx_modules() {
  find "$HOME/.npm/_npx" -maxdepth 2 -type d -name node_modules 2>/dev/null \
    -exec test -d '{}/puppeteer-core' ';' -print 2>/dev/null | head -1
}

CHROME="$(discover_chrome)"
NPX_CACHE="$(discover_npx_modules)"

echo "==> [1/3] Rendering diagrams (Mermaid -> PNG + SVG)"
echo '{ "args": ["--no-sandbox"] }' > "$SRC/puppeteer.json"
for f in mobile-flow dashboard-flow erd; do
  npx -y @mermaid-js/mermaid-cli -i "$SRC/$f.mmd" -o "$DIAG/$f.png" -b white -s 2 -p "$SRC/puppeteer.json" >/dev/null 2>&1
  npx -y @mermaid-js/mermaid-cli -i "$SRC/$f.mmd" -o "$DIAG/$f.svg" -b white      -p "$SRC/puppeteer.json" >/dev/null 2>&1
  echo "    ok  $f"
done

echo "==> [2/3] Building self-contained HTML (images inlined as base64)"
python3 build_pdf.py

echo "==> [3/3] Printing PDF with page-number footer"
if [ -n "$NPX_CACHE" ] && [ -d "$NPX_CACHE/puppeteer-core" ] && [ -n "$CHROME" ] && [ -x "$CHROME" ]; then
  # render_pdf.js reads CHROME_BIN if set, else falls back to its own discovery
  CHROME_BIN="$CHROME" NODE_PATH="$NPX_CACHE" node render_pdf.js
elif [ -n "$CHROME" ] && [ -x "$CHROME" ]; then
  echo "    (puppeteer-core not found; building without page-number footer)"
  "$CHROME" --headless --no-sandbox --disable-gpu \
    --print-to-pdf="$REPO/Digital-Banking-Platform-SDLC.pdf" --no-pdf-header-footer \
    "$(pwd)/_combined.html" 2>/dev/null
else
  echo "    ERROR: no chrome-headless-shell found under ~/.cache/puppeteer." >&2
  echo "    Run any diagram render once (npx @mermaid-js/mermaid-cli ...) to fetch it, then retry." >&2
  exit 1
fi

echo "==> Done: $REPO/Digital-Banking-Platform-SDLC.pdf"
