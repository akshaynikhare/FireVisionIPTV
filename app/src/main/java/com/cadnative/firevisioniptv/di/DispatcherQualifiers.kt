package com.cadnative.firevisioniptv.di

import javax.inject.Qualifier

/**
 * Qualifier annotation for IO dispatcher.
 * Use this for background operations like network calls and database operations.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier annotation for Main dispatcher.
 * Use this for UI operations that need to run on the main thread.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
