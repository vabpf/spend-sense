package com.spendsense

import android.app.Application
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.service.DailySpentReportWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SpendSenseApplication : Application() {

    @Inject
    lateinit var securePreferences: SecurePreferences

    override fun onCreate() {
        super.onCreate()
        
        // Schedule daily spent report notification safely
        try {
            DailySpentReportWorker.schedule(
                context = this,
                timeStr = securePreferences.getDailyReportTime(),
                enabled = securePreferences.isDailyReportEnabled()
            )
        } catch (e: Exception) {
            android.util.Log.e("SpendSenseApp", "Failed to schedule DailySpentReportWorker", e)
        }
    }
}
