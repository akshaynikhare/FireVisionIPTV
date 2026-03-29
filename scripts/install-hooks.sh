#!/bin/sh
# Install git hooks for this repository.
# Run once after cloning: sh scripts/install-hooks.sh

set -e

HOOKS_DIR="$(git rev-parse --git-dir)/hooks"

install_hook() {
    HOOK_NAME="$1"
    HOOK_CONTENT="$2"
    HOOK_PATH="$HOOKS_DIR/$HOOK_NAME"

    printf '%s\n' "$HOOK_CONTENT" > "$HOOK_PATH"
    chmod +x "$HOOK_PATH"
    echo "Installed $HOOK_NAME hook."
}

COMMIT_MSG_HOOK='#!/bin/sh
# Reject commit messages that reference internal AI tooling.
COMMIT_MSG_FILE="$1"
COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")
FORBIDDEN="paperclip|claude|codex|generated with|ai agent|noreply@paperclip"
MATCH=$(echo "$COMMIT_MSG" | grep -iE "$FORBIDDEN" || true)
if [ -n "$MATCH" ]; then
    echo ""
    echo "error: commit message contains a reference to AI tooling."
    echo ""
    echo "The following content is not allowed:"
    echo "$MATCH"
    echo ""
    echo "Do not reference: paperclip, claude, codex, generated with,"
    echo "ai agent, Co-Authored-By: Paperclip, or Co-Authored-By: Claude."
    echo "See CONTRIBUTING.md for the full policy."
    echo ""
    exit 1
fi'

install_hook "commit-msg" "$COMMIT_MSG_HOOK"

echo "All hooks installed successfully."
