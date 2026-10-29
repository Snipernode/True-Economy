#!/usr/bin/env bash
# Checks every factual claim printed in the branding art against the actual source.
# A marketplace listing that overstates what ships is a refund request later.
set -euo pipefail
cd "$(dirname "$0")/.."

entries=$(grep -cE '^    [A-Z0-9_]+:$' src/main/resources/config.yml)
commodities=$(grep -cE '^  [a-z0-9-]+:$' src/main/resources/market/items.yml)
achievements=$(grep -c 'add(new Achievement' src/main/java/com/economy/plugin/achievements/AchievementRegistry.java)
sectors=$(grep -cE '^    [A-Z_]+\("' src/main/java/com/economy/plugin/market/Sector.java)
builds=$(ls dist/*.jar 2>/dev/null | wc -l)
tests=$(grep -rc '@Test' src/test | awk -F: '{s+=$2} END {print s+0}')
categories=$(sed -n '150,1045p' src/main/resources/config.yml | grep -cE '^    [a-z0-9_]+:$')
first_version=$(ls dist/*.jar | sed 's|dist/true-economy-||; s|\.jar$||' | sort -V | head -1)
last_version=$(ls dist/*.jar | sed 's|dist/true-economy-||; s|\.jar$||' | sort -V | tail -1)

fail=0
check() { # name claimed actual
  if [ "$2" = "$3" ]; then printf '  OK    %-22s %s\n' "$1" "$3"
  else printf '  WRONG %-22s art says %s, source has %s\n' "$1" "$2" "$3"; fail=1; fi
}

check_text() { # file expected text
  if grep -qF -- "$2" "$1"; then printf '  OK    %-22s "%s"\n' "$(basename "$1")" "$2"
  else printf '  WRONG %-22s missing "%s"\n' "$(basename "$1")" "$2"; fail=1; fi
}

echo "Source counts:"
check "shop entries"    "734" "$entries"
check "shop categories" "27"  "$categories"
check "commodities"     "65"  "$commodities"
check "achievements"    "26"  "$achievements"
check "sectors"         "8"   "$sectors"
check "builds"          "68"  "$builds"
check "tests"           "237" "$tests"
check "version floor"   "1.8" "$first_version"
check "version ceiling" "26.3" "$last_version"

echo
echo "Art claims:"
check_text branding/cover.svg   "734 shop items"
check_text branding/cover.svg   "65 tradeable commodities"
check_text branding/cover.svg   "8 sectors"
check_text branding/cover.svg   "26 achievements"
check_text branding/cover.svg   "1.8 – 26.3"
check_text branding/cover.svg   "68 prebuilt jars, 1.8 to 26.3"

check_text branding/feature.svg "65 commodities"
check_text branding/feature.svg "27 categories"
check_text branding/feature.svg "734"
check_text branding/feature.svg "26 achievements"
check_text branding/feature.svg "237 tests"
check_text branding/feature.svg "68 builds"
check_text branding/feature.svg "1.8 → 26.3"

check_text branding/social.svg  "65 COMMODITIES"
check_text branding/social.svg  ">65</text>"
check_text branding/social.svg  ">734</text>"
check_text branding/social.svg  ">68</text>"
check_text branding/social.svg  ">26</text>"
check_text branding/social.svg  "1.8 → 26.3"

echo
if [ "$fail" -ne 0 ]; then
  echo "Branding claims do not match the build. Fix the art before uploading." >&2
fi
exit $fail
