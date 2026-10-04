package com.example.recipebook.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.recipebook.data.local.FavouriteDao
import com.example.recipebook.data.repository.OfflineFirstRecipeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class OfflinePreloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val favouriteDao: FavouriteDao,
    private val recipeRepository: OfflineFirstRecipeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val favourites = favouriteDao.observeAll().first()
            for (fav in favourites) {
                recipeRepository.refreshRecipe(fav.id, force = false)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "offline_preload_worker"
    }
}
