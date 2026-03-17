.PHONY: help tag tags debug release install clean lint test

# Default target
help:
	@echo ""
	@echo "FireVision IPTV — Available Commands"
	@echo "======================================"
	@echo ""
	@echo "Release Management:"
	@echo "  make tag VERSION=v1.2.3   Create and push an annotated release tag"
	@echo "  make tags                 List recent release tags (newest first)"
	@echo ""
	@echo "Gradle Shortcuts:"
	@echo "  make debug                Assemble debug APK"
	@echo "  make release              Assemble release APK"
	@echo "  make install              Build debug APK and install via adb"
	@echo "  make clean                Clean build outputs"
	@echo "  make lint                 Run Android lint checks"
	@echo "  make test                 Run unit tests"
	@echo ""

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

# ── Gradle shortcuts ─────────────────────────────────────────────────────────

debug:
	./gradlew assembleDebug

release:
	./gradlew assembleRelease

install:
	./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

clean:
	./gradlew clean

lint:
	./gradlew lint

test:
	./gradlew test
