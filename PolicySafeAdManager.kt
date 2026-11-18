package com.arovi.jobs.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.arovi.jobs.models.RemoteAdminConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object PolicySafeAdManager {
    private var rewardedAd: RewardedAd? = null

    fun loadRewardedAd(context: Context, config: RemoteAdminConfig) {
        if (config.emergencyAdsOff || !config.rewardedEnabled || !config.admobEnabled) return
        val unitId = if (config.testMode) "ca-app-pub-3940256099942544/5224354917" else config.admobRewardedId
        RewardedAd.load(context, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(err: LoadAdError) { rewardedAd = null }
        })
    }

    fun showUserInitiatedRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        rewardedAd?.show(activity) { onRewardEarned() }
    }
}

@Composable
fun PolicySafeBannerAd(config: RemoteAdminConfig) {
    if (config.emergencyAdsOff || !config.bannerEnabled || !config.admobEnabled) return
    val unitId = if (config.testMode) "ca-app-pub-3940256099942544/6300978111" else config.admobBannerId
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = unitId
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun SponsoredNativeSlot(config: RemoteAdminConfig) {
    if (config.emergencyAdsOff || !config.nativeEnabled) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0xFFE0E6ED), RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Sponsored",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6F00)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Global Career Accelerator & Resume Builder",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF0A2540)
        )
        Text(
            text = "Ad network mediation active (${if (config.admobEnabled) "AdMob" else "AppLovin MAX"})",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}