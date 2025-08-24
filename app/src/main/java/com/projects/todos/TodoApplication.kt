package com.projects.todos

import android.app.Application
import com.projects.todos.utils.AppLogger

class TodoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AppLogger with application context
        AppLogger.init(this)
        
        // Set log level based on build type
        // You can change this to AppLogger.Level.ERROR for release builds
        AppLogger.setLogLevel(AppLogger.Level.DEBUG)
        
        // Log application startup
        AppLogger.i("TodoApplication", "Application started")
    }
}
