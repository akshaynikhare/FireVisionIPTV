@echo off
echo Setting up git hooks...
git config core.hooksPath .githooks
echo Git hooks installed. Lint will run before each commit.
