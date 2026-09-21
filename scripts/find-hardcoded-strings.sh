#!/usr/bin/env bash
# Lists candidate user-facing string literals still inline in Kotlin.
#
# There is no lint check for hardcoded strings in Compose (HardcodedText is
# XML-only), so this is the standing substitute: run it before and after a
# migration and review the delta.
#
# The exclusions are not cosmetic. Animation labels, Crossfade state
# discriminators, persisted preference tokens and route strings all look like
# UI text and are all load-bearing — localising any of them breaks behaviour
# rather than just wording.
set -euo pipefail
cd "$(dirname "$0")/.."

rg -n --type kotlin '"[A-Z][^"]{2,}"' \
    app/src/main/java/com/cadnative/firevisioniptv \
  | rg -v 'label\s*=\s*"' \
  | rg -v 'Log\.[dviwe]|\bTAG\b|const val|KEY_|testTag|contentDescription = null' \
  | rg -v 'route|navArgument|item\(key|items\(.*key|https?://' \
  | rg -v '@Query|@ColumnInfo|@SerializedName|Entity\(|tableName|execSQL' \
  | rg -v ':\s*//|:\s*\*|:\s*/\*' \
  | rg -v 'data/source/(remote|local)' \
  | rg -v 'Screen\.kt|AppPreferences\.kt|PlayerKeyAction|NavOptions\.kt' \
  | rg -v '"(loading|error|empty|content|dark|light|system|on|off|paired|m3u|xtream)"'
