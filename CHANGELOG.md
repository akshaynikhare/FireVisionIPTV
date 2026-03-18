# Changelog

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
