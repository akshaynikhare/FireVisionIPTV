#!/usr/bin/env bash
# Lists candidate user-facing string literals still inline in Kotlin.
#
# There is no lint check for hardcoded strings in Compose (HardcodedText is
# XML-only), so this is the standing substitute: run it before and after a
# migration and review the delta.
#
# The exclusions are not cosmetic. Crossfade state discriminators, persisted
# preference tokens and route strings all look like UI text and are all
# load-bearing — localising any of them breaks behaviour rather than just
# wording.
#
# Three traps this script is shaped around, each of which hid real strings:
#
#  1. Path exclusions must be anchored on the directory. A bare "Screen\.kt"
#     also matches FavoritesScreen.kt, PlayerScreen.kt and every other screen
#     file — which is where most of the UI text lives.
#  2. Token exclusions must delete the token, not the line. A Crossfade
#     discriminator sits on the same line as real text, e.g.
#     `"empty" -> EmptyState(message = "No favorites yet")`.
#  3. There is deliberately NO `label =` exclusion: every animation label here
#     is camelCase, so the leading-capital pass skips them anyway, while
#     `label =` is also how real text reaches the filter chips and
#     PortraitActionButton.
#
# Pass 2 exists because hand-rolled plurals start lowercase
# (`if (n == 1) "channel" else "channels"`) and pass 1 cannot see them.
set -euo pipefail
cd "$(dirname "$0")/.."

SRC=app/src/main/java/com/cadnative/firevisioniptv

# Blank out load-bearing tokens in place so a real string sharing the line
# survives. Keep in sync with the persisted values in AppPreferences and the
# Crossfade/AnimatedContent keys.
strip_tokens() {
  sed -E 's/"(loading|error|empty|content|dark|light|system|on|off|paired|m3u|xtream|all|favorites|completed|expired)"/""/g'
}

filters() {
  rg -v 'Log\.[dviwe]|\bTAG\b|const val|KEY_|testTag|contentDescription = null' \
    | rg -v 'route|navArgument|item\(key|items\(.*key|https?://' \
    | rg -v '@Query|@ColumnInfo|@SerializedName|Entity\(|tableName|execSQL' \
    | rg -v ':\s*//|:\s*\*|:\s*/\*' \
    | rg -v 'data/source/(remote|local)' \
    | rg -v 'di/NetworkModule|update/ApkSignatures|Suppress(Lint)?\(' \
    | rg -v 'DateTimeFormatter\.ofPattern|SimpleDateFormat\(|URLEncoder\.encode|URLDecoder\.decode' \
    | rg -v 'Result\.Error\(|IllegalArgumentException\(|SecurityException\(|IllegalStateException\(' \
    | rg -v 'exType ==' \
    | rg -v 'ui/player/ErrorRecoveryManager|ui/player/StreamErrorMessageResolver' \
    | rg -v 'domain/service/ChannelHealthScanner' \
    | rg -v 'navigation/Screen\.kt|AppPreferences\.kt|PlayerKeyAction|NavOptions\.kt' \
    | rg -v 'mapOf\("(Accept|Authorization|Content-Type|X-)' \
    | rg -v '"KMGTPE"' \
    | rg -v 'rememberInfiniteTransition|updateTransition|animate[A-Za-z]*AsState' \
    | rg -v 'Typeface\.' \
    | rg -v 'onStreamDead\('
}

{
  # Pass 1: any capitalised literal — the bulk of UI copy.
  rg -n --type kotlin '"[A-Z][^"]{2,}"' "$SRC" | strip_tokens | filters | rg '"[A-Z][^"]{2,}"'

  # Pass 2: lowercase literals in slots that are always rendered.
  rg -n --type kotlin '(text|title|subtitle|message|label|placeholder|hint)\s*=\s*"[a-z]' "$SRC" \
    | rg -v 'label\s*=\s*"[a-z]+([A-Z][A-Za-z]*)+"' \
    | strip_tokens | filters
} | sort -u
