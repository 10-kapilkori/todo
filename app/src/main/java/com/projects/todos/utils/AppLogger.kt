package com.projects.todos.utils

import android.content.Context
import android.util.Log
import com.projects.todos.R

object AppLogger {
    
    private const val TAG_PREFIX = "TodoApp"
    private const val MAX_TAG_LENGTH = 23 // Android's limit for tag length
    
    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }
    
    // Logging configuration
    private var currentLogLevel = Level.DEBUG // Default to DEBUG level
    
    // Context for string resources (set by application)
    private var appContext: Context? = null
    
    /**
     * Initialize the logger with application context
     * Call this in your Application class
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    /**
     * Set the current log level
     */
    fun setLogLevel(level: Level) {
        currentLogLevel = level
        i("AppLogger", "Log level set to: $level")
    }
    
    /**
     * Get string resource without context
     * Use this in non-Android components like ViewModels and Repositories
     */
    fun getString(resourceId: Int, vararg formatArgs: Any?): String {
        return appContext?.getString(resourceId, *formatArgs) ?: "String resource not available"
    }
    
    /**
     * Creates a tag with the prefix and class name
     */
    private fun createTag(className: String): String {
        val tag = "$TAG_PREFIX.$className"
        return if (tag.length > MAX_TAG_LENGTH) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else {
            tag
        }
    }
    
    /**
     * Check if a log level should be printed
     */
    private fun shouldLog(level: Level): Boolean {
        return level.ordinal >= currentLogLevel.ordinal
    }
    
    /**
     * Log verbose messages
     */
    fun v(className: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.VERBOSE)) {
            val tag = createTag(className)
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
    
    /**
     * Log debug messages
     */
    fun d(className: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    /**
     * Log info messages
     */
    fun i(className: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.INFO)) {
            val tag = createTag(className)
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }
    
    /**
     * Log warning messages
     */
    fun w(className: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.WARNING)) {
            val tag = createTag(className)
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Log error messages
     */
    fun e(className: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.ERROR)) {
            val tag = createTag(className)
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * Log method entry
     */
    fun methodEntry(className: String, methodName: String, vararg params: Pair<String, Any?>) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val paramsString = if (params.isNotEmpty()) {
                params.joinToString(", ") { "${it.first}=${it.second}" }
            } else {
                "no parameters"
            }
            Log.d(tag, "→ $methodName($paramsString)")
        }
    }
    
    /**
     * Log method exit
     */
    fun methodExit(className: String, methodName: String, result: Any? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val resultString = if (result != null) " = $result" else ""
            Log.d(tag, "← $methodName$resultString")
        }
    }
    
    /**
     * Log database operations
     */
    fun dbOperation(className: String, operation: String, table: String, id: Any? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val idString = if (id != null) " (ID: $id)" else ""
            Log.d(tag, "DB: $operation on $table$idString")
        }
    }
    
    /**
     * Log UI operations
     */
    fun uiOperation(className: String, operation: String, details: String? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val detailsString = if (details != null) " - $details" else ""
            Log.d(tag, "UI: $operation$detailsString")
        }
    }
    
    /**
     * Log notification operations
     */
    fun notification(className: String, operation: String, taskId: Int? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val taskString = if (taskId != null) " for task $taskId" else ""
            Log.d(tag, "NOTIFICATION: $operation$taskString")
        }
    }
    
    /**
     * Log date/time operations
     */
    fun dateTime(className: String, operation: String, timestamp: Long? = null) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            val timestampString = if (timestamp != null) " (timestamp: $timestamp)" else ""
            Log.d(tag, "DATETIME: $operation$timestampString")
        }
    }
    
    /**
     * Log error with context
     */
    fun error(className: String, context: String, error: Throwable, additionalInfo: String? = null) {
        if (shouldLog(Level.ERROR)) {
            val tag = createTag(className)
            val infoString = if (additionalInfo != null) " - $additionalInfo" else ""
            Log.e(tag, "ERROR in $context$infoString", error)
        }
    }
    
    /**
     * Log performance metrics
     */
    fun performance(className: String, operation: String, duration: Long) {
        if (shouldLog(Level.DEBUG)) {
            val tag = createTag(className)
            Log.d(tag, "PERFORMANCE: $operation took ${duration}ms")
        }
    }
}
