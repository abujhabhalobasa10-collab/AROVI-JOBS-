package com.arovi.jobs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arovi.jobs.ads.PolicySafeBannerAd
import com.arovi.jobs.ads.SponsoredNativeSlot
import com.arovi.jobs.api.JobAggregator
import com.arovi.jobs.database.AppDatabase
import com.arovi.jobs.firebase.RemoteConfigManager
import com.arovi.jobs.models.JobItem
import com.arovi.jobs.models.SavedJob
import kotlinx.coroutines.launch

val NavyPrimary = Color(0xFF0A2540)
val RoyalBlue = Color(0xFF1565C0)
val AccentOrange = Color(0xFFFF6F00)
val SurfaceLight = Color(0xFFF4F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = RoyalBlue,
                    secondary = AccentOrange,
                    background = SurfaceLight
                )
            ) {
                AroviMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AroviMainApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val config by RemoteConfigManager.config.collectAsState()
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedLanguage by remember { mutableStateOf("English") }
    val cachedJobs by db.jobDao().getAllCachedJobs().collectAsState(initial = emptyList())
    val savedJobs by db.jobDao().getSavedJobs().collectAsState(initial = emptyList())

    LaunchedEffect(config.jobicyEnabled, config.arbeitnowEnabled) {
        val fresh = JobAggregator.fetchNormalizedJobs(config.jobicyEnabled, config.arbeitnowEnabled)
        if (fresh.isNotEmpty()) {
            db.jobDao().insertJobs(fresh)
        }
    }

    if (config.maintenanceMode) {
        Box(modifier = Modifier.fillMaxSize().background(NavyPrimary), contentAlignment = Alignment.Center) {
            Text(config.maintenanceMessage, color = Color.White, fontWeight = FontWeight.Bold)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AROVI Jobs", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Find Your Dream Job Anywhere", fontSize = 11.sp, color = Color(0xFFBBDEFB))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            Column {
                PolicySafeBannerAd(config)
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        Pair("Discover", Icons.Default.Search),
                        Pair("Countries", Icons.Default.Public),
                        Pair("Saved (${savedJobs.size})", Icons.Default.Bookmark),
                        Pair("Settings", Icons.Default.Settings)
                    )
                    items.forEachIndexed { idx, item ->
                        NavigationBarItem(
                            selected = currentTab == idx,
                            onClick = { currentTab = idx },
                            icon = { Icon(item.second, contentDescription = item.first) },
                            label = { Text(item.first, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(SurfaceLight)) {
            if (config.announcement.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().background(AccentOrange).padding(8.dp)) {
                    Text(config.announcement, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            when (currentTab) {
                0 -> {
                    // Search & Filters Header (Fiverr/Upwork Marketplace Style)
                    Column(modifier = Modifier.background(NavyPrimary).padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search jobs, skills, companies...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val types = listOf("All", "Remote", "Full-time", "Freelance", "Contract", "Hybrid", "Internship")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(types) { type ->
                                FilterChip(
                                    selected = selectedFilter == type,
                                    onClick = { selectedFilter = type },
                                    label = { Text(type) }
                                )
                            }
                        }
                    }

                    val filtered = cachedJobs.filter { job ->
                        (selectedFilter == "All" || job.jobType.contains(selectedFilter, true)) &&
                        (searchQuery.isBlank() || job.title.contains(searchQuery, true) || job.company.contains(searchQuery, true))
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filtered) { index, job ->
                            JobMarketplaceCard(
                                job = job,
                                isSaved = savedJobs.any { it.jobId == job.id },
                                onSave = {
                                    scope.launch {
                                        db.jobDao().saveJob(
                                            SavedJob(job.id, job.title, job.company, job.location, job.salary, job.applyUrl, job.source)
                                        )
                                    }
                                },
                                onApplyExternal = {
                                    if (job.applyUrl.startsWith("https://")) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(job.applyUrl)))
                                    }
                                }
                            )
                            if ((index + 1) % config.nativeAdFrequency == 0) {
                                SponsoredNativeSlot(config)
                            }
                        }
                    }
                }
                1 -> {
                    val countries = listOf("Worldwide Remote", "United States", "United Kingdom", "Germany", "Canada", "Malaysia", "Singapore", "Australia", "UAE", "Japan")
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { Text("Global Country Explorer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        items(countries) { country ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    searchQuery = if (country == "Worldwide Remote") "" else country
                                    currentTab = 0
                                },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = RoyalBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(country, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { Text("Offline Saved & Application Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        items(savedJobs) { sJob ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(sJob.title, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    Text("${sJob.company} • ${sJob.location}", fontSize = 13.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Saved", "Applied", "Interview").forEach { st ->
                                            AssistChip(
                                                onClick = { scope.launch { db.jobDao().updateApplicationStatus(sJob.jobId, st) } },
                                                label = { Text(st) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val langs = listOf("English", "Bengali (বাংলা)", "Hindi", "Arabic (العربية)", "Malay", "Indonesian", "Spanish", "French", "Portuguese", "German", "Chinese", "Japanese")
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Text("Language & Privacy Policy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        items(langs) { lang ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedLanguage = lang },
                                colors = CardDefaults.cardColors(containerColor = if (selectedLanguage == lang) Color(0xFFE3F2FD) else Color.White)
                            ) {
                                Text(lang, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Medium)
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Disclaimer: AROVI Jobs aggregates public job feeds and redirects directly to official employer portals. Zero personal data is stored on cloud servers.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobMarketplaceCard(job: JobItem, isSaved: Boolean, onSave: () -> Unit, onApplyExternal: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(6.dp)) {
                    Text(job.jobType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = RoyalBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Source: ${job.source}", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSave) {
                    Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Save", tint = AccentOrange)
                }
            }
            Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Text("${job.company} • ${job.location}", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(job.salary, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApplyExternal,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply on Official Employer Site", fontWeight = FontWeight.Bold)
            }
        }
    }
}