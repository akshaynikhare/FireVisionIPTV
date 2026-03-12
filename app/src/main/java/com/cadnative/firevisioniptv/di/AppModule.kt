package com.cadnative.firevisioniptv.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies.
 * Installed in SingletonComponent for app-wide availability.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides application context.
     */
    @Provides
    @Singleton
    fun provideContext(application: Application): Context = application

    /**
     * Provides IO dispatcher for background operations.
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides Main dispatcher for UI operations.
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
