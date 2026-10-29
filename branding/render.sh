#!/usr/bin/env bash
# Rasterises the vector masters to PNG.
#
# The SVGs are the master copies: text stays sharp, the palette stays editable in one place, and any
# size can be produced on demand. Prefers rsvg-convert; falls back to Inkscape 1.x.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p png

render() { # input width height output
  local input=$1 w=$2 h=$3 out=$4
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w "$w" -h "$h" -o "$out" "$input"
  elif command -v inkscape >/dev/null 2>&1; then
    inkscape "$input" --export-type=png --export-filename="$out" \
      --export-width="$w" --export-height="$h" >/dev/null 2>&1
  else
    echo "Need rsvg-convert or Inkscape to render. Install librsvg2-bin or inkscape." >&2
    exit 1
  fi
  echo "  $out"
}

# The png/ folder is generated output; clear old sizes so the folder always matches this script.
rm -f png/*.png

echo "icon:"
for s in 512 256 128 64 32; do
  render icon.svg "$s" "$s" "png/icon-$s.png"
done

echo "cover:"
for d in "1280 720" "800 450"; do
  set -- $d
  render cover.svg "$1" "$2" "png/cover-$1x$2.png"
done

echo "feature:"
for d in "1280 720" "800 450"; do
  set -- $d
  render feature.svg "$1" "$2" "png/feature-$1x$2.png"
done

echo "social:"
for d in "1200 630" "600 315"; do
  set -- $d
  render social.svg "$1" "$2" "png/social-$1x$2.png"
done
