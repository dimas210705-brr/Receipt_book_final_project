package com.example.recipebook.di

import android.content.Context
import androidx.room.Room
import com.example.recipebook.data.local.CollectionsDao
import com.example.recipebook.data.local.FavouriteDao
import com.example.recipebook.data.local.HistoryDao
import com.example.recipebook.data.local.OfflineRecipeDao
import com.example.recipebook.data.local.RecipeBookDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecipeBookDatabase =
        Room.databaseBuilder(
            context,
            RecipeBookDatabase::class.java,
            RecipeBookDatabase.NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideFavouriteDao(database: RecipeBookDatabase): FavouriteDao =
        database.favouriteDao()

    @Provides
    fun provideOfflineRecipeDao(database: RecipeBookDatabase): OfflineRecipeDao =
        database.offlineRecipeDao()

    @Provides
    fun provideHistoryDao(database: RecipeBookDatabase): HistoryDao =
        database.historyDao()

    @Provides
    fun provideCollectionsDao(database: RecipeBookDatabase): CollectionsDao =
        database.collectionsDao()
}
