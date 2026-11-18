package com.arovi.jobs.database

import android.content.Context
import androidx.room.*
import com.arovi.jobs.models.JobItem
import com.arovi.jobs.models.SavedJob
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs_cache ORDER BY postedAt DESC")
    fun getAllCachedJobs(): Flow<List<JobItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobItem>)

    @Query("SELECT * FROM saved_jobs ORDER BY savedAt DESC")
    fun getSavedJobs(): Flow<List<SavedJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveJob(job: SavedJob)

    @Delete
    suspend fun removeSavedJob(job: SavedJob)

    @Query("UPDATE saved_jobs SET status = :status WHERE jobId = :jobId")
    suspend fun updateApplicationStatus(jobId: String, status: String)
}

@Database(entities = [JobItem::class, SavedJob::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "arovi_local_db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}