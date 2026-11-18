package com.arovi.jobs.api

import com.arovi.jobs.models.JobItem
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class JobicyResponse(val jobs: List<JobicyJob>?)
data class JobicyJob(
    val id: Int,
    val jobTitle: String?,
    val companyName: String?,
    val jobGeo: String?,
    val jobIndustry: List<String>?,
    val jobType: List<String>?,
    val annualSalaryMin: String?,
    val annualSalaryMax: String?,
    val jobExcerpt: String?,
    val url: String?
)

interface JobicyApi {
    @GET("api/v2/remote-jobs?count=30")
    suspend fun getJobs(): JobicyResponse
}

data class ArbeitnowResponse(val data: List<ArbeitnowJob>?)
data class ArbeitnowJob(
    val slug: String,
    val company_name: String?,
    val title: String?,
    val description: String?,
    val remote: Boolean?,
    val url: String?,
    val tags: List<String>?,
    val location: String?
)

interface ArbeitnowApi {
    @GET("api/job-board-api")
    suspend fun getJobs(): ArbeitnowResponse
}

object JobAggregator {
    private val jobicyApi = Retrofit.Builder()
        .baseUrl("https://jobicy.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(JobicyApi::class.java)

    private val arbeitnowApi = Retrofit.Builder()
        .baseUrl("https://www.arbeitnow.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ArbeitnowApi::class.java)

    suspend fun fetchNormalizedJobs(useJobicy: Boolean, useArbeitnow: Boolean): List<JobItem> {
        val results = mutableListOf<JobItem>()
        if (useJobicy) {
            try {
                val res = jobicyApi.getJobs().jobs.orEmpty()
                results.addAll(res.mapNotNull { j ->
                    val url = j.url ?: return@mapNotNull null
                    if (!url.startsWith("https://")) return@mapNotNull null
                    JobItem(
                        id = "jobicy_${j.id}",
                        title = j.jobTitle ?: "Untitled Role",
                        company = j.companyName ?: "Verified Employer",
                        location = j.jobGeo ?: "Worldwide",
                        country = j.jobGeo ?: "Global",
                        category = j.jobIndustry?.firstOrNull() ?: "General",
                        jobType = j.jobType?.firstOrNull() ?: "Remote",
                        salary = if (!j.annualSalaryMin.isNullOrBlank()) "$${j.annualSalaryMin} - $${j.annualSalaryMax}" else "Competitive",
                        description = j.jobExcerpt ?: "",
                        applyUrl = url,
                        source = "Jobicy",
                        postedAt = System.currentTimeMillis()
                    )
                })
            } catch (_: Exception) {}
        }
        if (useArbeitnow) {
            try {
                val res = arbeitnowApi.getJobs().data.orEmpty()
                results.addAll(res.mapNotNull { a ->
                    val url = a.url ?: return@mapNotNull null
                    if (!url.startsWith("https://")) return@mapNotNull null
                    JobItem(
                        id = "arbeitnow_${a.slug}",
                        title = a.title ?: "Untitled Role",
                        company = a.company_name ?: "European Employer",
                        location = a.location ?: "Europe",
                        country = "Europe",
                        category = a.tags?.firstOrNull() ?: "Tech",
                        jobType = if (a.remote == true) "Remote" else "On-site",
                        salary = "Not Disclosed",
                        description = a.description?.take(250) ?: "",
                        applyUrl = url,
                        source = "Arbeitnow",
                        postedAt = System.currentTimeMillis()
                    )
                })
            } catch (_: Exception) {}
        }
        // Deduplicate by title + company
        return results.distinctBy { "${it.title.lowercase()}_${it.company.lowercase()}" }
    }
}