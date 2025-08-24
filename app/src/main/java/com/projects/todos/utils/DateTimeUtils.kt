package com.projects.todos.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    
    fun formatDate(timestamp: Long): String {
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_format_date, timestamp))
        return dateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_format_time, timestamp))
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatDateTime(timestamp: Long): String {
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_format_datetime, timestamp))
        return dateTimeFormat.format(Date(timestamp))
    }
    
    fun formatFullDateTime(timestamp: Long): String {
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_format_full, timestamp))
        return fullDateTimeFormat.format(Date(timestamp))
    }
    
    fun isOverdue(timestamp: Long): Boolean {
        val isOverdue = timestamp < System.currentTimeMillis()
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_is_overdue, timestamp))
        AppLogger.d("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.task_overdue_check, isOverdue, timestamp))
        return isOverdue
    }
    
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val taskDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val isToday = today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR)
        
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_is_today, timestamp))
        AppLogger.d("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.is_today_check, isToday, timestamp))
        return isToday
    }
    
    fun isTomorrow(timestamp: Long): Boolean {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val taskDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val isTomorrow = tomorrow.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
               tomorrow.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR)
        
        AppLogger.dateTime("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.datetime_is_tomorrow, timestamp))
        AppLogger.d("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.is_tomorrow_check, isTomorrow, timestamp))
        return isTomorrow
    }
    
    fun getRelativeTimeString(timestamp: Long): String {
        AppLogger.methodEntry("DateTimeUtils", "getRelativeTimeString", "timestamp" to timestamp)
        
        val result = when {
            isToday(timestamp) -> AppLogger.getString(com.projects.todos.R.string.today_at_format, formatTime(timestamp))
            isTomorrow(timestamp) -> AppLogger.getString(com.projects.todos.R.string.tomorrow_at_format, formatTime(timestamp))
            else -> formatFullDateTime(timestamp)
        }
        
        AppLogger.d("DateTimeUtils", AppLogger.getString(com.projects.todos.R.string.relative_time_string, result))
        AppLogger.methodExit("DateTimeUtils", "getRelativeTimeString", result)
        return result
    }
}
