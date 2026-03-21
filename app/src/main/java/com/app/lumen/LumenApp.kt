package com.app.lumen

import android.app.Application
import com.google.firebase.FirebaseApp

class LumenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
