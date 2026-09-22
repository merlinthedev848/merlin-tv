package com.example.merlinmedia.di

import android.content.Context
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.data.NetworkMonitor
import com.example.merlinmedia.data.local.AppDatabase
import com.example.merlinmedia.data.local.dao.FavoritesDao
import com.example.merlinmedia.data.local.dao.RecentHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideFavoritesDao(
        database: AppDatabase
    ): FavoritesDao = database.favoritesDao()

    @Provides
    @Singleton
    fun provideRecentHistoryDao(
        database: AppDatabase
    ): RecentHistoryDao = database.recentHistoryDao()

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor = NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideFavoritesManager(
        @ApplicationContext context: Context
    ): FavoritesManager = FavoritesManager(context)
}
