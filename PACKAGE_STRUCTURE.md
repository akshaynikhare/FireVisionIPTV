# FireVision IPTV - Clean Architecture Package Structure

This document describes the modular package structure created for the FireVision IPTV modernization project following Clean Architecture principles.

## Package Structure Overview

```
com.cadnative.firevisioniptv/
├── di/                          # Dependency Injection
│   └── package-info.java        # Hilt modules for providing dependencies
│
├── data/                        # Data Layer
│   ├── source/                  # Data sources (local, remote, cache)
│   │   └── package-info.java
│   ├── repository/              # Repository implementations
│   │   └── package-info.java
│   ├── mapper/                  # Data model mappers (DTO ↔ Domain)
│   │   └── package-info.java
│   └── model/                   # Data models (DTOs, Entities)
│       └── package-info.java
│
├── domain/                      # Domain Layer
│   ├── model/                   # Domain models (business entities)
│   │   └── package-info.java
│   ├── repository/              # Repository interfaces
│   │   └── package-info.java
│   └── usecase/                 # Use cases (business logic)
│       └── package-info.java
│
└── presentation/                # Presentation Layer
    ├── ui/                      # UI components (Activities, Fragments, Compose)
    │   └── package-info.java
    ├── viewmodel/               # ViewModels
    │   └── package-info.java
    ├── mapper/                  # UI model mappers (Domain ↔ UI)
    │   └── package-info.java
    └── navigation/              # Navigation graphs and routes
        └── package-info.java
```

## Layer Responsibilities

### Dependency Injection Layer (`di/`)
- Contains Hilt modules for dependency injection
- Provides application-level dependencies
- Configures dispatchers, database, network, repositories

### Data Layer (`data/`)
Responsible for data management and persistence:

- **`source/`**: Data source implementations
  - Local data sources (Room database)
  - Remote data sources (Retrofit API)
  - Cache data sources (in-memory)

- **`repository/`**: Repository pattern implementations
  - Implements repository interfaces from domain layer
  - Coordinates between local, remote, and cache sources
  - Implements offline-first strategy

- **`mapper/`**: Data model mappers
  - Converts DTOs to domain models
  - Converts domain models to entities
  - Ensures data consistency

- **`model/`**: Data models
  - DTOs for API responses
  - Room entities for database
  - Request/response wrappers

### Domain Layer (`domain/`)
Contains business logic and is framework-independent:

- **`model/`**: Domain models
  - Core business entities
  - Framework-independent
  - Represent business concepts

- **`repository/`**: Repository interfaces
  - Define data operation contracts
  - No implementation details
  - Used by use cases

- **`usecase/`**: Use cases
  - Encapsulate business logic
  - Orchestrate data flow
  - Single responsibility per use case

### Presentation Layer (`presentation/`)
Handles UI and user interactions:

- **`ui/`**: UI components
  - Activities and Fragments
  - Jetpack Compose screens
  - Reusable UI components
  - Leanback components

- **`viewmodel/`**: ViewModels
  - Manage UI state
  - Handle user interactions
  - Connect UI to domain layer
  - Use StateFlow for reactive updates

- **`mapper/`**: UI model mappers
  - Convert domain models to UI models
  - Prepare data for display
  - Handle UI-specific transformations

- **`navigation/`**: Navigation
  - Navigation graphs
  - Route definitions
  - Navigation utilities

## Dependency Rules

Following Clean Architecture principles:

1. **Dependencies point inward**: Outer layers depend on inner layers
2. **Domain layer is independent**: No dependencies on framework or UI
3. **Data layer depends on domain**: Implements domain interfaces
4. **Presentation layer depends on domain**: Uses domain models and use cases

```
Presentation → Domain ← Data
     ↓           ↑
     └─────────────────→ DI
```

## Migration Strategy

This package structure supports incremental migration:

1. New features will be implemented in the new structure
2. Existing code remains in the root package temporarily
3. Gradual migration of existing features to new structure
4. Backward compatibility maintained during transition

## Next Steps

1. Set up Hilt dependency injection (Task 1.4)
2. Implement Room database schema (Phase 2)
3. Create repository implementations (Phase 3)
4. Implement domain models and use cases (Phase 4)
5. Create ViewModels and UI components (Phase 5-6)

## References

- Requirements: TR-003 (Architecture Modernization)
- Design Document: Section 2 (Architecture Design)
- Tasks: Phase 1, Task 1.3
