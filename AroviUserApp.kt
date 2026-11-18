package com.arovi.jobs

import android.app.Application
import com.applovin.sdk.AppLovinSdk
import com.arovi.jobs.firebase.RemoteConfigManager
import com.google.android.gms.ads.MobileAds

class AroviUserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        AppLovinSdk.getInstance(this).setMediationProvider("max")
        AppLovinSdk.getInstance(this).initializeSdk {}
        RemoteConfigManager.startListening()
    }
}