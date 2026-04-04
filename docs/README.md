# FireVision IPTV Android TV App — Documentation

## Workflow

| Doc | Description |
|-----|-------------|
| [SETUP](workflow/SETUP.md) | Prerequisites, clone, build, Firebase, device setup, testing |
| [DEPLOYMENT_GUIDE](workflow/DEPLOYMENT_GUIDE.md) | Build variants, signing, Firebase, Fire TV test checklist |

## Architecture

| Doc | Description |
|-----|-------------|
| [ARCHITECTURE](ARCHITECTURE.md) | Clean Architecture layers, DI modules, key design decisions |
| [API](API.md) | REST API endpoints, repository interfaces, use cases, Room schema |
| [USER_GUIDE](USER_GUIDE.md) | Pairing, navigation, player controls, settings, troubleshooting |
| [APP_STARTUP_FLOW](APP_STARTUP_FLOW.md) | Splash → pairing → home flow with Mermaid diagrams |
| [PLAYER_BACK_PRESS_FLOW](PLAYER_BACK_PRESS_FLOW.md) | Player back press handling logic |

## Module Deep-Dives

| Doc | Description |
|-----|-------------|
| [modules/presentation](modules/presentation.md) | Screens, ViewModels, navigation, UI components, player infrastructure |
| [modules/data](modules/data.md) | Room database, Retrofit API, DTOs, mappers, repository implementations |
| [modules/domain](modules/domain.md) | Domain models, repository interfaces, use cases, services |

## Design

| Doc | Description |
|-----|-------------|
| [COLOR_PALETTE](COLOR_PALETTE.md) | Flame/Void/Parchment palette — Kotlin Color constants and theme mapping |

## Decisions

| ADR | Description |
|-----|-------------|
| [ADR-001](decisions/001-unified-color-palette.md) | Unified color palette across platforms |
| [ADR-002](decisions/002-pre-warm-viewmodel-during-splash.md) | Pre-warm ViewModel during splash screen |
