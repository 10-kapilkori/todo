package com.projects.todos.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.projects.todos.data.OnboardingItem

object JsonParser {
    
    fun parseOnboardingItems(context: Context): List<OnboardingItem> {
        return try {
            val jsonString = context.resources.openRawResource(
                context.resources.getIdentifier("onboarding", "raw", context.packageName)
            ).bufferedReader().use { it.readText() }
            
            val type = object : TypeToken<List<OnboardingItem>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
