.PHONY: help tag tags debug release install reinstall uninstall clean lint test \
       emulators emu devices run launch restart stop logcat setup

# ── Config ────────────────────────────────────────────────────────────────────

ANDROID_HOME  ?= $(HOME)/Library/Android/sdk
ANDROID_SDK_ROOT ?= $(ANDROID_HOME)
ADB           := $(ANDROID_HOME)/platform-tools/adb
EMULATOR      := $(ANDROID_HOME)/emulator/emulator
JAVA_HOME     := /opt/homebrew/opt/openjdk@17
export ANDROID_HOME
export ANDROID_SDK_ROOT
export JAVA_HOME
export PATH   := $(JAVA_HOME)/bin:$(ANDROID_HOME)/platform-tools:$(ANDROID_HOME)/emulator:$(PATH)

PACKAGE       := com.cadnative.firevisioniptv
ACTIVITY      := $(PACKAGE)/.ComposeMainActivity
APK           := app/build/outputs/apk/debug/app-debug.apk

# Default AVD (override with: make emu DEVICE=Pixel_8_Pro)
DEVICE        ?= Android_TV_1080p

# Default target
help:
	@echo ""
	@echo "FireVision IPTV — Available Commands"
	@echo "======================================"
	@echo ""
	@echo "Emulator & Device:"
	@echo "  make emulators            List available AVDs"
	@echo "  make emu                  Start emulator (default: $(DEVICE))"
	@echo "  make emu DEVICE=Name      Start a specific emulator AVD"
	@echo "  make devices              List connected devices/emulators"
	@echo ""
	@echo "Build & Run:"
	@echo "  make debug                Assemble debug APK"
	@echo "  make release              Assemble release APK"
	@echo "  make install              Build debug APK and install on device"
	@echo "  make reinstall            Uninstall app, then build and install fresh"
	@echo "  make uninstall            Remove app from device"
	@echo "  make run                  Build, install, and launch the app"
	@echo "  make launch               Launch app (skip build, must be installed)"
	@echo "  make restart              Force-stop and relaunch the app"
	@echo "  make stop                 Force-stop the app"
	@echo "  make clean                Clean build outputs"
	@echo ""
	@echo "Quality:"
	@echo "  make lint                 Run Android lint checks"
	@echo "  make test                 Run unit tests"
	@echo ""
	@echo "Setup:"
	@echo "  make setup                Install git hooks (lint before commit)"
	@echo ""
	@echo "Logging:"
	@echo "  make logcat               Show app logs (filtered to FireVision)"
	@echo ""
	@echo "Release Management:"
	@echo "  make tag VERSION=v1.2.3   Create and push an annotated release tag"
	@echo "  make tags                 List recent release tags (newest first)"
	@echo ""

# ── Emulator & Device ────────────────────────────────────────────────────────

emulators:
	@echo ""
	@echo "Available AVDs:"
	@echo "---------------"
	@$(EMULATOR) -list-avds
	@echo ""
	@echo "Start one with: make emu DEVICE=<avd-name>"
	@echo ""

emu:
	@echo "Starting emulator: $(DEVICE)..."
	$(EMULATOR) -avd $(DEVICE) -no-snapshot-load &
	@echo "Waiting for device to boot..."
	@$(ADB) wait-for-device
	@$(ADB) shell 'while [[ -z $$(getprop sys.boot_completed) ]]; do sleep 1; done'
	@echo "Emulator ready."

devices:
	@$(ADB) devices -l

# ── Build & Run ───────────────────────────────────────────────────────────────

debug:
	./gradlew assembleDebug

release:
	./gradlew assembleRelease

install:
	./gradlew assembleDebug && $(ADB) install -r $(APK)

reinstall: uninstall install

uninstall:
	@$(ADB) uninstall $(PACKAGE) 2>/dev/null || echo "App not installed, skipping."

run: install launch

launch:
	@echo "Launching $(PACKAGE)..."
	@$(ADB) shell am start -n $(ACTIVITY)

restart: stop launch

stop:
	@echo "Stopping $(PACKAGE)..."
	@$(ADB) shell am force-stop $(PACKAGE)

clean:
	./gradlew clean

# ── Quality ───────────────────────────────────────────────────────────────────

lint:
	./gradlew lint

test:
	./gradlew test

# ── Logging ───────────────────────────────────────────────────────────────────

logcat:
	@$(ADB) logcat --pid=$$($(ADB) shell pidof $(PACKAGE)) 2>/dev/null || \
		(echo "App not running. Showing all logs filtered by tag..." && \
		 $(ADB) logcat -s FireVision:* AndroidRuntime:E)

# ── Release tagging ──────────────────────────────────────────────────────────

tag:
ifndef VERSION
	$(error VERSION is required. Usage: make tag VERSION=v1.2.3)
endif
	@echo "$(VERSION)" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+$$' || \
		(echo "Error: VERSION must follow semantic versioning: vMAJOR.MINOR.PATCH (e.g. v1.2.3)" && exit 1)
	@echo "Creating annotated tag $(VERSION)..."
	git tag -a "$(VERSION)" -m "Release $(VERSION)"
	git push origin "$(VERSION)"
	@echo "Tag $(VERSION) created and pushed successfully."

tags:
	@echo ""
	@echo "Recent release tags (newest first):"
	@echo "-------------------------------------"
	@git tag --sort=-version:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+' | head -20 || echo "(no release tags found)"
	@echo ""

# ── Setup ─────────────────────────────────────────────────────────────────────

setup:
	git config core.hooksPath .githooks
	@echo "Git hooks installed. Lint will run before each commit."
