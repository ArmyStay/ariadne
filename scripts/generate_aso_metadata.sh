#!/usr/bin/env bash
#
# Generate a screenshot + keyword pair for the iOS App Store and Google Play,
# ready to paste into App Store Connect and the Google Play Console.
#
# Usage (from the repo root): ./scripts/generate_aso_metadata.sh "Invoices & expenses" en
#   The second argument is the target language (default: en).

set -e

BASENAME=${1:?"usage: ./scripts/generate_aso_metadata.sh \"App subtitle\" [lang]"}
LANG=${2:-en}

# A real banner: generated with the same images as the README hero so the store
# look stays consistent with the app's first screen.
BANNER=$(ls docs/*.png 2>/dev/null | head -1)
if [ -z "$BANNER" ]; then
  echo "warning: no docs/*.png found; the pair will say so."
fi

echo "--- $LANG ---"
echo "Title:       $(echo "$BASENAME" | head -c 80)"
echo "Description: $BASENAME — receipt scanning, trip tracking, and expenses on the go."
echo "Keywords:    receipts, expenses, scans, trips, travel, exports"
echo "Banner:      ${BANNER:-docs/unavailable.png}"
echo "Store:       iOS App Store — paste into App Store Connect."
