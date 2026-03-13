# FireVision IPTV - Modernized Architecture

## Overview

FireVision IPTV has been modernized with Clean Architecture, MVVM pattern, and modern Android libraries. This document describes the new architecture and implementation details.

## Architecture Layers

### 1. Presentation Layer
**Location**: `presentation/`

**Components**:
- **UI Models**: Data classes optimized for UI display
- **ViewModels**: State management with StateFlow
- **Screens**: Jetpack Compose UI for Android TV
- **Navigation**: Navigation Component with type-safe routes

**Key Features**:
- Compose for TV with focus management
- Material Design 3 theming
- TV-optimized typography (minimum 16sp)
- Reactive UI updates with Flow

### 2. Domain Layer
**Location**: `domain/`

**Components**:
- **Models**: Pure business logic entities
- **Repositories**: Interface definitions
- **Use Cases**: Single-responsibility business operations

**Key Features**:
- Framework-independent
- Testable business logic
- Clear separation of concerns

### 3. Data Layer
**Location**: `data/`

**Components**:
- **Repositories**: Implementation of domain interfaces
- **Data Sources**: Local (Room) and Remote (Retrofit)
- **Mappers**: Entity ↔ Domain ↔ DTO transformations
- **DTOs**: API response models

**Key Features**:
- Offline-first architecture
- Room database as single source of truth
- Automatic data synchronization
- Error handling with Result wrapper

## Key Technologies

- **Language**: Kotlin 1.9.24
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt 2.51
- **Database**: Room 2.6.1
- **Networking**: Retrofit 2.11.0
- **UI**: Jetpack Compose for TV 1.0.0-alpha10
- **Video**: Media3 ExoPlayer 1.4.1
- **Images**: Coil 2.7.0
- **Async**: Coroutines 1.8.0 + Flow

## Data Flow

```
UI (Compose) 
  ↓
ViewModel (StateFlow)
  ↓
Use Case
  ↓
Repository
  ↓
Data Sources (Local + Remote)
```

## Offline-First Strategy

1. **Immediate Response**: UI displays cached data instantly
2. **Background Sync**: Fetch fresh data from server
3. **Update Cache**: Store new data in Room database
4. **Reactive Updates**: Flow emits new data to UI

## State Management

- **StateFlow**: Reactive state in ViewModels
- **Immutable State**: UI state classes are data classes
- **Single Source of Truth**: ViewModels own the state
- **Lifecycle Aware**: Automatic cleanup with viewModelScope

## Dependency Injection

All dependencies managed by Hilt:
- **AppModule**: Application-level dependencies
- **DatabaseModule**: Room database and DAOs
- **NetworkModule**: Retrofit and API service
- **RepositoryModule**: Repository implementations
- **ImageLoadingModule**: Coil configuration

## Testing Strategy

- **Unit Tests**: ViewModels, Use Cases, Repositories
- **Integration Tests**: Database operations, API calls
- **UI Tests**: Compose screens with testing library
- **Property-Based Tests**: Correctness validation (optional)

## Performance Optimizations

- **Image Caching**: Memory (25%) + Disk (50MB) cache
- **Database Indexing**: Optimized queries
- **Background Sync**: WorkManager every 6 hours
- **Lazy Loading**: On-demand initialization
- **Hardware Acceleration**: Enabled for bitmaps

## Security

- **Encrypted Preferences**: Sensitive data protection
- **HTTPS Only**: Network security configuration
- **ProGuard/R8**: Code obfuscation for release
- **No Hardcoded Secrets**: BuildConfig for API keys

## Package Structure

```
com.cadnative.firevisioniptv/
├── data/
│   ├── mapper/          # Data transformations
│   ├── model/           # DTOs and Result wrapper
│   ├── repository/      # Repository implementations
│   └── source/
│       ├── local/       # Room database
│       └── remote/      # Retrofit API
├── domain/
│   ├── model/           # Business entities
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business operations
├── presentation/
│   ├── mapper/          # UI mappers
│   ├── model/           # UI models and states
│   ├── navigation/      # Navigation graph
│   ├── ui/
│   │   ├── components/  # Reusable UI components
│   │   ├── screens/     # Compose screens
│   │   ├── theme/       # Material Design theme
│   │   └── utils/       # UI utilities
│   └── viewmodel/       # ViewModels
├── di/                  # Hilt modules
├── worker/              # WorkManager workers
└── security/            # Security utilities
```

## Migration Notes

- All Java code converted to Kotlin
- Legacy Leanback fragments replaced with Compose
- Glide replaced with Coil
- ExoPlayer upgraded to Media3
- Manual DI replaced with Hilt
- Callbacks replaced with Coroutines/Flow

## Next Steps

1. Add remaining optional tests
2. Implement accessibility features
3. Set up CI/CD pipeline
4. Prepare release build
5. Conduct final QA on devices

---

**Version**: 1.0  
**Last Updated**: 2026-03-13  
**Status**: Core Implementation Complete
