package com.arovi.jobs.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs_cache")
data class JobItem(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val location: String,
    val country: String,
    val category: String,
    val jobType: String, // Remote, On-site, Hybrid, Freelance, Full-time, Part-time, Internship, Contract, Temporary
    val salary: String,
    val description: String,
    val applyUrl: String,
    val source: String, // Jobicy, RemoteOK, Arbeitnow, Adzuna, Jooble, USAJOBS
    val postedAt: Long,
    val isFeatured: Boolean = false
)

@Entity(tableName = "saved_jobs")
data class SavedJob(
    @PrimaryKey val jobId: String,
    val title: String,
    val company: String,
    val location: String,
    val salary: String,
    val applyUrl: String,
    val source: String,
    val status: String = "Saved", // Saved, Applied, Interview, Offer, Rejected
    val savedAt: Long = System.currentTimeMillis()
)

data class RemoteAdminConfig(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "System under scheduled maintenance.",
    val announcement: String = "",
    val admobEnabled: Boolean = true,
    val applovinEnabled: Boolean = false,
    val bannerEnabled: Boolean = true,
    val nativeEnabled: Boolean = true,
    val rewardedEnabled: Boolean = true,
    val emergencyAdsOff: Boolean = false,
    val testMode: Boolean = true,
    val nativeAdFrequency: Int = 5,
    val admobBannerId: String = "ca-app-pub-3940256099942544/6300978111",
    val admobNativeId: String = "ca-app-pub-3940256099942544/2247696110",
    val admobRewardedId: String = "ca-app-pub-3940256099942544/5224354917",
    val applovinBannerId: String = "",
    val applovinRewardedId: String = "",
    val jobicyEnabled: Boolean = true,
    val remoteOkEnabled: Boolean = true,
    val arbeitnowEnabled: Boolean = true
)