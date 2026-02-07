package com.spendsense

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpendSenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
