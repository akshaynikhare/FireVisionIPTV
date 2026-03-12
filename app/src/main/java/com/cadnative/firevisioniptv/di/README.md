# Dependency Injection with Hilt

This directory contains Hilt modules and qualifiers for dependency injection throughout the FireVision IPTV app.

## Setup Complete

✅ **FireVisionApplication** - Annotated with `@HiltAndroidApp`
✅ **AppModule** - Provides application-level dependencies
✅ **Dispatcher Qualifiers** - `@IoDispatcher` and `@MainDispatcher` for coroutine dispatchers

## Usage Examples

### Injecting Dependencies in Activities

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // Dependencies will be injected by Hilt
}
```

### Injecting Dependencies in Fragments

```kotlin
@AndroidEntryPoint
class MyFragment : Fragment() {
    // Dependencies will be injected by Hilt
}
```

### Injecting Dependencies in ViewModels

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    // ViewModel with injected dependencies
}
```

### Using Dispatchers

```kotlin
@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    fun loadChannels() {
        viewModelScope.launch(ioDispatcher) {
            // Background work on IO dispatcher
            val channels = getChannelsUseCase()
            
            withContext(mainDispatcher) {
                // Update UI on Main dispatcher
                _uiState.value = channels
            }
        }
    }
}
```

## Available Modules

### AppModule
Provides:
- Application Context
- IO Dispatcher (`@IoDispatcher`)
- Main Dispatcher (`@MainDispatcher`)

### DatabaseModule
Provides:
- FireVisionDatabase singleton
- ChannelDao
- CategoryDao
- FavoriteDao
- SearchHistoryDao
- PlaybackPositionDao

### NetworkModule
Provides:
- OkHttpClient with logging interceptor, timeouts, and custom headers
- Retrofit instance with base URL from BuildConfig
- FireVisionApiService for API calls

**Network Security:**
- HTTPS-only connections enforced via network security configuration
- Connect/Read/Write timeouts: 30 seconds
- Logging enabled in debug builds only
- Custom headers: Accept and Content-Type set to application/json

## Next Steps

Additional Hilt modules will be created as the modernization progresses:
- **RepositoryModule** - Repository implementations (Phase 3-4)
- **ImageLoadingModule** - Coil image loader (Phase 9)

## Requirements

This setup satisfies requirement **TR-003**: Architecture Modernization
- Implements proper dependency injection with Hilt
- Provides coroutine dispatchers for async operations
- Enables testability through constructor injection
