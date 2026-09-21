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
# Four traps this script is shaped around, each of which hid real strings:
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
#  4. A pass that finds nothing must not end the script. `rg` exits 1 when it
#     does not match, and under `set -e -o pipefail` that aborted the whole run
#     the moment pass 1 came back clean — so passes 2 and 3 never executed and
#     the script printed nothing and looked like success. Every pass is wrapped
#     in `|| true`. This one was the worst of the four: it made the tool report
#     clean precisely when it was closest to being useful.
#
# Pass 2 exists because hand-rolled plurals start lowercase
# (`if (n == 1) "channel" else "channels"`) and pass 1 cannot see them.
#
# Pass 3 exists because passes 1 and 2 both anchor on the literal's FIRST
# character, so one that opens with punctuation or interpolation slips through
# even when the words after it are plainly copy — `append("  ·  Recommended")`
# and `"$appVersion  ·  Up to date"` both did. It strips interpolations first, so
# a pure separator like `"  ·  ${size}"` reduces to punctuation and is ignored,
# while anything with real words left over is reported.
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

# Blank out ${...} and $identifier so a literal that is only a separator plus
# interpolation reduces to punctuation and drops out of pass 3.
strip_interpolation() {
  sed -E 's/\$\{[^}]*\}//g; s/\$[A-Za-z_][A-Za-z0-9_.]*//g'
}

# Each pass ends in `|| true`: an empty pass is a normal outcome, not a reason to
# stop (see trap 4).
pass1() {
  # Any capitalised literal — the bulk of UI copy.
  rg -n --type kotlin '"[A-Z][^"]{2,}"' "$SRC" \
    | strip_tokens | filters | rg '"[A-Z][^"]{2,}"' || true
}

pass2() {
  # Lowercase literals in slots that are always rendered, where hand-rolled
  # plurals live.
  rg -n --type kotlin '(text|title|subtitle|message|label|placeholder|hint)\s*=\s*"[a-z]' "$SRC" \
    | rg -v 'label\s*=\s*"[a-z]+([A-Z][A-Za-z]*)+"' \
    | strip_tokens | filters || true
}

# Rendering slots: where a literal becomes text a user reads. `append(` matters
# because buildAnnotatedString is how the update screens assemble their copy.
SLOTS='(append\(|text\s*=|title\s*=|subtitle\s*=|message\s*=|label\s*=|placeholder\s*=|hint\s*=)'

pass3() {
  # A rendered literal that OPENS with punctuation, whitespace or interpolation,
  # which is the one shape passes 1 and 2 cannot see. Deliberately scoped to the
  # rendering slots: matching words anywhere in any literal reported ~200 JSON
  # keys, analytics parameter names and category lookup keys, and a report that
  # size is one nobody reads.
  # The trailing "[^"]*\"" closes the literal on purpose: without it the regex
  # happily treats a CLOSING quote as an opening one and matches the code after
  # it, so `text = if (selected) "\u2713  " else label` looked like copy.
  # [^"]* between slot and literal, so a conditional still counts:
  # `text = if (upToDate) "$v  ·  Up to date" else v`.
  rg -n --type kotlin "$SLOTS"'[^"]*"[^A-Za-z"]' "$SRC" \
    | strip_interpolation | strip_tokens | filters \
    | rg '"[^"]*[A-Za-z]{3,}[^"]*"' || true
}

findings=$( { pass1; pass2; pass3; } | sort -u )
if [ -n "$findings" ]; then
  printf '%s\n' "$findings"
fi
# Self-check: pass 3's regex must still match a known-bad shape, so a future
# edit that breaks the pattern cannot masquerade as a clean codebase.
probe='x.kt:1: append("  \xc2\xb7  Recommended")'
if ! printf '%b\n' "$probe" | rg -q '"[^"]*[A-Za-z]{3,}[^"]*"'; then
  echo "find-hardcoded-strings.sh: pass 3 pattern no longer matches its probe" >&2
  exit 2
fi
