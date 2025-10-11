package com.example.agriconnect

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase services
        val firestore = Firebase.firestore
        val auth = Firebase.auth


        if (BuildConfig.DEBUG) {
            // Connect to Firebase Emulators (for local testing)
            firestore.useEmulator("10.0.2.2", 8080)
            auth.useEmulator("10.0.2.2", 9099)

        }
    }
}
