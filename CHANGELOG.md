# Changelog

## [2.1.2] - 2026-04-04

### Added
- enhance Favorites feature to include favorite categories and navigation
- add ChannelsByCategory route to sidebar navigation
- enhance ChannelCard to display logo overlay on thumbnail

---

## [2.1.1] - 2026-04-04

### Added
- Integrate Amazon Appstore DRM for license verification and update ProGuard rules

---

## [2.1.0] - 2026-04-04

### Added
- Add header redaction for X-TV-Code in NetworkModule and recycle QR code bitmap in SettingsViewModel
- Enhance channel synchronization and playback handling in ChannelManager and PlayerScreen
- Implement Box overlay pattern for splash screen to pre-warm ViewModel
- Enhance channel management and UI with cache clearing functionality, improved empty states, and app startup flow documentation
- Add FUNDING.yml for GitHub sponsorship and support options

### Changed
- Update README for clarity and formatting improvements

### Other
- Add documentation for plaintext TV code storage decision, app startup flow, deployment guide, favorites workflow, health scanner lifecycle, player back press flow, and developer setup guide
- Refactor ChannelManager to use Hilt for dependency injection, sync EPG data, and improve channel management

---

## [2.0.5] - 2026-04-03

### Added
- Update splash animation colors and enhance font mapping in SplashScreen
- Implement pullFavoritesFromServer use case and related tests
- Implement unified color palette across FireVision IPTV app
- show current EPG program title on channel cards
- add unit tests for error handling in repository classes and update dependencies for testing
- add pre-commit hook for Android lint checks and update Makefile for setup instructions
- integrate Sentry for error tracking and reporting in the Android app
- add Sentry crash tracking and Jacoco/Codecov coverage reporting
- add portrait orientation support for Android phone users
- add EPG Phase 1 now/next program display in player overlay

### Changed
- add Android app user guide for end users
- streamline TV code management by centralizing SharedPreferences access
- update CHANGELOG.md for v2.0.4
- update preview assets by removing obsolete video and adding new images

### Fixed
- Update release tagging command to use 'v' instead of 'VERSION' for consistency
- Update border color references to use subtleBorder for consistency across components
- Implement long-press favorite toggle and enhance channel navigation logic
- Enhance category chip focus effects and improve visual feedback
- fix:#35 Improve thumbnail loading and enhance favorite button auto-hide logic
- update CI workflow to generate coverage report and set Sentry environment variables
- set ANDROID_SDK_ROOT and export environment variables in Makefile
- address PR review feedback for portrait mode
- add InnerClasses attribute to proguard keep rules for Gson TypeToken
- add EnclosingMethod and TypeToken keep rules to prevent Gson crash on release builds
- settings key event consumed when no handler, remove unused parameter
- Fire TV remote optimization — D-pad focus, OK/Menu/Settings buttons, debouncing

### Other
- Enhance UI state management and improve color theming
- Refactor app icons and backgrounds for improved design consistency
- Fix race condition in EpgRepositoryImpl cache loading
- Apply suggestions from code review
- Add GitHub issue templates (bug, feature request, remote navigation)

---

## [2.0.4] - 2026-03-21

### Added
- implement alternate stream fallback and enhance stream reporting
- enhance stream reporting with proxy support and update serialized names
- add stream metrics reporting and bidirectional favorites sync

### Changed
- add server-side implementation context for stream metrics

### Fixed
- adjust card dimensions and improve layout responsiveness across screens
- prevent FK crash in pullFavorites and suppress buffer watch during recovery
- wire onStreamUnresponsive callback and reset play tracker on load

### Other
- Add GitHub issue drafts for stream metrics feature

---

## [2.0.3] - 2026-03-18

### Added
- expand Makefile with emulator, device, and app lifecycle commands

### Changed
- remove legacy SettingsActivity, autoload channel feature, and extract CategoryCard component

### Fixed
- update playlist endpoint path to /api/v1/channels/playlist.m3u
- rename auth header from X-Session-ID to X-TV-Code to match server API
- defer keyboard popup on TV text inputs until explicit OK press
- improve error handling with auth-aware states and redesign settings layout
- overhaul pairing flow with PIN-based QR codes, reset support, and race condition fixes

### Other
- Fix Fire TV launcher banner and increase card sizes for TV viewing

---

## [2.0.2] - 2026-03-18

_(No notable changes recorded)_

---

All notable changes to FireVision IPTV are documented in this file.

## [2.0] - 2026-03-15

Major architecture modernization and full Kotlin migration.

### Architecture
- Migrated entire codebase from Java to Kotlin
- Implemented clean architecture with domain layer, repositories, and ViewModels
- Modernized build system and implemented Room database architecture
- Implemented ViewModel-based state management for channels

### Features
- Added screens for Channels, Favorites, Home, Player, Search, and Settings with new UI components
- Enhanced Player and Search screens with Pairing functionality
- Implemented animated splash screen with gradient background
- Enhanced PairingActivity with countdown functionality and improved handler management
- Added app version display and check-for-updates in settings

### UI/UX
- Refactored SettingsScreen and related components for improved UI
- Updated drawable resources for app icons and banners

### Cleanup
- Removed unused animation files

---

## [1.5] - 2025-11-27

### Features
- Implemented category and language selection UI
- Netflix-style UI enhancements with error handling and default channel options
- Enhanced PlaybackActivity with wake lock and improved key handling
- Refactored settings layout for Netflix-style UI and sidebar navigation
- Added playlist support

### Cleanup
- Removed Realm and Firebase Firestore dependencies
- Removed SearchActivity/SearchFragment and associated layouts
- Cleaned up unused fileReader class and related XML layouts
- Removed outdated build fix documentation

---

## [1.4] - 2025-11-01

### Features
- Implemented sidebar navigation and updated UI components
- Implemented Channel Overlay feature
- Enhanced search UI components
- Implemented update management and loading indicators

### DevOps
- Added GitHub build action

---

## [1.3] - 2024-06-05

### Features
- Added search functionality in the app

---

## [1.2] - 2024-06-05

_(No user-facing changes — internal release)_

---

## [1.1] - 2024-06-05

### Features
- Added categories support
- Version bump

---

## [1.0] - 2024-06-05

### Initial Release
- Initial commit with core IPTV functionality
- Firebase and Google Services integration
- CI/CD pipeline setup with GitHub Actions
